package com.pickupdrop.util;

import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.security.dto.UserDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthorizationUtil {

    private AuthorizationUtil() {
    }

    /** Current authenticated user; throws when the context has no valid principal. */
    public static UserDetail getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetail userDetail)) {
            throw new ApiException(ErrorCode.JWT_TOKEN_IS_INVALID);
        }
        return userDetail;
    }
}
