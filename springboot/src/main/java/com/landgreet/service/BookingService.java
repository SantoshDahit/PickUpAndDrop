package com.landgreet.service;

import com.landgreet.dto.BookingDto;
import com.landgreet.entity.Booking;
import com.landgreet.exception.ApiException;
import com.landgreet.exception.ErrorCode;
import com.landgreet.repository.booking.BookingRepository;
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

    @Transactional(readOnly = true)
    public List<Booking> getActiveByGroupId(String groupId) {
        return bookingRepository.findActiveByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public Page<Booking> search(BookingDto.SearchRequest searchRequest, Pageable pageable) {
        return bookingRepository.search(searchRequest, pageable);
    }
}
