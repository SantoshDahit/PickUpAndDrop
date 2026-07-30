package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickupdrop.entity.User;
import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestAuthHelper;
import com.pickupdrop.support.TestDataHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class ServiceRequestControllerTest extends IntegrationTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private String simPayload(LocalDate arrival) {
        return """
               {"type":"SIM_CARD","arrivalDate":"%s","airport":"ICN",
                "detail":"30 days / 10GB","deliverTo":"Arrivals gate 5",
                "contact":"+977 98...","notes":"Landing 06:00"}
               """.formatted(arrival);
    }

    private String createFor(User user) throws Exception {
        String json = mockMvc.perform(post("/v1/service-requests")
                        .header("Authorization", authHelper.bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simPayload(LocalDate.now().plusDays(20))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(json).get("id").asText();
    }

    @Test
    void travellerRequestsASimAndSeesItAsRequested() throws Exception {
        User traveller = dataHelper.createUser();

        mockMvc.perform(post("/v1/service-requests")
                        .header("Authorization", authHelper.bearerFor(traveller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simPayload(LocalDate.now().plusDays(20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SIM_CARD"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.detail").value("30 days / 10GB"));

        mockMvc.perform(get("/v1/service-requests/me")
                        .header("Authorization", authHelper.bearerFor(traveller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].airport").value("ICN"));
    }

    @Test
    void myListShowsOnlyMyOwnRequests() throws Exception {
        User mine = dataHelper.createUser();
        User theirs = dataHelper.createUser();
        createFor(mine);
        createFor(theirs);

        mockMvc.perform(get("/v1/service-requests/me")
                        .header("Authorization", authHelper.bearerFor(mine)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void anotherTravellerCannotCancelMyRequest() throws Exception {
        User owner = dataHelper.createUser();
        String requestId = createFor(owner);

        mockMvc.perform(delete("/v1/service-requests/" + requestId)
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SVC_NF_001"));
    }

    @Test
    void ownerCancelsWhileOpenAndCannotCancelTwice() throws Exception {
        User owner = dataHelper.createUser();
        String requestId = createFor(owner);
        String token = authHelper.bearerFor(owner);

        mockMvc.perform(delete("/v1/service-requests/" + requestId).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/service-requests/me").header("Authorization", token))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));

        mockMvc.perform(delete("/v1/service-requests/" + requestId).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SVC_BR_001"));
    }

    @Test
    void aDeliveredRequestCanNoLongerBeCancelled() throws Exception {
        User owner = dataHelper.createUser();
        String requestId = createFor(owner);
        String adminToken = authHelper.bearerFor(dataHelper.createAdmin());

        mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        mockMvc.perform(delete("/v1/service-requests/" + requestId)
                        .header("Authorization", authHelper.bearerFor(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SVC_BR_001"));
    }

    @Test
    void arrivalDateMustBeSane() throws Exception {
        String token = authHelper.bearerFor(dataHelper.createUser());

        for (LocalDate bad : new LocalDate[]{LocalDate.now().minusDays(1), LocalDate.now().plusDays(400)}) {
            mockMvc.perform(post("/v1/service-requests")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(simPayload(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SVC_BR_002"));
        }
    }

    @Test
    void arrivalDateIsOptionalForTravellersWhoHaveNotBookedFlightsYet() throws Exception {
        mockMvc.perform(post("/v1/service-requests")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SIM_CARD","detail":"30 days / 10GB"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void oversizedNotesAreRejected() throws Exception {
        mockMvc.perform(post("/v1/service-requests")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SIM_CARD\",\"notes\":\"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestingWithoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/service-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simPayload(LocalDate.now().plusDays(20))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theCustomerViewNeverCarriesTheOperatorNote() throws Exception {
        User owner = dataHelper.createUser();
        String requestId = createFor(owner);

        mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                        .header("Authorization", authHelper.bearerFor(dataHelper.createAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"adminNote":"carrier promo expires Friday"}
                                """))
                .andExpect(status().isOk());

        String json = mockMvc.perform(get("/v1/service-requests/me")
                        .header("Authorization", authHelper.bearerFor(owner)))
                .andReturn().getResponse().getContentAsString();
        JsonNode mine = MAPPER.readTree(json).get(0);
        org.assertj.core.api.Assertions.assertThat(mine.has("adminNote")).isFalse();
    }
}
