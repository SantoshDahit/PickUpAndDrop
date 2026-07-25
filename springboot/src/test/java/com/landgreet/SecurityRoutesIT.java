package com.landgreet;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.landgreet.user.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRoutesIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void ensureRegularUser() {
        try {
            userService.register("Route Tester", "routes@example.com", null, "secret1");
        } catch (com.landgreet.user.DuplicateEmailException ignored) {
            // already created by an earlier test in this class
        }
    }

    /** Spring Session stores auth server-side; the SESSION cookie is the handle. */
    private Cookie loginAs(String email, String password) throws Exception {
        Cookie cookie = mvc.perform(formLogin().user(email).password(password))
                .andExpect(authenticated())
                .andReturn().getResponse().getCookie("SESSION");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    void publicRoutesAreOpen() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/signup")).andExpect(status().isOk());
    }

    @Test
    void accountRequiresLogin() throws Exception {
        mvc.perform(get("/account")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void adminAreaForbiddenForRegularUser() throws Exception {
        var cookie = loginAs("routes@example.com", "secret1");
        mvc.perform(get("/admin/users").cookie(cookie)).andExpect(status().isForbidden());
    }

    @Test
    void adminAreaOpenForSeededAdmin() throws Exception {
        var cookie = loginAs("admin@landgreet.com", "admin123");
        mvc.perform(get("/admin/users").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void loginFailureStaysGeneric() throws Exception {
        mvc.perform(formLogin().user("routes@example.com").password("nope"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void stateChangingPostWithoutCsrfIsRejected() throws Exception {
        var cookie = loginAs("routes@example.com", "secret1");
        mvc.perform(post("/account/profile").cookie(cookie)
                        .param("name", "X").param("email", "routes@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void avatarKeysAreStrictlyValidated() throws Exception {
        // Encoded traversal is stopped by Spring's firewall (400) — any 4xx
        // is fine as long as it is never file contents.
        mvc.perform(get("/avatars/{f}", "..%2Fapp-spring.db")).andExpect(status().is4xxClientError());
        mvc.perform(get("/avatars/evil.jpg")).andExpect(status().isNotFound());
        mvc.perform(get("/avatars/00000000-0000-0000-0000-000000000000.jpg"))
                .andExpect(status().isNotFound());
    }
}
