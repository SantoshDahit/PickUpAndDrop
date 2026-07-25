package com.pickupdrop.entity;

import com.pickupdrop.entity.base.BaseTimeEntity;
import com.pickupdrop.enums.GroupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "travel_group")
@Getter
@NoArgsConstructor
public class TravelGroup extends BaseTimeEntity {

    public static final int MAX_SEATS = 6;

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    public TravelGroup(Route route) {
        this.id = UUID.randomUUID().toString();
        this.route = route;
        this.status = GroupStatus.OPEN;
    }

    public void updateStatus(GroupStatus status) {
        this.status = status;
    }

    public void assignDriver(Driver driver) {
        this.driver = driver;
    }

    public void unassignDriver() {
        this.driver = null;
    }
}
