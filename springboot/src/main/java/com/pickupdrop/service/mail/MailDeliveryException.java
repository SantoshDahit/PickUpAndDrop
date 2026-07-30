package com.pickupdrop.service.mail;

/** Provider-agnostic transport failure. Never surfaced to API clients. */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
