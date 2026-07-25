package com.landgreet.entity;

import com.landgreet.entity.base.BaseCreateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_message")
@Getter
@NoArgsConstructor
public class GroupMessage extends BaseCreateEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroup travelGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    public GroupMessage(TravelGroup travelGroup, User user, String body) {
        this.id = UUID.randomUUID().toString();
        this.travelGroup = travelGroup;
        this.user = user;
        this.body = body;
    }
}
