package com.landgreet.entity;

import com.landgreet.entity.base.BaseFullTimeEntity;
import com.landgreet.enums.DriverStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "driver")
@Getter
@NoArgsConstructor
public class Driver extends BaseFullTimeEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "license_no")
    private String licenseNo;

    @Column(name = "owns_vehicle", nullable = false)
    private boolean ownsVehicle;

    @Column(name = "vehicle")
    private String vehicle;    // model, e.g. 'Hyundai Staria'

    @Column(name = "plate_no")
    private String plateNo;    // what travellers look for at arrivals

    @Column(name = "seats", nullable = false)
    private int seats;         // passenger seats, not counting the driver

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DriverStatus status;

    public Driver(String name, String phone, String licenseNo, boolean ownsVehicle,
                  String vehicle, String plateNo, int seats) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.phone = phone;
        this.licenseNo = licenseNo;
        this.ownsVehicle = ownsVehicle;
        this.vehicle = vehicle;
        this.plateNo = plateNo;
        this.seats = seats;
        this.status = DriverStatus.ACTIVE;
    }

    public void update(String name, String phone, String licenseNo, Boolean ownsVehicle,
                       String vehicle, String plateNo, Integer seats) {
        if (name != null && !name.isBlank()) this.name = name;
        if (phone != null) this.phone = phone.isBlank() ? null : phone;
        if (licenseNo != null) this.licenseNo = licenseNo.isBlank() ? null : licenseNo;
        if (ownsVehicle != null) this.ownsVehicle = ownsVehicle;
        if (vehicle != null) this.vehicle = vehicle.isBlank() ? null : vehicle;
        if (plateNo != null) this.plateNo = plateNo.isBlank() ? null : plateNo;
        if (seats != null) this.seats = seats;
    }

    public void updateStatus(DriverStatus status) {
        this.status = status;
    }

    public boolean isAssignable() {
        return !isDeleted() && status == DriverStatus.ACTIVE;
    }
}
