package com.pickupdrop.security.service;

import com.pickupdrop.entity.EmailVerification;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.EmailPurpose;
import com.pickupdrop.enums.EmailStatus;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.repository.emailverification.EmailVerificationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and redeems emailed verification codes. The raw code exists only in
 * the email; the table keeps its SHA-256, so a DB leak yields nothing usable.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_BYTES = 32;

    private final EmailVerificationRepository emailVerificationRepository;

    /**
     * Creates a code for this user and purpose and returns the raw value to
     * email. Any outstanding PENDING code for the same purpose is retired
     * first, so exactly one live link exists per user per flow.
     */
    @Transactional
    public String issue(User user, EmailPurpose purpose) {
        emailVerificationRepository.deletePendingByUserIdAndPurpose(user.getId(), purpose);
        byte[] bytes = new byte[CODE_BYTES];
        RANDOM.nextBytes(bytes);
        String rawCode = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        emailVerificationRepository.save(
                new EmailVerification(user, user.getEmail(), purpose, hash(rawCode)));
        return rawCode;
    }

    /**
     * Consumes a code and returns the account it belongs to.
     *
     * @throws ApiException if the code is unknown, expired, already used, or
     *                      was issued for a different purpose — one error for
     *                      all cases, so the response reveals nothing about
     *                      which codes exist.
     */
    @Transactional
    public User redeem(String rawCode, EmailPurpose purpose) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        EmailVerification verification = emailVerificationRepository.findByCodeHash(hash(rawCode))
                .orElseThrow(() -> new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        if (!purpose.equals(verification.getPurpose()) || !verification.isRedeemable()) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        verification.updateStatus(EmailStatus.USED);
        emailVerificationRepository.save(verification);
        return verification.getUser();
    }

    private static String hash(String rawCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                   .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for email verification", e);
        }
    }
}
