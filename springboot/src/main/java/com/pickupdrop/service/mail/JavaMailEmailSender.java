package com.pickupdrop.service.mail;

import com.pickupdrop.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** SMTP transport (Gmail in dev/pro). Active only when {@code app.mail.enabled=true}. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class JavaMailEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void send(String to, String subject, String htmlBody) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            setFrom(helper);
            javaMailSender.send(message);
            log.info("mail sent to={} subject={}", to, subject);
        } catch (Exception e) {
            // Wrapped so MailService sees one failure type regardless of provider.
            throw new MailDeliveryException("Could not send mail to " + to, e);
        }
    }

    private void setFrom(MimeMessageHelper helper) throws Exception {
        String from = mailProperties.getFrom();
        if (from == null || from.isBlank()) {
            return; // JavaMailSender falls back to spring.mail.username
        }
        try {
            helper.setFrom(from, mailProperties.getFromName());
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(from);
        }
    }
}
