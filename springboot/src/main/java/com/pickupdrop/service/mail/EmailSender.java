package com.pickupdrop.service.mail;

/**
 * Transport for outbound email (Strategy, as with {@code SmsSender} in
 * convention 17). Swapping providers means adding one implementation.
 */
public interface EmailSender {

    /**
     * Delivers one message. Implementations throw on transport failure —
     * {@link MailService} decides what a failure means for the caller.
     */
    void send(String to, String subject, String htmlBody);
}
