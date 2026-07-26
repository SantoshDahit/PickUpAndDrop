package com.pickupdrop.support;

import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.entity.Driver;
import com.pickupdrop.entity.Route;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.MatchPref;
import com.pickupdrop.enums.Role;
import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.service.BookingFacade;
import com.pickupdrop.service.DriverFacade;
import com.pickupdrop.service.DriverService;
import com.pickupdrop.service.RouteService;
import com.pickupdrop.service.UserService;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestComponent
@RequiredArgsConstructor
public class TestDataHelper {

    public static final String PASSWORD = "secret1";

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final UserService userService;
    private final RouteService routeService;
    private final BookingFacade bookingFacade;
    private final DriverService driverService;
    private final DriverFacade driverFacade;
    private final PasswordEncoder passwordEncoder;

    public User createUser() {
        int n = SEQ.incrementAndGet();
        return userService.save(new User(
                "traveller" + n + "-" + System.nanoTime() + "@example.com",
                passwordEncoder.encode(PASSWORD),
                "Traveller " + n, null, Role.USER));
    }

    public User createAdmin() {
        int n = SEQ.incrementAndGet();
        return userService.save(new User(
                "admin" + n + "-" + System.nanoTime() + "@example.com",
                passwordEncoder.encode(PASSWORD),
                "Admin " + n, null, Role.ADMIN));
    }

    public Route firstRoute() {
        return routeService.getAllActive().get(0);
    }

    public Driver createDriver(int seats) {
        int n = SEQ.incrementAndGet();
        return driverService.save(new Driver("Driver " + n, "+82 10-2222-" + String.format("%04d", n),
                "LIC-" + n, true, "Hyundai Staria", n + "가" + (1000 + n), seats));
    }

    /** Creates a login for the driver and returns the account user (password = PASSWORD). */
    public User createDriverAccount(Driver driver) {
        String email = "drv" + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@example.com";
        driverFacade.createAccount(driver.getId(), new DriverDto.AccountPostRequest(email, PASSWORD));
        return userService.getActiveByEmail(email);
    }

    /**
     * Books a trip then selects a group the plan-008 way: join the first
     * suggested same-week group, else start a new one.
     */
    public BookingDto.Response createGroupBooking(User user, LocalDate travelDate, int partySize) {
        BookingDto.Response created = bookingFacade.create(user.getId(), new BookingDto.PostRequest(
                firstRoute().getId(), null, travelDate, null, partySize, MatchPref.GROUP,
                "hello from " + user.getName(), null, null));
        var suggestions = bookingFacade.suggestGroups(user.getId(), created.getId());
        String groupId = suggestions.getGroups().isEmpty() ? null : suggestions.getGroups().get(0).getId();
        return bookingFacade.selectGroup(user.getId(), created.getId(),
                new BookingDto.SelectGroupRequest(groupId));
    }

    /**
     * A future date anchored to a landing-week start (day 1/8/15/22) so the
     * date and date+3 always share a bucket; W5 anchors back to W4.
     */
    public LocalDate groupableDate(int offsetDays) {
        LocalDate d = LocalDate.now().plusDays(offsetDays);
        int weekStart = Math.min(3, (d.getDayOfMonth() - 1) / 7) * 7 + 1; // 1/8/15/22
        LocalDate anchored = d.withDayOfMonth(weekStart);
        return anchored.isBefore(LocalDate.now().plusDays(1)) ? anchored.plusDays(7) : anchored;
    }
}
