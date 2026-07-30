package com.pickupdrop.service;

import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.repository.booking.BookingRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public Booking getById(String id) {
        return bookingRepository.findById(id)
                .filter(booking -> !booking.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_IS_NOT_FOUND));
    }

    @Transactional
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllByUserId(String userId) {
        return bookingRepository.findAllByUserId(userId);
    }

    /** Active bookings on a route inside a landing week — admin add candidates. */
    @Transactional(readOnly = true)
    public List<Booking> getActiveByRouteIdAndWeek(String routeId, LocalDate from, LocalDate to) {
        return bookingRepository.findActiveByRouteIdAndTravelDateBetween(routeId, from, to);
    }

    @Transactional(readOnly = true)
    public List<Booking> getActiveByGroupId(String groupId) {
        return bookingRepository.findActiveByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public Page<Booking> search(BookingDto.SearchRequest searchRequest, Pageable pageable) {
        return bookingRepository.search(searchRequest, pageable);
    }

    @Transactional(readOnly = true)
    public List<Booking> getActiveIndividualByDriverId(String driverId, LocalDate from) {
        return bookingRepository.findActiveIndividualByDriverId(driverId, from);
    }

    @Transactional(readOnly = true)
    public boolean existsUpcomingByDriverId(String driverId, LocalDate from) {
        return bookingRepository.existsUpcomingByDriverId(driverId, from);
    }

    @Transactional(readOnly = true)
    public boolean existsByRouteId(String routeId) {
        return bookingRepository.existsByRouteId(routeId);
    }
}
