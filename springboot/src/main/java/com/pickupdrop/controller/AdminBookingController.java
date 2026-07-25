package com.pickupdrop.controller;

import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.service.BookingFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingFacade bookingFacade;

    /** Ops overview: filter by route/status/date range. ADMIN only (route rule). */
    @GetMapping("/search")
    public Page<BookingDto.SummaryResponse> search(
            @ModelAttribute BookingDto.SearchRequest searchRequest,
            Pageable pageable) {
        return bookingFacade.search(searchRequest, pageable);
    }
}
