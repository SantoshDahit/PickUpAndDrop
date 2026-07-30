package com.pickupdrop.service.mail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Email bodies. Plain string templates on purpose: mail HTML has to survive
 * clients that ignore stylesheets, so it stays inline and simple — no view
 * engine in the dependency graph for four messages.
 */
final class MailTemplates {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);

    private static final String ACCENT = "#1BBC9B";
    private static final String INK = "#292929";

    private MailTemplates() {
    }

    static String welcome(String name, String bookUrl) {
        return page(
                "Welcome aboard, " + escape(firstName(name)) + "!",
                """
                <p>Your Pickup &amp; Drop account is ready. You can book an airport
                pickup anywhere in Korea, share the ride with other travellers, and
                pay the driver in cash when you land.</p>
                <p>One account is enough for your whole group — book the seats for
                everyone travelling with you.</p>
                """,
                bookUrl,
                "Book a pickup");
    }

    static String bookingConfirmation(String name, String fromLocation, String toLocation,
                                      LocalDate travelDate, int partySize, String flightNo,
                                      Integer farePerPerson, String tripsUrl) {
        StringBuilder rows = new StringBuilder()
                .append(row("Route", escape(fromLocation) + " → " + escape(toLocation)))
                .append(row("Travel date", travelDate.format(DATE)))
                .append(row("Passengers", partySize + (partySize == 1 ? " person" : " people")));
        if (flightNo != null && !flightNo.isBlank()) {
            rows.append(row("Flight", escape(flightNo)));
        }
        if (farePerPerson != null) {
            rows.append(row("Fare", "₩" + String.format("%,d", farePerPerson) + " per person"));
        }

        return page(
                "Your pickup is booked",
                "<p>Thanks, " + escape(firstName(name)) + " — we have your request. "
                        + "Here are the details:</p>"
                        + "<table cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;"
                        + "border-collapse:collapse;margin:22px 0;font-size:15px\">"
                        + rows
                        + "</table>"
                        + """
                        <p><strong>Pay cash on arrival</strong> — no card needed. We'll email
                        you again once a driver is assigned to your ride.</p>
                        <p>Travelling with others on the same route that week? Open your trip
                        to join a group and split the fare.</p>
                        """,
                tripsUrl,
                "View my trips");
    }

    static String passwordReset(String name, String resetUrl, int validMinutes) {
        return page(
                "Reset your password",
                "<p>Hi " + escape(firstName(name)) + ", we got a request to reset your "
                        + "Pickup &amp; Drop password. Use the button below to choose a new one — "
                        + "the link works once and expires in " + validMinutes + " minutes.</p>"
                        + "<p style=\"color:#6b6b6b\">If you didn't ask for this, you can ignore "
                        + "this email; your password stays as it is.</p>",
                resetUrl,
                "Choose a new password");
    }

    private static String row(String label, String value) {
        return "<tr>"
                + "<td style=\"padding:9px 0;color:#6b6b6b;border-bottom:1px solid #ececec\">"
                + escape(label) + "</td>"
                + "<td style=\"padding:9px 0;text-align:right;font-weight:500;color:" + INK
                + ";border-bottom:1px solid #ececec\">" + value + "</td>"
                + "</tr>";
    }

    /** Shell shared by every message: centred card, one call-to-action. */
    private static String page(String heading, String bodyHtml, String ctaUrl, String ctaLabel) {
        return """
               <div style="background:#f6f8f7;padding:32px 12px;font-family:Helvetica,Arial,sans-serif;color:%s">
                 <div style="max-width:520px;margin:0 auto;background:#ffffff;border:1px solid #ececec;border-radius:16px;padding:32px">
                   <p style="margin:0 0 6px;font-size:18px;font-weight:600">
                     Pickup<span style="color:%s">&amp;</span>Drop
                   </p>
                   <p style="margin:0 0 22px;font-size:13px;color:#8a8a8a">
                     Airport pickup → anywhere in Korea
                   </p>
                   <h1 style="margin:0 0 16px;font-size:22px;line-height:1.3">%s</h1>
                   <div style="font-size:15px;line-height:1.6">%s</div>
                   <p style="margin:28px 0 0">
                     <a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;padding:13px 26px;border-radius:999px;font-weight:500">%s</a>
                   </p>
                   <p style="margin:26px 0 0;font-size:13px;color:#8a8a8a">
                     Or open this link: <a href="%s" style="color:%s">%s</a>
                   </p>
                 </div>
                 <p style="max-width:520px;margin:16px auto 0;font-size:12px;color:#9a9a9a;text-align:center">
                   Pickup &amp; Drop · Pay cash on arrival, no card needed
                 </p>
               </div>
               """.formatted(INK, ACCENT, heading, bodyHtml, ctaUrl, ACCENT, ctaLabel,
                             ctaUrl, ACCENT, ctaUrl);
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "there";
        }
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    /** Names and flight numbers are user input — they must not break the markup. */
    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
