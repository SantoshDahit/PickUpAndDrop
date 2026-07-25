package com.landgreet.repository.booking;

import com.landgreet.entity.Booking;
import com.landgreet.enums.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingJpaRepository extends JpaRepository<Booking, String> {

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<Booking> findAllByTravelGroupIdAndStatusOrderByCreatedAtAsc(String groupId, BookingStatus status);

    boolean existsByDriverIdAndStatusAndTravelDateGreaterThanEqual(
            String driverId, BookingStatus status, LocalDate from);

    boolean existsByTravelGroupDriverIdAndStatusAndTravelDateGreaterThanEqual(
            String driverId, BookingStatus status, LocalDate from);
}
