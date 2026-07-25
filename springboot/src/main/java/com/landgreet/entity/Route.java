package com.landgreet.entity;

import com.landgreet.entity.base.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "route")
@Getter
@NoArgsConstructor
public class Route extends BaseTimeEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @Column(name = "from_location", nullable = false)
    private String fromLocation;

    @Column(name = "to_location", nullable = false)
    private String toLocation;

    @Column(name = "active", nullable = false)
    private boolean active;

    public Route(String fromLocation, String toLocation) {
        this.id = UUID.randomUUID().toString();
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.active = true;
    }
}
