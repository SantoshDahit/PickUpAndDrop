package com.landgreet.support;

import com.landgreet.dto.BookingDto;
import com.landgreet.entity.Driver;
import com.landgreet.entity.Route;
import com.landgreet.entity.User;
import com.landgreet.enums.MatchPref;
import com.landgreet.enums.Role;
import com.landgreet.service.BookingFacade;
import com.landgreet.service.DriverService;
import com.landgreet.service.RouteService;
import com.landgreet.service.UserService;
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

    public BookingDto.Response createGroupBooking(User user, LocalDate travelDate, int partySize) {
        return bookingFacade.create(user.getId(), new BookingDto.PostRequest(
                firstRoute().getId(), travelDate, null, partySize, MatchPref.GROUP,
                "hello from " + user.getName(), null, null));
    }
}
