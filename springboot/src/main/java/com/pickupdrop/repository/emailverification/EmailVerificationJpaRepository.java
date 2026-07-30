package com.pickupdrop.repository.emailverification;

import com.pickupdrop.entity.EmailVerification;
import com.pickupdrop.enums.EmailPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerification, String> {

    Optional<EmailVerification> findByCodeHash(String codeHash);

    // Bulk JPQL so the delete lands before the replacement insert flushes
    // (code_hash carries a unique key). Flush first, clear after, so no stale
    // managed row can be re-flushed — same shape as PriceTierJpaRepository.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           delete from EmailVerification v
            where v.user.id = :userId
              and v.purpose = :purpose
              and v.status = com.pickupdrop.enums.EmailStatus.PENDING
           """)
    int deletePendingByUserIdAndPurpose(@Param("userId") String userId,
                                       @Param("purpose") EmailPurpose purpose);
}
