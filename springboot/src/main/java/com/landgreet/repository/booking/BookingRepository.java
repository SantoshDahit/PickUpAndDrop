package com.landgreet.repository.booking;

import com.landgreet.dto.BookingDto;
import com.landgreet.entity.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingRepository {

    Optional<Booking> findById(String id);

    Booking save(Booking booking);

    List<Booking> findAllByUserId(String userId);

    List<Booking> findActiveByGroupId(String groupId);

    Page<Booking> search(BookingDto.SearchRequest searchRequest, Pageable pageable);
}
