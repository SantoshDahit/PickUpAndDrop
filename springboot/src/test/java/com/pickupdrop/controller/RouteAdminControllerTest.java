package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestAuthHelper;
import com.pickupdrop.support.TestDataHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class RouteAdminControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Test
    void adminCrudLifecycle() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());

        var created = mockMvc.perform(post("/v1/admin/routes")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromLocation":"ICN Airport","toLocation":"Busan",
                                 "tiers":[{"groupSize":1,"pricePerPerson":60000},
                                          {"groupSize":4,"pricePerPerson":30000}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.tiers[1].groupSize").value(4))
                .andReturn();
        String id = new ObjectMapper()
                .readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/admin/routes/" + id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toLocation").value("Busan"))
                .andExpect(jsonPath("$.tiers.length()").value(2));

        // Partial update: rename + deactivate + replace the fare ladder. fromLocation kept (null = keep).
        mockMvc.perform(patch("/v1/admin/routes/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toLocation":"Busan Station","active":false,
                                 "tiers":[{"groupSize":2,"pricePerPerson":35000}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromLocation").value("ICN Airport"))
                .andExpect(jsonPath("$.toLocation").value("Busan Station"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.tiers.length()").value(1))
                .andExpect(jsonPath("$.tiers[0].pricePerPerson").value(35000));

        // Deactivated routes disappear from the public listing.
        var publicList = mockMvc.perform(get("/v1/routes"))
                .andExpect(status().isOk())
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(publicList.getResponse().getContentAsString())
                .doesNotContain(id);

        // Unreferenced → hard delete works.
        mockMvc.perform(delete("/v1/admin/routes/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/admin/routes/" + id).header("Authorization", admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RTE_NF_001"));
    }

    @Test
    void duplicateTierSizesAreRefused() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());

        mockMvc.perform(post("/v1/admin/routes")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromLocation":"ICN Airport","toLocation":"Sokcho",
                                 "tiers":[{"groupSize":2,"pricePerPerson":40000},
                                          {"groupSize":2,"pricePerPerson":38000}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RTE_BR_001"));
    }

    @Test
    void deleteIsRefusedWhileRouteIsInUse() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        String routeId = dataHelper.firstRoute().getId();
        dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(14), 2);

        mockMvc.perform(delete("/v1/admin/routes/" + routeId).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RTE_BR_002"));
    }

    @Test
    void regularUsersAreRefused() throws Exception {
        String user = authHelper.bearerFor(dataHelper.createUser());

        mockMvc.perform(get("/v1/admin/routes").header("Authorization", user))
                .andExpect(status().isForbidden());
    }
}
