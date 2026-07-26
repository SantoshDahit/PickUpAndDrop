package com.pickupdrop.controller;

import com.pickupdrop.dto.GroupMessageDto;
import com.pickupdrop.dto.TravelGroupDto;
import com.pickupdrop.security.dto.UserDetail;
import com.pickupdrop.service.TravelGroupFacade;
import com.pickupdrop.util.AuthorizationUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/groups")
@RequiredArgsConstructor
public class TravelGroupController {

    private final TravelGroupFacade travelGroupFacade;

    /** Joinable admin-published rides — no personal data. Any signed-in user. */
    @GetMapping("/open")
    public List<TravelGroupDto.OpenRideResponse> openRides() {
        return travelGroupFacade.listOpenRides();
    }

    /** Group view: members-only (admin may view); non-members get 404. */
    @GetMapping("/{groupId}")
    public TravelGroupDto.Response getById(@PathVariable String groupId) {
        UserDetail current = AuthorizationUtil.getCurrentUser();
        return travelGroupFacade.getById(current.getUserId(), current.isAdmin(), groupId);
    }

    /** Group chat history. */
    @GetMapping("/{groupId}/messages")
    public List<GroupMessageDto.Response> getMessages(@PathVariable String groupId) {
        UserDetail current = AuthorizationUtil.getCurrentUser();
        return travelGroupFacade.getMessages(current.getUserId(), current.isAdmin(), groupId);
    }

    /** Post a chat message (members only). */
    @PostMapping("/{groupId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMessageDto.Response postMessage(
            @PathVariable String groupId,
            @RequestBody @Valid GroupMessageDto.PostRequest request) {
        return travelGroupFacade.postMessage(
                AuthorizationUtil.getCurrentUser().getUserId(), groupId, request);
    }

    /** Leave the group — my booking continues individually. */
    @DeleteMapping("/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable String groupId) {
        travelGroupFacade.leave(AuthorizationUtil.getCurrentUser().getUserId(), groupId);
    }
}
