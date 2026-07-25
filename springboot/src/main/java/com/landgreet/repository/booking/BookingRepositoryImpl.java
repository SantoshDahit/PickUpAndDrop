package com.landgreet.repository.booking;

import com.landgreet.dto.BookingDto;
import com.landgreet.entity.Booking;
import com.landgreet.enums.BookingStatus;
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
}
