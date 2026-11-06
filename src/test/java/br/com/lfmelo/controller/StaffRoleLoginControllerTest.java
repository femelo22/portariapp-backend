package br.com.lfmelo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.dto.auth.LoginResponse;
import br.com.lfmelo.enums.StaffRole;
import br.com.lfmelo.exception.GlobalExceptionHandler;
import br.com.lfmelo.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StaffRoleLoginControllerTest {

    @Mock private AuthService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthControllerImpl(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldIncludeRoleInLoginResponse(StaffRole role) throws Exception {
        when(service.login(any())).thenReturn(new LoginResponse("token", "Bearer",
                Instant.parse("2026-01-02T03:04:05Z"), UUID.randomUUID(), UUID.randomUUID(),
                "Maria", "staff@example.test", role));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff@example.test\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
