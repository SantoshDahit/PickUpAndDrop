package com.pickupdrop.controller;

import com.pickupdrop.dto.SupportDto;
import com.pickupdrop.service.SupportFacade;
import com.pickupdrop.util.AuthorizationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A traveller's own conversation with the team (plan 014). */
@RestController
@RequestMapping("/v1/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportFacade supportFacade;

    /** My thread. There is no id: a traveller has exactly one. */
    @GetMapping("/messages")
    public SupportDto.ThreadResponse myThread() {
        return supportFacade.getMyThread(AuthorizationUtil.getCurrentUser().getUserId());
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportDto.MessageResponse send(@RequestBody @Valid SupportDto.PostRequest request) {
        return supportFacade.postFromTraveller(
                AuthorizationUtil.getCurrentUser().getUserId(), request);
    }
}
