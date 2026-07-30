package com.pickupdrop.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default transport when mail is off (loc, tests). Logs what would have been
 * sent so the flow is verifiable without an SMTP account.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("mail suppressed (app.mail.enabled=false) to={} subject={}", to, subject);
        log.debug("mail body to={}\n{}", to, htmlBody);
    }
}
