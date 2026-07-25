package com.pickupdrop.repository.booking;

import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.enums.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {

    private final BookingJpaRepository bookingJpaRepository;
    private final BookingQueryRepository bookingQueryRepository;

    @Override
    public Optional<Booking> findById(String id) {
        return bookingJpaRepository.findById(id);
    }

    @Override
    public Booking save(Booking booking) {
        return bookingJpaRepository.save(booking);
    }

    @Override
    public List<Booking> findAllByUserId(String userId) {
        return bookingJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Booking> findActiveByGroupId(String groupId) {
        return bookingJpaRepository.findAllByTravelGroupIdAndStatusOrderByCreatedAtAsc(groupId, BookingStatus.ACTIVE);
    }

    @Override
    public Page<Booking> search(BookingDto.SearchRequest searchRequest, Pageable pageable) {
        return bookingQueryRepository.search(searchRequest, pageable);
    }

    @Override
    public boolean existsUpcomingByDriverId(String driverId, LocalDate from) {
        return bookingJpaRepository.existsByDriverIdAndStatusAndTravelDateGreaterThanEqual(
                driverId, BookingStatus.ACTIVE, from)
                || bookingJpaRepository.existsByTravelGroupDriverIdAndStatusAndTravelDateGreaterThanEqual(
                driverId, BookingStatus.ACTIVE, from);
    }
}
