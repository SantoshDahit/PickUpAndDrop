package com.pickupdrop.repository.emailverification;

import com.pickupdrop.entity.EmailVerification;
import com.pickupdrop.enums.EmailPurpose;
import java.util.Optional;

public interface EmailVerificationRepository {

    Optional<EmailVerification> findByCodeHash(String codeHash);

    EmailVerification save(EmailVerification emailVerification);

    /** Issuing a new code retires any outstanding one for the same purpose. */
    int deletePendingByUserIdAndPurpose(String userId, EmailPurpose purpose);
}
