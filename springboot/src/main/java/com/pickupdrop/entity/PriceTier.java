package com.pickupdrop.entity;

import com.pickupdrop.entity.base.BaseTimeEntity;
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
@Table(name = "price_tier")
@Getter
@NoArgsConstructor
public class PriceTier extends BaseTimeEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "group_size", nullable = false)
    private int groupSize;

    @Column(name = "price_per_person", nullable = false)
    private int pricePerPerson;

    public PriceTier(Route route, int groupSize, int pricePerPerson) {
        this.id = UUID.randomUUID().toString();
        this.route = route;
        this.groupSize = groupSize;
        this.pricePerPerson = pricePerPerson;
    }
}
