package com.sfood.automation.auth.infrastructure.mail;

import com.sfood.automation.auth.application.port.OtpMailPort;
import com.sfood.automation.common.exception.OtpMailDeliveryException;
import com.sfood.automation.common.slack.SlackNotifier;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class OtpMailSender implements OtpMailPort {

    private static final Logger log = LoggerFactory.getLogger(OtpMailSender.class);

    private final JavaMailSender javaMailSender;
    private final SlackNotifier slackNotifier;
    private final String senderAddress;

    public OtpMailSender(
        JavaMailSender javaMailSender,
        SlackNotifier slackNotifier,
        @Value("${spring.mail.username}") String senderAddress
    ) {
        this.javaMailSender = javaMailSender;
        this.slackNotifier = slackNotifier;
        this.senderAddress = senderAddress;
    }

    @Override
    public void send(String email, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(senderAddress);
            helper.setTo(email);
            helper.setSubject("[SFOOD] OTP Verification Code");
            helper.setText(buildHtmlBody(code), true);
            javaMailSender.send(message);
            log.info("OTP mail sent to {}", email);
        } catch (Exception exception) {
            slackNotifier.send(
                "OtpMailSender.send",
                "Failed to send OTP mail to %s: %s".formatted(email, exception.getMessage())
            );
            log.warn("Failed to send OTP mail to {}", email, exception);
            throw new OtpMailDeliveryException();
        }
    }

    private String buildHtmlBody(String code) {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>SFOOD-MALL OTP Verification</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:'Malgun Gothic',Apple SD Gothic Neo,sans-serif;color:#1f2933;">
                <div style="max-width:560px;margin:0 auto;padding:40px 20px;">
                    <div style="background-color:#ffffff;border:1px solid #d9e2ec;border-radius:16px;padding:40px 32px;text-align:center;">
                        <p style="margin:0 0 12px 0;font-size:14px;line-height:20px;color:#52606d;">SFOOD-MALL 동기화 서비스 이메일 인증</p>
                        <h1 style="margin:0 0 16px 0;font-size:28px;line-height:36px;color:#102a43;">인증 코드를 입력해 주세요</h1>
                        <p style="margin:0 0 28px 0;font-size:15px;line-height:24px;color:#52606d;">
                            아래 6자리 인증 코드를 화면에 입력하세요.
                        </p>
                        <div style="margin:0 auto 28px auto;padding:18px 24px;border-radius:14px;background-color:#f0f4f8;border:1px solid #bcccdc;display:inline-block;">
                            <span style="font-size:36px;line-height:42px;font-weight:700;letter-spacing:10px;color:#0b5fff;">%s</span>
                        </div>
                        <p style="margin:0 0 12px 0;font-size:14px;line-height:22px;color:#52606d;">
                            인증 코드는 5분 동안 유효합니다.
                        </p>
                        <p style="margin:0;font-size:13px;line-height:20px;color:#7b8794;">
                            본인이 요청하지 않은 메일이라면 이 메일을 무시해 주세요.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
}
