package com.taskflow.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.api.config.RateLimitFilter;
import com.taskflow.api.dto.response.AuthResponse;
import com.taskflow.api.exception.ConflictException;
import com.taskflow.api.security.JwtAuthenticationFilter;
import com.taskflow.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {JwtAuthenticationFilter.class, RateLimitFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void registerReturns201WithTheIssuedTokens() throws Exception {
        when(authService.register(any())).thenReturn(AuthResponse.bearer("access-token", "refresh-token", 900));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                { "email": "jordan@example.com", "password": "password123", "fullName": "Jordan" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerReturnsAProblemDetailWhenTheBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "not-an-email", "password": "short" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void duplicateEmailIsMappedTo409() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("An account with that email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                { "email": "taken@example.com", "password": "password123", "fullName": "Taken" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An account with that email already exists"));
    }
}
