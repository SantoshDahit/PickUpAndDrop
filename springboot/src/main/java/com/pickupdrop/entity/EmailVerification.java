package com.pickupdrop.entity;

import com.pickupdrop.entity.base.BaseTimeEntity;
import com.pickupdrop.enums.EmailPurpose;
import com.pickupdrop.enums.EmailStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One emailed verification code, for any email flow (convention 17's
 * {@code SmsVerification}, for the email channel).
 *
 * <p>Only the SHA-256 of the emailed code is stored, so the table alone cannot
 * be used to take over an account. Validity comes from {@link EmailPurpose}.
 */
@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor
public class EmailVerification extends BaseTimeEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    /** Null for flows that verify an address before an account exists. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** The address the code was sent to, as it was at issue time. */
    @Column(name = "contact", nullable = false)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private EmailPurpose purpose;

    @Column(name = "code_hash", nullable = false, columnDefinition = "char(64)")
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public EmailVerification(User user, String contact, EmailPurpose purpose, String codeHash) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.contact = contact;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.status = EmailStatus.PENDING;
        this.expiresAt = LocalDateTime.now().plusMinutes(purpose.getValidMinutes());
    }

    public void updateStatus(EmailStatus status) {
        if (EmailStatus.VERIFIED.equals(status)) {
            this.verifiedAt = LocalDateTime.now();
        }
        if (EmailStatus.USED.equals(status)) {
            this.usedAt = LocalDateTime.now();
        }
        this.status = status;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Spendable right now: still PENDING and inside its window. A link-based
     * flow goes straight from PENDING to USED, so VERIFIED is not accepted here.
     */
    public boolean isRedeemable() {
        return EmailStatus.PENDING.equals(status) && !isExpired();
    }
}
