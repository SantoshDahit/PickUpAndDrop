package com.pickupdrop.dto;

import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ServiceRequestDto {

    public record PostRequest(
            @NotNull ServiceType type,
            LocalDate arrivalDate,
            @Size(max = 60) String airport,
            @Size(max = 120) String detail,
            @Size(max = 255) String deliverTo,
            @Size(max = 60) String contact,
            @Size(max = 1000) String notes
    ) {
    }

    /** Admin status move and/or internal note; both optional, at least one used. */
    public record AdminPatchRequest(
            ServiceRequestStatus status,
            @Size(max = 1000) String adminNote
    ) {
    }

    /** What the traveller sees about their own request — no operator note. */
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private ServiceType type;
        private ServiceRequestStatus status;
        private LocalDate arrivalDate;
        private String airport;
        private String detail;
        private String deliverTo;
        private String contact;
        private String notes;
        private LocalDateTime createdAt;

        public Response(String id, ServiceType type, ServiceRequestStatus status,
                        LocalDate arrivalDate, String airport, String detail, String deliverTo,
                        String contact, String notes, LocalDateTime createdAt) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.arrivalDate = arrivalDate;
            this.airport = airport;
            this.detail = detail;
            this.deliverTo = deliverTo;
            this.contact = contact;
            this.notes = notes;
            this.createdAt = createdAt;
        }
    }

    /**
     * Operator queue row: adds who asked and the internal note. A separate type
     * from {@link Response} so no customer route can return another person's
     * identity (plan 013 §4.3).
     */
    @Getter
    @NoArgsConstructor
    public static class AdminResponse {
        private String id;
        private ServiceType type;
        private ServiceRequestStatus status;
        private LocalDate arrivalDate;
        private String airport;
        private String detail;
        private String deliverTo;
        private String contact;
        private String notes;
        private String adminNote;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AdminResponse(String id, ServiceType type, ServiceRequestStatus status,
                             LocalDate arrivalDate, String airport, String detail,
                             String deliverTo, String contact, String notes, String adminNote,
                             String customerName, String customerEmail, String customerPhone,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.arrivalDate = arrivalDate;
            this.airport = airport;
            this.detail = detail;
            this.deliverTo = deliverTo;
            this.contact = contact;
            this.notes = notes;
            this.adminNote = adminNote;
            this.customerName = customerName;
            this.customerEmail = customerEmail;
            this.customerPhone = customerPhone;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}
