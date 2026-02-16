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

import com.rami.weeklymealplanner.kroger.application.FindKrogerProductsService;

@WebMvcTest(KrogerProductsController.class)
class KrogerProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindKrogerProductsService productsService;

    @Test
    void getProductsReturnsBadRequestWhenTermIsBlank() throws Exception {
        mockMvc.perform(
                        get("/api/v1/kroger/products")
                                .param("term", " ")
                                .param("locationId", "01400433")
                                .param("limit", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("term is required"));
    }

    @Test
    void getProductsReturnsBadRequestWhenLocationIdIsBlank() throws Exception {
        mockMvc.perform(
                        get("/api/v1/kroger/products")
                                .param("term", "milk")
                                .param("locationId", " ")
                                .param("limit", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("locationId is required"));
    }

    @Test
    void getProductsReturnsBadRequestWhenLimitIsInvalid() throws Exception {
        mockMvc.perform(
                        get("/api/v1/kroger/products")
                                .param("term", "milk")
                                .param("locationId", "01400433")
                                .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("limit must be > 0"));
    }

    @Test
    void getProductsReturnsOkWhenServiceSucceeds() throws Exception {
        List<ProductSummaryResponse> results = List.of(
                new ProductSummaryResponse(
                        "0001111041729",
                        "Kroger 2% Milk",
                        "Kroger",
                        "0001111041729",
                        "1 gal",
                        3.99,
                        2.99,
                        3.99,
                        2.99,
                        4.29,
                        3.79,
                        4.29,
                        3.79,
                        "UNIT",
                        "REFRIGERATED"
                )
        );

        when(productsService.find("milk", "01400433", "Kroger", 5)).thenReturn(results);

        mockMvc.perform(
                        get("/api/v1/kroger/products")
                                .param("term", "milk")
                                .param("locationId", "01400433")
                                .param("brand", "Kroger")
                                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("0001111041729"))
                .andExpect(jsonPath("$[0].brand").value("Kroger"))
                .andExpect(jsonPath("$[0].regularPrice").value(3.99));
    }

    @Test
    void getProductsReturnsInternalServerErrorWhenServiceThrowsIOException() throws Exception {
        when(productsService.find("milk", "01400433", null, 5))
                .thenThrow(new IOException("Products call failed: HTTP 401 Unauthorized"));

        mockMvc.perform(
                        get("/api/v1/kroger/products")
                                .param("term", "milk")
                                .param("locationId", "01400433")
                                .param("limit", "5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to fetch products"))
                .andExpect(jsonPath("$.message", containsString("HTTP 401")));
    }
}
