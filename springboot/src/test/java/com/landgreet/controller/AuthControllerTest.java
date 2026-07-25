package com.landgreet.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.landgreet.support.IntegrationTestBase;
import com.landgreet.support.TestAuthHelper;
import com.landgreet.support.TestDataHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class AuthControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Test
    void signupReturnsTokenAndNormalizesEmail() throws Exception {
        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Minji Kim","email":"Minji@Example.com","password":"secret1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("minji@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void duplicateEmailIsRejectedWithErrorCode() throws Exception {
        var user = dataHelper.createUser();
        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dup","email":"%s","password":"secret1"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USR_BR_001"));
    }

    @Test
    void loginFailureIsGenericAndUnauthorized() throws Exception {
        var user = dataHelper.createUser();
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("USR_UA_001"));
    }

    @Test
    void protectedEndpointNeedsToken() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("CMN_UA_001"));

        var user = dataHelper.createUser();
        mockMvc.perform(get("/v1/users/me").header("Authorization", authHelper.bearerFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }
}
