package com.sfood.automation.auth.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.sfood.automation.common.exception.OtpMailDeliveryException;
import com.sfood.automation.common.slack.SlackNotifier;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class OtpMailSenderTest {

    private JavaMailSender javaMailSender;
    private SlackNotifier slackNotifier;

    @BeforeEach
    void setUp() {
        javaMailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        org.mockito.Mockito.when(javaMailSender.createMimeMessage())
            .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void should_send_otp_mail_as_html_mime_message() throws Exception {
        OtpMailSender otpMailSender = new OtpMailSender(javaMailSender, slackNotifier, "sender@gmail.com");

        otpMailSender.send("user@s-food.com", "123456");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        MimeMessage mimeMessage = captor.getValue();
        assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("sender@gmail.com");
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("user@s-food.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("[SFOOD] OTP Verification Code");
        assertThat(mimeMessage.getDataHandler().getContentType()).contains("text/html");
        Object content = mimeMessage.getContent();
        assertThat(content).isInstanceOf(String.class);
        assertThat(((String) content)).contains("123456");
        assertThat(((String) content)).contains("인증 코드");
    }

    @Test
    void should_notify_slack_and_throw_when_mail_send_fails() {
        org.mockito.Mockito.doThrow(new MailSendException("smtp down"))
            .when(javaMailSender)
            .send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
        OtpMailSender otpMailSender = new OtpMailSender(javaMailSender, slackNotifier, "sender@gmail.com");

        assertThatThrownBy(() -> otpMailSender.send("user@s-food.com", "123456"))
            .isInstanceOf(OtpMailDeliveryException.class);

        verify(slackNotifier).send(eq("OtpMailSender.send"), eq("Failed to send OTP mail to user@s-food.com: smtp down"));
    }
}
