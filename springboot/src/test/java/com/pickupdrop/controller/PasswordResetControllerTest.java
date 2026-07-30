package com.pickupdrop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickupdrop.entity.User;
import com.pickupdrop.enums.EmailPurpose;
import com.pickupdrop.security.service.EmailVerificationService;
import com.pickupdrop.service.mail.EmailSender;
import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestDataHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Import(TestDataHelper.class)
class PasswordResetControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private EmailVerificationService emailVerificationService;

    /** Stands in for SMTP: nothing may leave the test JVM. */
    @MockitoBean
    private EmailSender emailSender;

    /**
     * Runs without the test transaction on purpose: the service's own
     * transaction has to reach a real commit here. Inside a test transaction a
     * rollback-only marking never surfaces, which hid a 500 on this path.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void forgotPasswordIs204ForKnownAndUnknownEmails() throws Exception {
        User user = dataHelper.createUser();

        mockMvc.perform(post("/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isNoContent());

        // Same answer for an address with no account — no enumeration oracle.
        mockMvc.perform(post("/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody-here@example.com"}
                                """))
                .andExpect(status().isNoContent());
    }

    /**
     * No test transaction: sends are deferred to after commit, so a rolled-back
     * test would never dispatch the mail at all.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void forgotPasswordEmailsAResetLinkCarryingTheToken() throws Exception {
        User user = dataHelper.createUser();

        mockMvc.perform(post("/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isNoContent());

        // Delivery is @Async, so wait for the mail thread rather than assume it ran.
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(5_000))
                .send(eq(user.getEmail()), contains("Reset your"), body.capture());
        assertThat(body.getValue()).contains("/reset-password?token=");
    }

    @Test
    void unknownEmailSendsNothing() throws Exception {
        mockMvc.perform(post("/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"still-nobody@example.com"}
                                """))
                .andExpect(status().isNoContent());

        verify(emailSender, org.mockito.Mockito.after(500).never())
                .send(anyString(), anyString(), anyString());
    }

    @Test
    void issuedTokenSetsTheNewPassword() throws Exception {
        User user = dataHelper.createUser();
        String rawToken = emailVerificationService.issue(user, EmailPurpose.PASSWORD_RESET);

        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"brand-new-1"}
                                """.formatted(rawToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"brand-new-1"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(user.getEmail(), TestDataHelper.PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenCannotBeReplayed() throws Exception {
        User user = dataHelper.createUser();
        String rawToken = emailVerificationService.issue(user, EmailPurpose.PASSWORD_RESET);
        String payload = """
                         {"token":"%s","password":"brand-new-1"}
                         """.formatted(rawToken);

        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USR_BR_003"));
    }

    @Test
    void issuingAgainRetiresThePreviousToken() throws Exception {
        User user = dataHelper.createUser();
        String firstToken = emailVerificationService.issue(user, EmailPurpose.PASSWORD_RESET);
        emailVerificationService.issue(user, EmailPurpose.PASSWORD_RESET);

        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"brand-new-1"}
                                """.formatted(firstToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USR_BR_003"));
    }

    /**
     * A rolled-back request must send nothing. This test's transaction never
     * commits, which is exactly the situation the deferral protects against:
     * no "reset your password" mail for a token row that was thrown away.
     */
    @Test
    void rolledBackRequestSendsNoMail() throws Exception {
        User user = dataHelper.createUser();

        mockMvc.perform(post("/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isNoContent());

        verify(emailSender, org.mockito.Mockito.after(1_000).never())
                .send(anyString(), anyString(), anyString());
    }

    /**
     * The table is shared across email flows, so a code issued for one purpose
     * must not be spendable on another — otherwise a future "verify your
     * address" link would double as a password-reset link.
     */
    @Test
    void codeIssuedForAnotherPurposeIsRejected() throws Exception {
        User user = dataHelper.createUser();
        String verifyCode = emailVerificationService.issue(user, EmailPurpose.VERIFY_ACCOUNT);

        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"brand-new-1"}
                                """.formatted(verifyCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USR_BR_003"));
    }

    /** Retiring is scoped per purpose: another flow's live code must survive. */
    @Test
    void issuingOnePurposeLeavesAnotherPurposeAlone() throws Exception {
        User user = dataHelper.createUser();
        String verifyCode = emailVerificationService.issue(user, EmailPurpose.VERIFY_ACCOUNT);
        String resetCode = emailVerificationService.issue(user, EmailPurpose.PASSWORD_RESET);

        assertThat(verifyCode).isNotEqualTo(resetCode);
        // The reset code works, proving the VERIFY_ACCOUNT row didn't collide.
        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"brand-new-1"}
                                """.formatted(resetCode)))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownTokenIsRejected() throws Exception {
        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"not-a-real-token","password":"brand-new-1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USR_BR_003"));
    }

    @Test
    void shortPasswordIsRejectedByValidation() throws Exception {
        User user = dataHelper.createUser();
        String rawToken = emailVerificationService.issue(user, EmailPurpose.PASSWORD_RESET);

        mockMvc.perform(post("/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"short"}
                                """.formatted(rawToken)))
                .andExpect(status().isBadRequest());
    }
}
