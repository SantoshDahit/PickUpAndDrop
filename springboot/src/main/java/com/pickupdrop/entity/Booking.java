package com.pickupdrop.entity;

import com.pickupdrop.entity.base.BaseFullTimeEntity;
import com.pickupdrop.enums.BookingStatus;
import com.pickupdrop.enums.MatchPref;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "booking")
@Getter
@NoArgsConstructor
public class Booking extends BaseFullTimeEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TravelGroup travelGroup;   // null = riding individually

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;             // individual rides only; grouped rides use the group's driver

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "flight_no")
    private String flightNo;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_pref", nullable = false)
    private MatchPref matchPref;

    @Column(name = "intro")
    private String intro;              // shown to group members

    @Column(name = "contact")
    private String contact;            // driver/admin only

    @Column(name = "notes")
    private String notes;              // driver/admin only

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    public Booking(User user, Route route, LocalDate travelDate, String flightNo, int partySize,
                   MatchPref matchPref, String intro, String contact, String notes) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.route = route;
        this.travelDate = travelDate;
        this.flightNo = flightNo;
        this.partySize = partySize;
        this.matchPref = matchPref;
        this.intro = intro;
        this.contact = contact;
        this.notes = notes;
        this.status = BookingStatus.ACTIVE;
    }

    public void forceGroupPref() {
        this.matchPref = MatchPref.GROUP;
    }

    public void joinGroup(TravelGroup travelGroup) {
        this.travelGroup = travelGroup;
    }

    public void leaveGroup() {
        this.travelGroup = null;
        this.matchPref = MatchPref.INDIVIDUAL;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
        this.travelGroup = null;
    }

    public void updateTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public void assignDriver(Driver driver) {
        this.driver = driver;
    }

    public void unassignDriver() {
        this.driver = null;
    }

    /** Effective driver: the group's when grouped, else this booking's own. */
    public Driver effectiveDriver() {
        return travelGroup != null ? travelGroup.getDriver() : driver;
    }

    public boolean isActive() {
        return status == BookingStatus.ACTIVE;
    }

    public boolean isGrouped() {
        return travelGroup != null;
    }
}
