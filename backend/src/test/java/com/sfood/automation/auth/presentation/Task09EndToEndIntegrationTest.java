package com.sfood.automation.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.auth.application.port.OtpCodeGenerator;
import com.sfood.automation.auth.application.port.OtpMailPort;
import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.common.slack.SlackNotifier;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.model.GodoGoodsInfo;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import com.sfood.automation.wms.infrastructure.scraping.WmsStockItem;
import com.sfood.automation.wms.infrastructure.scraping.WmsTransport;
import com.sfood.automation.wms.infrastructure.scraping.WmsTransportResponse;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Task09EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeGodoApiPort fakeGodoApiPort;

    @Autowired
    private FakeWmsTransport fakeWmsTransport;

    @Autowired
    private RecordingSlackNotifier recordingSlackNotifier;

    @Autowired
    private RecordingMailSender recordingMailSender;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
        registry.add("app.batch.scheduler.auto-start", () -> "false");
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();
        fakeGodoApiPort.reset();
        fakeWmsTransport.reset();
        recordingSlackNotifier.reset();
        recordingMailSender.reset();
    }

    @AfterEach
    void tearDown() {
        fakeWmsTransport.releaseStockFetch();
    }

    @Test
    void should_return_401_for_protected_apis_without_session() throws Exception {
        mockMvc.perform(get("/api/goods"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/goods/1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/goods/status-logs"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(put("/api/admin/batch-schedules/product-sync")
                .contentType("application/json")
                .content("""
                    {"enabled":true,"fixedDelaySeconds":300,"modifiedBy":"admin@s-food.com"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/admin/batch-schedules/product-sync/run"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void should_return_skipped_when_manual_batch_is_requested_while_running() throws Exception {
        fakeWmsTransport.setStockItems(List.of(stockItem("ERP-T09-002", 20, 80)));
        fakeWmsTransport.blockNextStockFetch();

        MockHttpSession session = authenticate();
        registerGoods(session, "10090002", "ERP-T09-002", "동시실행 상품", "N");

        CompletableFuture<Void> firstRun = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(post("/api/admin/batch-schedules/product-sync/run").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.triggered").value(true))
                    .andExpect(jsonPath("$.status").value("STARTED"));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });

        fakeWmsTransport.awaitStockFetchStarted();

        mockMvc.perform(post("/api/admin/batch-schedules/product-sync/run").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.triggered").value(false))
            .andExpect(jsonPath("$.status").value("SKIPPED"));

        fakeWmsTransport.releaseStockFetch();
        firstRun.get(5, TimeUnit.SECONDS);
    }

    @Test
    void should_hide_goods_when_godo_sync_is_y() throws Exception {
        MockHttpSession session = authenticate();
        Integer goodsId = registerGoods(session, "10090003", "ERP-T09-003", "고도연동 상품", "N");

        mockMvc.perform(delete("/api/goods/{id}", goodsId).session(session))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goods/{id}", goodsId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("HIDE"));
    }

    @Test
    void should_physically_delete_goods_when_godo_sync_is_n() throws Exception {
        MockHttpSession session = authenticate();
        Integer goodsId = createUnsyncedGoods(session);

        mockMvc.perform(delete("/api/goods/{id}", goodsId).session(session))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goods/{id}", goodsId).session(session))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("GOODS_NOT_FOUND"));
    }

    @Test
    void should_notify_slack_and_keep_goods_state_when_godo_api_fails() throws Exception {
        fakeWmsTransport.setStockItems(List.of(stockItem("ERP-T09-005", 20, 80)));
        fakeGodoApiPort.failUpdate("10090005");

        MockHttpSession session = authenticate();
        Integer goodsId = registerGoods(session, "10090005", "ERP-T09-005", "고도오류 상품", "N");

        mockMvc.perform(post("/api/admin/batch-schedules/product-sync/run").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.triggered").value(true));
        assertThat(awaitBatchEffect(() -> count("SELECT COUNT(*) FROM batch_execution_log WHERE job_name = 'product-sync'") >= 1)).isTrue();

        mockMvc.perform(get("/api/goods/{id}", goodsId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.soldOutYn").value("N"));

        assertThat(recordingSlackNotifier.messages())
            .anySatisfy(message -> assertThat(message).contains("Godo API error for godoGoodsCode=10090005"));
        assertThat(count("SELECT COUNT(*) FROM goods_status_log WHERE godo_goods_code = '10090005'")).isZero();
    }

    @Test
    void should_fail_batch_and_keep_schedule_when_wms_collection_fails() throws Exception {
        fakeWmsTransport.failNextStockFetch();

        MockHttpSession session = authenticate();

        mockMvc.perform(post("/api/admin/batch-schedules/product-sync/run").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.triggered").value(true))
            .andExpect(jsonPath("$.status").value("STARTED"));

        assertThat(count("SELECT COUNT(*) FROM batch_execution_log WHERE job_name = 'product-sync' AND status = 'FAIL'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM batch_schedule_config WHERE job_name = 'product-sync' AND enabled = 0")).isEqualTo(1);
        assertThat(recordingSlackNotifier.messages())
            .anySatisfy(message -> assertThat(message).contains("[product-sync]").contains("WMS"));
    }

    @Test
    void should_create_user_on_first_login_and_update_preferences() throws Exception {
        MockHttpSession session = authenticate();

        assertThat(count("SELECT COUNT(*) FROM users WHERE email = 'user@s-food.com'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM users WHERE email = 'user@s-food.com' AND email_notification_yn = 'N' AND teams_notification_yn = 'N'"))
            .isEqualTo(1);

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@s-food.com"))
            .andExpect(jsonPath("$.emailNotificationYn").value("N"))
            .andExpect(jsonPath("$.teamsNotificationYn").value("N"));

        updateNotifications(session, "Y", "Y");

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailNotificationYn").value("Y"))
            .andExpect(jsonPath("$.teamsNotificationYn").value("Y"));
    }

    private MockHttpSession authenticate() throws Exception {
        MvcResult requestResult = mockMvc.perform(post("/api/auth/email-verifications")
                .contentType("application/json")
                .content("""
                    {"email":"user@s-food.com"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();

        String verificationKey = objectMapper.readTree(requestResult.getResponse().getContentAsString())
            .get("verificationKey")
            .asText();

        MvcResult confirmResult = mockMvc.perform(post("/api/auth/email-verifications/confirm")
                .contentType("application/json")
                .content("""
                    {"verificationKey":"%s","email":"user@s-food.com","code":"123456"}
                    """.formatted(verificationKey)))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) confirmResult.getRequest().getSession(false);
    }

    private Integer registerGoods(MockHttpSession session, String godoGoodsCode, String erpGoodsCode, String goodsName, String onlyMallYn)
        throws Exception {
        MvcResult result = mockMvc.perform(post("/api/goods")
                .session(session)
                .contentType("application/json")
                .content("""
                    [{"godoGoodsCode":"%s","erpGoodsCode":"%s","goodsName":"%s","onlyMallYn":"%s"}]
                    """.formatted(godoGoodsCode, erpGoodsCode, goodsName, onlyMallYn)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("items").get(0).get("id").asInt();
    }

    private void updateNotifications(MockHttpSession session, String emailNotificationYn, String teamsNotificationYn) throws Exception {
        mockMvc.perform(put("/api/auth/me/notifications")
                .session(session)
                .contentType("application/json")
                .content("""
                    {"emailNotificationYn":"%s","teamsNotificationYn":"%s"}
                    """.formatted(emailNotificationYn, teamsNotificationYn)))
            .andExpect(status().isOk());
    }

    private Integer createUnsyncedGoods(MockHttpSession session) {
        jdbcTemplate.update(
            """
                INSERT INTO goods (
                    godo_goods_code, erp_goods_code, goods_name, only_mall_yn, sold_out_yn, available_qty, safety_rate,
                    godo_sync_yn, erp_sync_yn, description, message, status, created_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
                """,
            "10090004",
            "ERP-T09-004",
            "미연동 상품",
            "N",
            "N",
            0,
            0,
            "N",
            "Y",
            null,
            null,
            "UNVERIFIED",
            session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY)
        );
        return jdbcTemplate.queryForObject("SELECT id FROM goods WHERE godo_goods_code = '10090004'", Integer.class);
    }

    private boolean triggerBatchUntilStarted(MockHttpSession session) throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            MvcResult result = mockMvc.perform(post("/api/admin/batch-schedules/product-sync/run").session(session))
                .andExpect(status().isOk())
                .andReturn();

            boolean triggered = objectMapper.readTree(result.getResponse().getContentAsString()).get("triggered").asBoolean();
            if (triggered) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private WmsStockItem stockItem(String itemCd, int invnQty, Integer validPercent) {
        return new WmsStockItem(itemCd, "테스트상품", invnQty, validPercent, "LOT-1", "OA110", "104", "", "SLP001",
            "2026-04-01T00:00:00");
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private String querySoldOutYn(Integer goodsId) {
        return jdbcTemplate.queryForObject("SELECT sold_out_yn FROM goods WHERE id = ?", String.class, goodsId);
    }

    private boolean awaitBatchEffect(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM goods_status_log");
        jdbcTemplate.update("DELETE FROM api_transaction_log");
        jdbcTemplate.update("DELETE FROM email_send_log");
        jdbcTemplate.update("DELETE FROM batch_execution_log");
        jdbcTemplate.update("DELETE FROM batch_schedule_change_log");
        jdbcTemplate.update("DELETE FROM email_verification");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM goods");
        jdbcTemplate.update(
            """
                UPDATE batch_schedule_config
                SET enabled = 0, fixed_delay_seconds = 600, initial_delay_seconds = 0, updated_at = datetime('now'), last_modified_by = NULL
                WHERE job_name = 'product-sync'
                """
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        OtpCodeGenerator fixedOtpCodeGenerator() {
            return () -> "123456";
        }

        @Bean
        @Primary
        OtpMailPort testOtpMailPort() {
            return (email, code) -> { };
        }

        @Bean("godoApiClient")
        @Primary
        FakeGodoApiPort fakeGodoApiPort() {
            return new FakeGodoApiPort();
        }

        @Bean
        @Primary
        FakeWmsTransport fakeWmsTransport() {
            return new FakeWmsTransport();
        }

        @Bean
        @Primary
        RecordingMailSender recordingMailSender() {
            return new RecordingMailSender();
        }

        @Bean
        @Primary
        RecordingSlackNotifier recordingSlackNotifier() {
            return new RecordingSlackNotifier();
        }
    }

    static final class FakeGodoApiPort implements GodoApiPort {
        private final List<String> updateCalls = new ArrayList<>();
        private final Set<String> failingCodes = java.util.concurrent.ConcurrentHashMap.newKeySet();

        @Override
        public GodoGoodsInfo searchGoods(String godoGoodsCode) {
            return new GodoGoodsInfo(godoGoodsCode, "검색상품-" + godoGoodsCode, "N");
        }

        @Override
        public GodoApiResult updateSoldOutStatus(String godoGoodsCode, String soldOutFl) {
            updateCalls.add(godoGoodsCode + ":" + soldOutFl);
            if (failingCodes.contains(godoGoodsCode)) {
                throw new GodoApiException();
            }
            return new GodoApiResult(true, "000", "success");
        }

        void failUpdate(String godoGoodsCode) {
            failingCodes.add(godoGoodsCode);
        }

        void reset() {
            updateCalls.clear();
            failingCodes.clear();
        }

        List<String> updateCalls() {
            return List.copyOf(updateCalls);
        }
    }

    static final class FakeWmsTransport implements WmsTransport {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final List<WmsStockItem> stockItems = new ArrayList<>();
        private volatile boolean failStockFetch;
        private volatile CountDownLatch stockFetchStarted = new CountDownLatch(0);
        private volatile CountDownLatch stockFetchRelease = new CountDownLatch(0);

        @Override
        public WmsTransportResponse get(String url, String query) {
            return new WmsTransportResponse("", Map.of("Set-Cookie", "JSESSIONID=test-session; Path=/"));
        }

        @Override
        public WmsTransportResponse post(String url, String body, HttpCookie cookie) {
            stockFetchStarted.countDown();
            awaitReleaseIfNeeded();
            if (failStockFetch) {
                return new WmsTransportResponse("{\"DS_TOTALCOUNT\":0,\"ErrorMsg\":\"WMS error\",\"ErrorCode\":1,\"DS_OUT\":[]}", Map.of());
            }
            return new WmsTransportResponse(successBody(), Map.of());
        }

        void setStockItems(List<WmsStockItem> items) {
            stockItems.clear();
            stockItems.addAll(items);
        }

        void failNextStockFetch() {
            this.failStockFetch = true;
        }

        void blockNextStockFetch() {
            this.stockFetchStarted = new CountDownLatch(1);
            this.stockFetchRelease = new CountDownLatch(1);
        }

        void awaitStockFetchStarted() throws InterruptedException {
            assertThat(stockFetchStarted.await(5, TimeUnit.SECONDS)).isTrue();
        }

        void releaseStockFetch() {
            stockFetchRelease.countDown();
        }

        void reset() {
            stockItems.clear();
            failStockFetch = false;
            stockFetchStarted = new CountDownLatch(0);
            stockFetchRelease = new CountDownLatch(0);
        }

        private void awaitReleaseIfNeeded() {
            try {
                stockFetchRelease.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
        }

        private String successBody() {
            try {
                return objectMapper.writeValueAsString(Map.of(
                    "DS_TOTALCOUNT", stockItems.size(),
                    "ErrorMsg", "",
                    "ErrorCode", 0,
                    "DS_OUT", stockItems
                ));
            } catch (JsonProcessingException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    static final class RecordingMailSender implements JavaMailSender {
        private final List<String> subjects = new ArrayList<>();
        private final List<String> recipients = new ArrayList<>();
        private final List<String> htmlBodies = new ArrayList<>();

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getInstance(new Properties()));
        }

        @Override
        public MimeMessage createMimeMessage(java.io.InputStream contentStream) throws MailException {
            try {
                return new MimeMessage(Session.getInstance(new Properties()), contentStream);
            } catch (Exception exception) {
                throw new org.springframework.mail.MailParseException("Failed to create mime message", exception);
            }
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            try {
                subjects.add(mimeMessage.getSubject());
                if (mimeMessage.getAllRecipients() != null) {
                    for (var recipient : mimeMessage.getAllRecipients()) {
                        recipients.add(recipient.toString());
                    }
                }
                Object content = mimeMessage.getContent();
                if (content instanceof String body) {
                    htmlBodies.add(body);
                }
            } catch (Exception exception) {
                throw new org.springframework.mail.MailParseException("Failed to read mime message subject", exception);
            }
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            for (MimeMessage mimeMessage : mimeMessages) {
                send(mimeMessage);
            }
        }

        @Override
        public void send(org.springframework.mail.javamail.MimeMessagePreparator mimeMessagePreparator) {
        }

        @Override
        public void send(org.springframework.mail.javamail.MimeMessagePreparator... mimeMessagePreparators) {
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            subjects.add(simpleMessage.getSubject());
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            for (SimpleMailMessage simpleMessage : simpleMessages) {
                send(simpleMessage);
            }
        }

        void reset() {
            subjects.clear();
            recipients.clear();
            htmlBodies.clear();
        }

        List<String> subjects() {
            return List.copyOf(subjects);
        }

        List<String> recipients() {
            return List.copyOf(recipients);
        }

        List<String> htmlBodies() {
            return List.copyOf(htmlBodies);
        }
    }

    static final class RecordingSlackNotifier extends SlackNotifier {
        private final List<String> messages = new ArrayList<>();

        RecordingSlackNotifier() {
            super(RestClient.builder(), "");
        }

        @Override
        public void send(String location, String message) {
            messages.add(location + "::" + message);
        }

        @Override
        public void sendInfo(String location, String message) {
            messages.add(location + "::" + message);
        }

        void reset() {
            messages.clear();
        }

        List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
