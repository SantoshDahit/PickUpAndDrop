package com.pickupdrop.repository.emailverification;

import com.pickupdrop.entity.EmailVerification;
import com.pickupdrop.enums.EmailPurpose;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EmailVerificationRepositoryImpl implements EmailVerificationRepository {

    private final EmailVerificationJpaRepository emailVerificationJpaRepository;

    @Override
    public Optional<EmailVerification> findByCodeHash(String codeHash) {
        return emailVerificationJpaRepository.findByCodeHash(codeHash);
    }

    @Override
    public EmailVerification save(EmailVerification emailVerification) {
        return emailVerificationJpaRepository.save(emailVerification);
    }

    @Override
    public int deletePendingByUserIdAndPurpose(String userId, EmailPurpose purpose) {
        return emailVerificationJpaRepository.deletePendingByUserIdAndPurpose(userId, purpose);
    }
}
