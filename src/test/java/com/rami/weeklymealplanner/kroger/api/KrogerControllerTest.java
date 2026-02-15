package com.rami.weeklymealplanner.kroger.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rami.weeklymealplanner.kroger.application.FindKrogerLocationsService;

@WebMvcTest(KrogerController.class)
class KrogerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindKrogerLocationsService locationsService;

    @Test
    void getLocationsReturnsBadRequestWhenZipIsBlank() throws Exception {
        mockMvc.perform(
                        get("/api/v1/kroger/locations")
                                .param("zip", " ")
                                .param("radius", "10")
                                .param("limit", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("zip is required"));
    }

    @Test
    void getLocationsReturnsBadRequestWhenRadiusOrLimitAreInvalid() throws Exception {
        mockMvc.perform(
                        get("/api/v1/kroger/locations")
                                .param("zip", "85338")
                                .param("radius", "0")
                                .param("limit", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("radius and limit must be > 0"));
    }

    @Test
    void getLocationsReturnsOkWhenServiceSucceeds() throws Exception {
        List<LocationSummaryResponse> results = List.of(
                new LocationSummaryResponse(
                        "01400433",
                        "Kroger Marketplace",
                        "Kroger",
                        "Goodyear",
                        "AZ",
                        "85338",
                        "623-555-1212",
                        "America/Phoenix",
                        "08:00-20:00",
                        true,
                        true
                )
        );

        when(locationsService.find("85338", 10, 5)).thenReturn(results);

        mockMvc.perform(
                        get("/api/v1/kroger/locations")
                                .param("zip", "85338")
                                .param("radius", "10")
                                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationId").value("01400433"))
                .andExpect(jsonPath("$[0].pickup").value(true))
                .andExpect(jsonPath("$[0].delivery").value(true));
    }

    @Test
    void getLocationsReturnsInternalServerErrorWhenServiceThrowsIOException() throws Exception {
        when(locationsService.find("85338", 10, 5))
                .thenThrow(new IOException("Token call failed: HTTP 401 Unauthorized"));

        mockMvc.perform(
                        get("/api/v1/kroger/locations")
                                .param("zip", "85338")
                                .param("radius", "10")
                                .param("limit", "5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to fetch locations"))
                .andExpect(jsonPath("$.message", containsString("HTTP 401")));
    }
}
