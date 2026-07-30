package com.pickupdrop.controller;

import com.pickupdrop.dto.GroupMessageDto;
import com.pickupdrop.dto.TravelGroupDto;
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

/**
 * Chat moderation for the operator (plan 012). ADMIN via the {@code /v1/admin/**}
 * route rule.
 *
 * <p>Deliberately separate endpoints from {@code /v1/groups/**} rather than a
 * widened payload: the customer responses are a privacy contract (002 §4.5), and
 * keeping the admin view in its own response type means no traveller route can
 * ever return an email or a phone number.
 */
@RestController
@RequestMapping("/v1/admin/groups")
@RequiredArgsConstructor
public class AdminGroupChatController {

    private final TravelGroupFacade travelGroupFacade;

    /** Chat index — most recently active first, silent groups last. */
    @GetMapping("/chats")
    public List<TravelGroupDto.ChatSummaryResponse> listChats() {
        return travelGroupFacade.listChats();
    }

    /** Group detail with full member identities and contact details. */
    @GetMapping("/{groupId}/detail")
    public TravelGroupDto.AdminDetailResponse detail(@PathVariable String groupId) {
        return travelGroupFacade.getAdminDetail(groupId);
    }

    /** Transcript with real author names. */
    @GetMapping("/{groupId}/messages")
    public List<GroupMessageDto.AdminResponse> messages(@PathVariable String groupId) {
        return travelGroupFacade.getAdminMessages(groupId);
    }

    /** Reply as the Pickup &amp; Drop team; membership not required. */
    @PostMapping("/{groupId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMessageDto.AdminResponse reply(
            @PathVariable String groupId,
            @RequestBody @Valid GroupMessageDto.PostRequest request) {
        return travelGroupFacade.postStaffMessage(
                AuthorizationUtil.getCurrentUser().getUserId(), groupId, request);
    }

    /** Bookings that may be added to this group — already rule-checked. */
    @GetMapping("/{groupId}/candidates")
    public List<TravelGroupDto.CandidateResponse> candidates(@PathVariable String groupId) {
        return travelGroupFacade.listAddCandidates(groupId);
    }

    /** Add a booking to the group (same route, same landing week, seats must fit). */
    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable String groupId,
                          @RequestBody @Valid TravelGroupDto.AdminAddMemberRequest request) {
        travelGroupFacade.addMember(groupId, request);
    }

    /** Remove a booking from the group; the booking stays active. */
    @DeleteMapping("/{groupId}/members/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable String groupId, @PathVariable String bookingId) {
        travelGroupFacade.removeMember(groupId, bookingId);
    }
}
