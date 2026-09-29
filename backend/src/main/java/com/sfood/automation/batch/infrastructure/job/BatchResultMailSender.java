package com.sfood.automation.batch.infrastructure.job;

import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.batch.domain.enums.EmailSendType;
import com.sfood.automation.batch.domain.model.EmailSendLog;
import com.sfood.automation.batch.domain.port.EmailSendLogRepository;
import com.sfood.automation.common.slack.SlackNotifier;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class BatchResultMailSender {

    private static final Logger log = LoggerFactory.getLogger(BatchResultMailSender.class);
    private static final DateTimeFormatter SUBJECT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));
    private static final String ON_SALE_STATUS = "ON_SALE";
    private static final String OUT_OF_STOCK_STATUS = "OUT_OF_STOCK";

    private final JavaMailSender javaMailSender;
    private final EmailSendLogRepository emailSendLogRepository;
    private final GoodsRepository goodsRepository;
    private final UserAccountRepository userAccountRepository;
    private final SlackNotifier slackNotifier;
    private final String senderAddress;

    public BatchResultMailSender(
        JavaMailSender javaMailSender,
        EmailSendLogRepository emailSendLogRepository,
        GoodsRepository goodsRepository,
        UserAccountRepository userAccountRepository,
        SlackNotifier slackNotifier,
        @Value("${spring.mail.username:}") String senderAddress
    ) {
        this.javaMailSender = javaMailSender;
        this.emailSendLogRepository = emailSendLogRepository;
        this.goodsRepository = goodsRepository;
        this.userAccountRepository = userAccountRepository;
        this.slackNotifier = slackNotifier;
        this.senderAddress = senderAddress;
    }

    public void send(List<GoodsStatusLog> changedLogs) {
        if (changedLogs.isEmpty()) {
            return;
        }
        List<String> recipients = userAccountRepository.findAllEmailNotificationEnabled().stream()
            .map(com.sfood.automation.auth.domain.model.UserAccount::getEmail)
            .toList();
        if (recipients.isEmpty()) {
            log.info("Skip batch result email because no opted-in recipients exist");
            return;
        }

        Instant now = Instant.now();
        String subject = "[에쓰푸드몰] 품목 판매 상태 변경 알림 " + SUBJECT_FORMAT.format(now);
        String recipientSummary = String.join(",", recipients);
        EmailSendLog pendingLog = emailSendLogRepository.save(
            EmailSendLog.pending(EmailSendType.BATCH_RESULT, recipientSummary, subject, now)
        );

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(senderAddress);
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(buildHtmlBody(changedLogs, now), true);
            javaMailSender.send(message);
            emailSendLogRepository.save(pendingLog.markSuccess(Instant.now()));
        } catch (Exception exception) {
            emailSendLogRepository.save(pendingLog.markFailure(exception.getMessage(), Instant.now()));
            slackNotifier.send("BatchResultMailSender.send", exception.getMessage());
            log.warn("Failed to send batch result email", exception);
        }
    }

    private String buildHtmlBody(List<GoodsStatusLog> changedLogs, Instant now) {
        StringBuilder rows = new StringBuilder();
        for (GoodsStatusLog changedLog : changedLogs) {
            String goodsName = goodsRepository.findByGodoCode(changedLog.godoGoodsCode())
                .map(Goods::getGoodsName)
                .orElse(changedLog.godoGoodsCode());
            rows.append("""
                    <tr>
                        <td style="padding:12px;border:1px solid #d9e2ec;">%s</td>
                        <td style="padding:12px;border:1px solid #d9e2ec;">%s</td>
                        <td style="padding:12px;border:1px solid #d9e2ec;">%s</td>
                        <td style="padding:12px;border:1px solid #d9e2ec;">%s</td>
                        <td style="padding:12px;border:1px solid #d9e2ec;text-align:right;">%d</td>
                    </tr>
                """.formatted(
                changedLog.godoGoodsCode(),
                goodsName,
                toDisplayStatus(changedLog.beforeStatus()),
                toDisplayStatus(changedLog.afterStatus()),
                changedLog.wmsAvailableQty()
            ));
        }
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>품목 판매 상태 변경 알림</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:'Malgun Gothic',Apple SD Gothic Neo,sans-serif;color:#1f2933;">
                <div style="max-width:880px;margin:0 auto;padding:32px 20px;">
                    <div style="background-color:#ffffff;border:1px solid #d9e2ec;border-radius:16px;padding:32px;">
                        <p style="margin:0 0 8px 0;font-size:14px;color:#52606d;">SFOOD-MALL 동기화 결과 알림</p>
                        <h1 style="margin:0 0 12px 0;font-size:28px;color:#102a43;">품목 판매 상태 변경 내역</h1>
                        <p style="margin:0 0 24px 0;font-size:14px;color:#52606d;">
                            배치 실행 시각: %s<br/>
                            변경 건수: %d건
                        </p>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                            <thead>
                                <tr style="background-color:#f0f4f8;">
                                    <th style="padding:12px;border:1px solid #d9e2ec;text-align:left;">품목 코드</th>
                                    <th style="padding:12px;border:1px solid #d9e2ec;text-align:left;">품목명</th>
                                    <th style="padding:12px;border:1px solid #d9e2ec;text-align:left;">변경 전</th>
                                    <th style="padding:12px;border:1px solid #d9e2ec;text-align:left;">변경 후</th>
                                    <th style="padding:12px;border:1px solid #d9e2ec;text-align:right;">근거 재고수량</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(SUBJECT_FORMAT.format(now), changedLogs.size(), rows);
    }

    private String toDisplayStatus(String status) {
        if (ON_SALE_STATUS.equals(status)) {
            return "판매중";
        } else if(OUT_OF_STOCK_STATUS.equals(status)) {
            return "품절";
        }
        return status;
    }
}
