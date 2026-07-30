package com.pickupdrop.service.mail;

import com.pickupdrop.config.MailProperties;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.EmailPurpose;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends the product's transactional email.
 *
 * <p>Every method is {@code @Async} and swallows transport failures: a booking
 * must not fail because Gmail is slow or unreachable. Callers get no signal, by
 * design — the send is a side effect of an already-committed action.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailService {

    /** Kept in step with {@code EmailPurpose.PASSWORD_RESET.validMinutes}. */
    public static final int RESET_LINK_VALID_MINUTES =
            EmailPurpose.PASSWORD_RESET.getValidMinutes();

    private final EmailSender emailSender;
    private final MailProperties mailProperties;

    @Async("mailExecutor")
    public void sendWelcome(String email, String name) {
        deliver(email, "Welcome to Pickup & Drop",
                MailTemplates.welcome(name, url("/book")));
    }

    /**
     * Booking receipt. Reads only values already loaded on the entity — it runs
     * on another thread, after the caller's transaction, so lazy associations
     * must be resolved by the caller (see {@link #of}).
     */
    @Async("mailExecutor")
    public void sendBookingConfirmation(BookingMail booking) {
        deliver(booking.email(), "Your pickup on " + booking.travelDate() + " is booked",
                MailTemplates.bookingConfirmation(booking.name(), booking.fromLocation(),
                        booking.toLocation(), booking.travelDate(), booking.partySize(),
                        booking.flightNo(), booking.farePerPerson(), url("/trips")));
    }

    @Async("mailExecutor")
    public void sendPasswordReset(String email, String name, String rawToken) {
        String link = url("/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        deliver(email, "Reset your Pickup & Drop password",
                MailTemplates.passwordReset(name, link, RESET_LINK_VALID_MINUTES));
    }

    private void deliver(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            emailSender.send(to, subject, htmlBody);
        } catch (Exception e) {
            // Logged, never rethrown: the user's action already succeeded.
            log.error("mail delivery failed to={} subject={}", to, subject, e);
        }
    }

    private String url(String path) {
        return mailProperties.getWebBaseUrl() + path;
    }

    /**
     * Flat snapshot of what the confirmation email needs, built inside the
     * caller's transaction while the associations are still attached.
     */
    public record BookingMail(
            String email,
            String name,
            String fromLocation,
            String toLocation,
            LocalDate travelDate,
            int partySize,
            String flightNo,
            Integer farePerPerson
    ) {
        public static BookingMail of(Booking booking, Integer farePerPerson) {
            User user = booking.getUser();
            return new BookingMail(
                    user.getEmail(),
                    user.getName(),
                    booking.getRoute().getFromLocation(),
                    booking.getRoute().getToLocation(),
                    booking.getTravelDate(),
                    booking.getPartySize(),
                    booking.getFlightNo(),
                    farePerPerson);
        }
    }
}
