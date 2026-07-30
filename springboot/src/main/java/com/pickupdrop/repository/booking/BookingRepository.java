package com.pickupdrop.repository.booking;

import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.entity.Booking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingRepository {

    Optional<Booking> findById(String id);

    Booking save(Booking booking);

    List<Booking> findAllByUserId(String userId);

    List<Booking> findActiveByGroupId(String groupId);

    List<Booking> findActiveIndividualByDriverId(String driverId, LocalDate from);

    /** Active bookings on a route whose travel date falls inside a landing week. */
    List<Booking> findActiveByRouteIdAndTravelDateBetween(String routeId, LocalDate from, LocalDate to);

    Page<Booking> search(BookingDto.SearchRequest searchRequest, Pageable pageable);

    /** Any active ride (group or individual) with this driver dated {@code from} or later? */
    boolean existsUpcomingByDriverId(String driverId, LocalDate from);

    boolean existsByRouteId(String routeId);
}
