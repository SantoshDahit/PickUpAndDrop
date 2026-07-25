package com.landgreet.entity;

import com.landgreet.entity.base.BaseFullTimeEntity;
import com.landgreet.enums.Role;
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
@Table(name = "user")
@Getter
@NoArgsConstructor
public class User extends BaseFullTimeEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "char(36)")
    private String id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    public User(String email, String password, String name, String phone, Role role) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
    }

    public void updatePassword(String password) {
        if (password == null || password.isBlank()) {
            return;
        }
        this.password = password;
    }

    public void update(String name, String phone) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone.isBlank() ? null : phone;
        }
    }

    /** Frees the email slot for re-registration while keeping UNIQUE honest. */
    public void releaseEmailOnDelete() {
        this.email = "deleted:" + this.id + ":" + this.email;
    }
}
