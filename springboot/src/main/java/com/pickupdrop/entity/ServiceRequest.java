package com.pickupdrop.entity;

import com.pickupdrop.entity.base.BaseFullTimeEntity;
import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
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

/**
 * A traveller's request for a non-ride service (plan 013). One row shape for
 * every {@link ServiceType}: the type-specific columns are nullable because
 * each facility needs a different subset.
 */
@Entity
@Table(name = "service_request")
@Getter
@NoArgsConstructor
public class ServiceRequest extends BaseFullTimeEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ServiceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ServiceRequestStatus status;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "airport")
    private String airport;

    /** The option the traveller picked — for a SIM, the data/duration plan. */
    @Column(name = "detail")
    private String detail;

    @Column(name = "deliver_to")
    private String deliverTo;

    @Column(name = "contact")
    private String contact;

    @Column(name = "notes", length = 1000)
    private String notes;

    /** Operator-only; never returned by a customer route. */
    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    public ServiceRequest(User user, ServiceType type, LocalDate arrivalDate, String airport,
                          String detail, String deliverTo, String contact, String notes) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.type = type;
        this.status = ServiceRequestStatus.REQUESTED;
        this.arrivalDate = arrivalDate;
        this.airport = airport;
        this.detail = detail;
        this.deliverTo = deliverTo;
        this.contact = contact;
        this.notes = notes;
    }

    /** @return false when the move is not legal from the current status. */
    public boolean updateStatus(ServiceRequestStatus next) {
        if (!status.canMoveTo(next)) {
            return false;
        }
        this.status = next;
        return true;
    }

    public void updateAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public boolean isOpen() {
        return !status.isTerminal();
    }
}
