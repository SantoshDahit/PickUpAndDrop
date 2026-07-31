package com.pickupdrop.controller;

import com.pickupdrop.dto.SupportDto;
import com.pickupdrop.service.SupportFacade;
import com.pickupdrop.util.AuthorizationUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Support inbox for the operator. ADMIN via the /v1/admin/** route rule. */
@RestController
@RequestMapping("/v1/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportFacade supportFacade;

    /** Who has written, newest activity first, with unread counts. */
    @GetMapping
    public List<SupportDto.InboxRowResponse> inbox() {
        return supportFacade.getInbox();
    }

    /** One traveller's thread; opening it marks their messages read. */
    @GetMapping("/{userId}/messages")
    public SupportDto.ThreadResponse thread(@PathVariable String userId) {
        return supportFacade.getThreadForAdmin(
                AuthorizationUtil.getCurrentUser().getUserId(), userId);
    }

    @PostMapping("/{userId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportDto.MessageResponse reply(@PathVariable String userId,
                                            @RequestBody @Valid SupportDto.PostRequest request) {
        return supportFacade.postFromStaff(
                AuthorizationUtil.getCurrentUser().getUserId(), userId, request);
    }
}
