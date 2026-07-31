package com.pickupdrop.entity;

import com.pickupdrop.entity.base.BaseCreateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * One line in a traveller's conversation with the team (plan 014).
 *
 * <p>There is no thread table: a thread is every row sharing {@code user} — the
 * traveller it belongs to. {@code author} is who actually typed it, so a staff
 * reply records which operator sent it while still hanging off the traveller's
 * thread.
 */
@Entity
@Table(name = "support_message")
@Getter
@NoArgsConstructor
public class SupportMessage extends BaseCreateEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    /** The traveller whose thread this is — never the admin, even for replies. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Written by the team. Fixed at write time rather than derived from the
     * author's role, for the same reason as {@code group_message.staff}: role is
     * mutable and must not relabel history (plan 012 §4.2).
     */
    @Column(name = "staff", nullable = false)
    private boolean staff;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public SupportMessage(User user, User author, boolean staff, String body) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.author = author;
        this.staff = staff;
        this.body = body;
    }

    public void markRead() {
        if (readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isUnread() {
        return readAt == null;
    }
}
