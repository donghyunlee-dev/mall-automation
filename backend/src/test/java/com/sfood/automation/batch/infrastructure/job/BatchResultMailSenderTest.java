package com.sfood.automation.batch.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.batch.domain.enums.EmailSendType;
import com.sfood.automation.batch.domain.model.EmailSendLog;
import com.sfood.automation.batch.domain.port.EmailSendLogRepository;
import com.sfood.automation.common.slack.SlackNotifier;
import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class BatchResultMailSenderTest {

    @Test
    void should_skip_send_when_no_changed_logs() {
        JavaMailSender javaMailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        FakeEmailSendLogRepository emailSendLogRepository = new FakeEmailSendLogRepository();
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        UserAccountRepository userAccountRepository = new FakeUserAccountRepository();
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        BatchResultMailSender sender = new BatchResultMailSender(
            javaMailSender,
            emailSendLogRepository,
            goodsRepository,
            userAccountRepository,
            slackNotifier,
            "no-reply@s-food.com"
        );

        sender.send(List.of());

        org.mockito.Mockito.verifyNoInteractions(javaMailSender, goodsRepository, slackNotifier);
        assertThat(emailSendLogRepository.savedLogs).isEmpty();
    }

    @Test
    void should_send_batch_result_mail_with_table_body() throws Exception {
        RecordingMailSender javaMailSender = new RecordingMailSender();
        FakeEmailSendLogRepository emailSendLogRepository = new FakeEmailSendLogRepository();
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        UserAccountRepository userAccountRepository = new FakeUserAccountRepository(
            user("user1@s-food.com", "Y", "N"),
            user("user2@s-food.com", "Y", "Y")
        );
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        whenGoods(goodsRepository);
        BatchResultMailSender sender = new BatchResultMailSender(
            javaMailSender,
            emailSendLogRepository,
            goodsRepository,
            userAccountRepository,
            slackNotifier,
            "no-reply@s-food.com"
        );

        sender.send(List.of(
            log("10000001", "ERP-0001", "ON_SALE", "OUT_OF_STOCK", 20),
            log("10000002", "ERP-0002", "OUT_OF_STOCK", "ON_SALE", 35),
            log("10000003", "ERP-0003", "ON_SALE", "OUT_OF_STOCK", 10)
        ));

        assertThat(javaMailSender.sentMessage.getAllRecipients())
            .extracting(Object::toString)
            .containsExactly("user1@s-food.com", "user2@s-food.com");
        assertThat(javaMailSender.htmlBody).contains("품목 판매 상태 변경 내역");
        assertThat(javaMailSender.htmlBody).contains("Goods 1");
        assertThat(javaMailSender.htmlBody).contains("근거 재고수량");
        assertThat(javaMailSender.htmlBody).contains("판매중");
        assertThat(javaMailSender.htmlBody).contains("품절");
        assertThat(javaMailSender.htmlBody).doesNotContain("ON_SALE");
        assertThat(javaMailSender.htmlBody).doesNotContain("OUT_OF_STOCK");
        assertThat(emailSendLogRepository.savedLogs).hasSize(2);
        assertThat(emailSendLogRepository.savedLogs.get(1).isSuccess()).isTrue();
    }

    @Test
    void should_save_success_log_when_send_succeeds() {
        JavaMailSender javaMailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        FakeEmailSendLogRepository emailSendLogRepository = new FakeEmailSendLogRepository();
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        UserAccountRepository userAccountRepository = new FakeUserAccountRepository(user("ops@s-food.com", "Y", "N"));
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        whenGoods(goodsRepository);
        BatchResultMailSender sender = new BatchResultMailSender(
            javaMailSender,
            emailSendLogRepository,
            goodsRepository,
            userAccountRepository,
            slackNotifier,
            "no-reply@s-food.com"
        );

        sender.send(List.of(log("10000001", "ERP-0001", "ON_SALE", "OUT_OF_STOCK", 20)));

        assertThat(emailSendLogRepository.savedLogs.get(1).sendType()).isEqualTo(EmailSendType.BATCH_RESULT);
        assertThat(emailSendLogRepository.savedLogs.get(1).isSuccess()).isTrue();
    }

    @Test
    void should_save_failure_log_and_notify_slack_when_send_fails() {
        JavaMailSender javaMailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        org.mockito.Mockito.doThrow(new MailSendException("smtp down")).when(javaMailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
        FakeEmailSendLogRepository emailSendLogRepository = new FakeEmailSendLogRepository();
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        UserAccountRepository userAccountRepository = new FakeUserAccountRepository(user("ops@s-food.com", "Y", "N"));
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        whenGoods(goodsRepository);
        BatchResultMailSender sender = new BatchResultMailSender(
            javaMailSender,
            emailSendLogRepository,
            goodsRepository,
            userAccountRepository,
            slackNotifier,
            "no-reply@s-food.com"
        );

        sender.send(List.of(log("10000001", "ERP-0001", "ON_SALE", "OUT_OF_STOCK", 20)));

        assertThat(emailSendLogRepository.savedLogs).hasSize(2);
        assertThat(emailSendLogRepository.savedLogs.get(1).isSuccess()).isFalse();
        assertThat(emailSendLogRepository.savedLogs.get(1).errorMessage()).contains("smtp down");
        org.mockito.Mockito.verify(slackNotifier).send("BatchResultMailSender.send", "smtp down");
    }

    @Test
    void should_skip_send_when_no_opted_in_recipients() {
        JavaMailSender javaMailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        FakeEmailSendLogRepository emailSendLogRepository = new FakeEmailSendLogRepository();
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        UserAccountRepository userAccountRepository = new FakeUserAccountRepository(user("ops@s-food.com", "N", "N"));
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        BatchResultMailSender sender = new BatchResultMailSender(
            javaMailSender,
            emailSendLogRepository,
            goodsRepository,
            userAccountRepository,
            slackNotifier,
            "no-reply@s-food.com"
        );

        sender.send(List.of(log("10000001", "ERP-0001", "ON_SALE", "OUT_OF_STOCK", 20)));

        org.mockito.Mockito.verifyNoInteractions(javaMailSender, goodsRepository, slackNotifier);
        assertThat(emailSendLogRepository.savedLogs).isEmpty();
    }

    private void whenGoods(GoodsRepository goodsRepository) {
        org.mockito.Mockito.when(goodsRepository.findByGodoCode("10000001")).thenReturn(Optional.of(goods("10000001", "ERP-0001", "Goods 1")));
        org.mockito.Mockito.when(goodsRepository.findByGodoCode("10000002")).thenReturn(Optional.of(goods("10000002", "ERP-0002", "Goods 2")));
        org.mockito.Mockito.when(goodsRepository.findByGodoCode("10000003")).thenReturn(Optional.of(goods("10000003", "ERP-0003", "Goods 3")));
    }

    private GoodsStatusLog log(String godoCode, String erpCode, String beforeStatus, String afterStatus, int qty) {
        return new GoodsStatusLog(null, godoCode, erpCode, qty, 20, 50, 80, beforeStatus, afterStatus, "2026-04-01T00:00:00Z", "2026-04-01T00:00:00Z");
    }

    private Goods goods(String godoCode, String erpCode, String goodsName) {
        return new Goods(
            1,
            godoCode,
            erpCode,
            goodsName,
            "N",
            "N",
            20,
            50,
            150,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );
    }

    private UserAccount user(String email, String emailNotificationYn, String teamsNotificationYn) {
        Instant now = Instant.parse("2026-04-01T00:00:00Z");
        return new UserAccount(1, email, emailNotificationYn, teamsNotificationYn, now, now, now);
    }

    private static final class FakeEmailSendLogRepository implements EmailSendLogRepository {
        private final List<EmailSendLog> savedLogs = new ArrayList<>();
        private int sequence = 1;

        @Override
        public EmailSendLog save(EmailSendLog emailSendLog) {
            EmailSendLog saved = emailSendLog.id() == null
                ? new EmailSendLog(sequence++, emailSendLog.sendType(), emailSendLog.recipient(), emailSendLog.subject(),
                    emailSendLog.isSuccess(), emailSendLog.errorMessage(), emailSendLog.sentAt(), emailSendLog.createdAt())
                : emailSendLog;
            savedLogs.add(saved);
            return saved;
        }
    }

    private static final class FakeUserAccountRepository implements UserAccountRepository {
        private final List<UserAccount> users;

        private FakeUserAccountRepository(UserAccount... users) {
            this.users = new ArrayList<>(List.of(users));
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            return userAccount;
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return users.stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public List<UserAccount> findAllEmailNotificationEnabled() {
            return users.stream().filter(UserAccount::isEmailNotificationEnabled).toList();
        }
    }

    private static final class RecordingMailSender implements JavaMailSender {
        private MimeMessage sentMessage;
        private String htmlBody;

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getInstance(new Properties()));
        }

        @Override
        public MimeMessage createMimeMessage(java.io.InputStream contentStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            this.sentMessage = mimeMessage;
            try {
                Object content = mimeMessage.getContent();
                if (content instanceof String body) {
                    this.htmlBody = body;
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
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
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(org.springframework.mail.javamail.MimeMessagePreparator... mimeMessagePreparators) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(org.springframework.mail.SimpleMailMessage simpleMessage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(org.springframework.mail.SimpleMailMessage... simpleMessages) {
            throw new UnsupportedOperationException();
        }
    }
}
