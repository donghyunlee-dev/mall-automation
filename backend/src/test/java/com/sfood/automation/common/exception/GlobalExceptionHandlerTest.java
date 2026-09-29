package com.sfood.automation.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new GlobalExceptionHandlerTestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()))
            .build();
    }

    @Test
    void should_return_mapped_status_and_message_for_business_exception() throws Exception {
        mockMvc.perform(get("/test/exceptions/business"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("EMAIL_FORMAT_INVALID"))
            .andExpect(jsonPath("$.message").value("Invalid email format"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_return_exception_message_for_business_exception_with_custom_message() throws Exception {
        mockMvc.perform(get("/test/exceptions/business-custom"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_GOODS"))
            .andExpect(jsonPath("$.message").value("ERP goods code already exists: ERP-0002"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_return_internal_server_error_for_unhandled_exception() throws Exception {
        mockMvc.perform(get("/test/exceptions/system"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").value("Internal server error"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    public static class GlobalExceptionHandlerTestController {

        @GetMapping("/test/exceptions/business")
        public void businessException() {
            throw new EmailFormatException();
        }

        @GetMapping("/test/exceptions/system")
        public void systemException() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/test/exceptions/business-custom")
        public void businessCustomException() {
            throw new DuplicateGoodsException("ERP goods code already exists: ERP-0002");
        }
    }
}
