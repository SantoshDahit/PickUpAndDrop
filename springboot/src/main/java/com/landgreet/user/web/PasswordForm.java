package com.landgreet.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordForm {

    @NotBlank(message = "Please enter your current password.")
    private String currentPassword;

    @NotBlank(message = "Please choose a new password.")
    @Size(min = 6, max = 200, message = "New password needs at least 6 characters.")
    private String newPassword;

    @NotBlank(message = "Please repeat the new password.")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
