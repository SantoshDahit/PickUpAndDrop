package com.pickupdrop.controller;

import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.service.BookingFacade;
import com.pickupdrop.util.AuthorizationUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingFacade bookingFacade;

    /** Create a booking; GROUP preference triggers 7-day matching. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto.Response create(@RequestBody @Valid BookingDto.PostRequest request) {
        return bookingFacade.create(AuthorizationUtil.getCurrentUser().getUserId(), request);
    }

    /** My bookings, newest first. */
    @GetMapping("/me")
    public List<BookingDto.SummaryResponse> getMyBookings() {
        return bookingFacade.getMyBookings(AuthorizationUtil.getCurrentUser().getUserId());
    }

    /** Move my preferred landing day (group members converge via chat). */
    @PatchMapping("/{bookingId}")
    public BookingDto.Response updateTravelDate(
            @PathVariable String bookingId,
            @RequestBody @Valid BookingDto.PatchRequest request) {
        return bookingFacade.updateTravelDate(
                AuthorizationUtil.getCurrentUser().getUserId(), bookingId, request);
    }

    /** Cancel my booking (frees group seats). */
    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String bookingId) {
        bookingFacade.cancel(AuthorizationUtil.getCurrentUser().getUserId(), bookingId);
    }
}
