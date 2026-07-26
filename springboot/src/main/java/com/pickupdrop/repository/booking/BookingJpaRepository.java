package com.pickupdrop.repository.booking;

import com.pickupdrop.entity.Booking;
import com.pickupdrop.enums.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingJpaRepository extends JpaRepository<Booking, String> {

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<Booking> findAllByTravelGroupIdAndStatusOrderByCreatedAtAsc(String groupId, BookingStatus status);

    List<Booking> findAllByDriverIdAndStatusAndTravelDateGreaterThanEqualOrderByTravelDateAsc(
            String driverId, BookingStatus status, LocalDate from);

    boolean existsByDriverIdAndStatusAndTravelDateGreaterThanEqual(
            String driverId, BookingStatus status, LocalDate from);

    boolean existsByTravelGroupDriverIdAndStatusAndTravelDateGreaterThanEqual(
            String driverId, BookingStatus status, LocalDate from);

    boolean existsByRouteId(String routeId);
}
