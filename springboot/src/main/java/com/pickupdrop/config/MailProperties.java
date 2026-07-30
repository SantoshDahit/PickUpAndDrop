package com.pickupdrop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mail settings (convention 19). Credentials stay in {@code spring.mail.*} from
 * the environment; this holds only the non-secret shape of what we send.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Off by default: local runs and tests must never reach a real SMTP server. */
    private boolean enabled = false;

    /** Envelope sender. Gmail rewrites this to the authenticated account anyway. */
    private String from = "";

    private String fromName = "Pickup & Drop";

    /** Public site root, used to build links in emails (no trailing slash). */
    private String webBaseUrl = "http://localhost:3000";

    public String getWebBaseUrl() {
        return webBaseUrl != null && webBaseUrl.endsWith("/")
                ? webBaseUrl.substring(0, webBaseUrl.length() - 1)
                : webBaseUrl;
    }
}
