package br.com.lfmelo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.staffmember.CreateStaffMemberRequest;
import br.com.lfmelo.dto.staffmember.StaffMemberDto;
import br.com.lfmelo.dto.staffmember.UpdateStaffMemberRequest;
import br.com.lfmelo.enums.StaffRole;
import br.com.lfmelo.exception.GlobalExceptionHandler;
import br.com.lfmelo.service.StaffMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StaffMemberRoleControllerTest {

    private final UUID condominiumId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    @Mock private StaffMemberService service;
    private MockMvc mvc;
    private String base;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new StaffMemberControllerImpl(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        base = "/api/v1/condominiums/" + condominiumId + "/staff-members";
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldCreateStaffWithRole(StaffRole role) throws Exception {
        when(service.create(eq(condominiumId), any())).thenReturn(dto(role));

        mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(",\"role\":\"" + role + "\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.id").value(staffId.toString()));

        ArgumentCaptor<CreateStaffMemberRequest> request = ArgumentCaptor.forClass(CreateStaffMemberRequest.class);
        verify(service).create(eq(condominiumId), request.capture());
        assertThat(request.getValue().role()).isEqualTo(role);
        assertThat(request.getValue().userId()).isEqualTo(userId);
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldUpdateStaffWithRole(StaffRole role) throws Exception {
        when(service.update(eq(condominiumId), eq(staffId), any())).thenReturn(dto(role));

        mvc.perform(put(base + "/" + staffId).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(",\"role\":\"" + role + "\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role.name()));

        ArgumentCaptor<UpdateStaffMemberRequest> request = ArgumentCaptor.forClass(UpdateStaffMemberRequest.class);
        verify(service).update(eq(condominiumId), eq(staffId), request.capture());
        assertThat(request.getValue().role()).isEqualTo(role);
        assertThat(request.getValue().active()).isFalse();
    }

    @Test
    void shouldReturnRolesInListAndSingleLookup() throws Exception {
        when(service.findAll(condominiumId)).thenReturn(List.of(dto(StaffRole.DOORMAN), dto(StaffRole.ADMIN)));
        when(service.findById(condominiumId, staffId)).thenReturn(dto(StaffRole.ADMIN));

        mvc.perform(get(base))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("DOORMAN"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));
        mvc.perform(get(base + "/" + staffId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"role\":null"})
    void shouldRejectMissingOrNullRoleOnCreate(String roleField) throws Exception {
        mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content(createBody(roleField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.role").exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"role\":null"})
    void shouldRejectMissingOrNullRoleOnUpdate(String roleField) throws Exception {
        mvc.perform(put(base + "/" + staffId).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(roleField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.role").exists());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectInvalidRoleOnCreate() throws Exception {
        mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(",\"role\":\"INVALID\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or missing request body"));
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectInvalidRoleOnUpdate() throws Exception {
        mvc.perform(put(base + "/" + staffId).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(",\"role\":\"INVALID\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or missing request body"));
        verifyNoInteractions(service);
    }

    private String createBody(String roleField) {
        return "{\"condominiumId\":\"" + condominiumId + "\",\"userId\":\"" + userId
                + "\",\"fullName\":\"Maria\"" + roleField + "}";
    }

    private String updateBody(String roleField) {
        return "{\"fullName\":\"Maria\",\"active\":false" + roleField + "}";
    }

    private StaffMemberDto dto(StaffRole role) {
        return new StaffMemberDto(staffId, condominiumId, userId, "Maria", true,
                Instant.parse("2026-01-02T03:04:05Z"), role);
    }
}
