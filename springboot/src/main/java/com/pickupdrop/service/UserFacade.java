package com.pickupdrop.service;

import com.pickupdrop.dto.UserDto;
import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.User;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final BookingService bookingService;
    private final BookingFacade bookingFacade;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto.Response getMe(String userId) {
        return userMapper.toResponse(userService.getById(userId));
    }

    @Transactional
    public UserDto.Response updateMe(String userId, UserDto.PatchRequest request) {
        User user = userService.getById(userId);
        user.update(request.name(), request.phone());
        return userMapper.toResponse(user);
    }

    @Transactional
    public void updatePassword(String userId, UserDto.PasswordPatchRequest request) {
        User user = userService.getById(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.USER_PASSWORD_NOT_MATCH);
        }
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * Soft delete with password re-auth. Active bookings are cancelled and
     * their groups refreshed; the email slot is freed for re-registration.
     */
    @Transactional
    public void deleteMe(String userId, UserDto.DeleteRequest request) {
        User user = userService.getById(userId);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.USER_PASSWORD_NOT_MATCH);
        }
        for (Booking booking : bookingService.getAllByUserId(userId)) {
            if (booking.isActive()) {
                bookingFacade.cancelInternal(booking);
            }
        }
        user.releaseEmailOnDelete();
        user.softDelete();
    }
}
