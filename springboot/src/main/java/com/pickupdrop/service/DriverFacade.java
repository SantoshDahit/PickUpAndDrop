package com.pickupdrop.service;

import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.Driver;
import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.GroupStatus;
import com.pickupdrop.enums.Role;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.DriverMapper;
import com.pickupdrop.mapper.RouteMapper;
import com.pickupdrop.security.service.AuthFacade;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DriverFacade {

    private final DriverService driverService;
    private final TravelGroupService travelGroupService;
    private final BookingService bookingService;
    private final UserService userService;
    private final DriverMapper driverMapper;
    private final RouteMapper routeMapper;
    private final PasswordEncoder passwordEncoder;

    // ===== Roster CRUD =====

    @Transactional
    public DriverDto.Response create(DriverDto.PostRequest request) {
        Driver driver = new Driver(request.name().trim(), blankToNull(request.phone()),
                blankToNull(request.licenseNo()), request.ownsVehicle(),
                blankToNull(request.vehicle()), blankToNull(request.plateNo()), request.seats());
        return driverMapper.toResponse(driverService.save(driver));
    }

    @Transactional(readOnly = true)
    public DriverDto.Response getById(String driverId) {
        return driverMapper.toResponse(driverService.getById(driverId));
    }

    @Transactional(readOnly = true)
    public Page<DriverDto.SummaryResponse> search(DriverDto.SearchRequest searchRequest, Pageable pageable) {
        return driverService.search(searchRequest, pageable).map(driverMapper::toSummaryResponse);
    }

    @Transactional
    public DriverDto.Response update(String driverId, DriverDto.PatchRequest request) {
        Driver driver = driverService.getById(driverId);
        driver.update(request.name(), request.phone(), request.licenseNo(), request.ownsVehicle(),
                request.vehicle(), request.plateNo(), request.seats());
        return driverMapper.toResponse(driver);
    }

    @Transactional
    public DriverDto.Response updateStatus(String driverId, DriverDto.StatusPatchRequest request) {
        Driver driver = driverService.getById(driverId);
        driver.updateStatus(request.status());
        return driverMapper.toResponse(driver);
    }

    /** Soft delete; refused while the driver has rides dated today or later. */
    @Transactional
    public void delete(String driverId) {
        Driver driver = driverService.getById(driverId);
        if (bookingService.existsUpcomingByDriverId(driverId, LocalDate.now())) {
            throw new ApiException(ErrorCode.DRIVER_HAS_UPCOMING_RIDES);
        }
        driver.softDelete();
    }

    // ===== Driver login accounts (admin-created; no self-signup) =====

    @Transactional
    public DriverDto.Response createAccount(String driverId, DriverDto.AccountPostRequest request) {
        Driver driver = driverService.getById(driverId);
        if (driver.hasAccount()) {
            throw new ApiException(ErrorCode.DRIVER_ACCOUNT_EXISTS);
        }
        String email = AuthFacade.normalizeEmail(request.email());
        if (userService.existsActiveByEmail(email)) {
            throw new ApiException(ErrorCode.USER_DUPLICATE_EMAIL);
        }
        User account = userService.save(new User(email, passwordEncoder.encode(request.password()),
                driver.getName(), driver.getPhone(), Role.DRIVER));
        driver.linkAccount(account);
        return driverMapper.toResponse(driver);
    }

    // ===== Driver portal (resolved from the token's user, never a parameter) =====

    @Transactional(readOnly = true)
    public DriverDto.Response getMyProfile(String userId) {
        return driverMapper.toResponse(driverService.getByUserId(userId));
    }

    @Transactional
    public DriverDto.Response updateMyProfile(String userId, DriverDto.MePatchRequest request) {
        Driver driver = driverService.getByUserId(userId);
        driver.updateOwnProfile(request.phone(), request.vehicle(), request.plateNo());
        return driverMapper.toResponse(driver);
    }

    /** Upcoming assigned rides: grouped rolls up members; individual is one passenger. */
    @Transactional(readOnly = true)
    public List<DriverDto.RideResponse> getMyRides(String userId) {
        Driver driver = driverService.getByUserId(userId);
        LocalDate today = LocalDate.now();
        List<DriverDto.RideResponse> rides = new ArrayList<>();

        for (TravelGroup group : travelGroupService.getAllByDriverId(driver.getId())) {
            List<Booking> members = bookingService.getActiveByGroupId(group.getId());
            List<Booking> upcoming = members.stream()
                    .filter(b -> !b.getTravelDate().isBefore(today))
                    .toList();
            if (upcoming.isEmpty()) {
                continue;
            }
            rides.add(new DriverDto.RideResponse(
                    "GROUP", group.getId(), routeMapper.toResponse(group.getRoute()),
                    upcoming.stream().map(Booking::getTravelDate).min(Comparator.naturalOrder()).orElseThrow(),
                    upcoming.stream().map(Booking::getTravelDate).max(Comparator.naturalOrder()).orElseThrow(),
                    upcoming.stream().mapToInt(Booking::getPartySize).sum(),
                    upcoming.stream().map(DriverFacade::toPassenger).toList()));
        }

        for (Booking booking : bookingService.getActiveIndividualByDriverId(driver.getId(), today)) {
            rides.add(new DriverDto.RideResponse(
                    "INDIVIDUAL", booking.getId(), routeMapper.toResponse(booking.getRoute()),
                    booking.getTravelDate(), booking.getTravelDate(), booking.getPartySize(),
                    List.of(toPassenger(booking))));
        }

        rides.sort(Comparator.comparing(DriverDto.RideResponse::getEarliestDate));
        return rides;
    }

    private static DriverDto.PassengerResponse toPassenger(Booking booking) {
        String name = booking.getUser().getName().trim();
        int space = name.indexOf(' ');
        return new DriverDto.PassengerResponse(
                space > 0 ? name.substring(0, space) : name,
                booking.getPartySize(), booking.getTravelDate(),
                booking.getFlightNo(), booking.getContact());
    }

    // ===== Assignment =====

    @Transactional
    public void assignToGroup(String groupId, DriverDto.AssignRequest request) {
        TravelGroup group = travelGroupService.getById(groupId);
        if (group.getStatus() == GroupStatus.CLOSED) {
            throw new ApiException(ErrorCode.GROUP_IS_NOT_FOUND);
        }
        Driver driver = assignableDriver(request.driverId());
        List<Booking> members = bookingService.getActiveByGroupId(groupId);
        int seats = members.stream().mapToInt(Booking::getPartySize).sum();
        if (seats > driver.getSeats()) {
            throw new ApiException(ErrorCode.DRIVER_SEATS_INSUFFICIENT);
        }
        group.assignDriver(driver);
        travelGroupService.save(group);
    }

    @Transactional
    public void unassignFromGroup(String groupId) {
        TravelGroup group = travelGroupService.getById(groupId);
        group.unassignDriver();
        travelGroupService.save(group);
    }

    @Transactional
    public void assignToBooking(String bookingId, DriverDto.AssignRequest request) {
        Booking booking = bookingService.getById(bookingId);
        if (booking.isGrouped()) {
            throw new ApiException(ErrorCode.BOOKING_IS_GROUPED);
        }
        if (!booking.isActive()) {
            throw new ApiException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        Driver driver = assignableDriver(request.driverId());
        if (booking.getPartySize() > driver.getSeats()) {
            throw new ApiException(ErrorCode.DRIVER_SEATS_INSUFFICIENT);
        }
        booking.assignDriver(driver);
        bookingService.save(booking);
    }

    @Transactional
    public void unassignFromBooking(String bookingId) {
        Booking booking = bookingService.getById(bookingId);
        booking.unassignDriver();
        bookingService.save(booking);
    }

    private Driver assignableDriver(String driverId) {
        Driver driver = driverService.getById(driverId);
        if (!driver.isAssignable()) {
            throw new ApiException(ErrorCode.DRIVER_IS_NOT_ASSIGNABLE);
        }
        return driver;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
