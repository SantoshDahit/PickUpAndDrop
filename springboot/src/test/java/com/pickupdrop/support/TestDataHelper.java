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

    public BookingDto.Response createGroupBooking(User user, LocalDate travelDate, int partySize) {
        return bookingFacade.create(user.getId(), new BookingDto.PostRequest(
                firstRoute().getId(), travelDate, null, partySize, MatchPref.GROUP,
                "hello from " + user.getName(), null, null));
    }
}
