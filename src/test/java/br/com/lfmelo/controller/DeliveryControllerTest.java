package br.com.lfmelo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import br.com.lfmelo.enums.DeliveryStatus;
import br.com.lfmelo.exception.GlobalExceptionHandler;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    private final UUID condominiumId = UUID.randomUUID();
    private final UUID deliveryId = UUID.randomUUID();
    private final UUID residentId = UUID.randomUUID();
    @Mock private DeliveryService service;
    private MockMvc mvc;
    private String base;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DeliveryControllerImpl(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        base = "/api/v1/condominiums/" + condominiumId + "/deliveries";
    }

    @Test
    void shouldCreateDelivery() throws Exception {
        mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientResidentId\":\"" + residentId + "\"}"))
                .andExpect(status().isCreated());
        verify(service).create(eq(condominiumId), any());
    }

    @Test
    void shouldRejectMissingRecipient() throws Exception {
        mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPassListFiltersToService() throws Exception {
        mvc.perform(get(base).param("status", "RECEIVED").param("recipientResidentId", residentId.toString()))
                .andExpect(status().isOk());
        verify(service).findAll(condominiumId, DeliveryStatus.RECEIVED, residentId);
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        mvc.perform(get(base).param("status", "UNKNOWN")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetDelivery() throws Exception {
        mvc.perform(get(base + "/" + deliveryId)).andExpect(status().isOk());
        verify(service).findById(condominiumId, deliveryId);
    }

    @Test
    void shouldReturnNotFoundForMissingDelivery() throws Exception {
        when(service.findById(condominiumId, deliveryId))
                .thenThrow(new ResourceNotFoundException("Delivery not found"));
        mvc.perform(get(base + "/" + deliveryId)).andExpect(status().isNotFound());
    }

    @Test
    void shouldGetEvents() throws Exception {
        mvc.perform(get(base + "/" + deliveryId + "/events")).andExpect(status().isOk());
        verify(service).findEvents(condominiumId, deliveryId);
    }

    @Test
    void shouldRecordPickup() throws Exception {
        mvc.perform(post(base + "/" + deliveryId + "/pickup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedUpByName\":\"Maria\"}"))
                .andExpect(status().isOk());
        verify(service).pickup(eq(condominiumId), eq(deliveryId), any());
    }

    @Test
    void shouldRejectBlankPickupName() throws Exception {
        mvc.perform(post(base + "/" + deliveryId + "/pickup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedUpByName\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPickupNameOverDatabaseLimit() throws Exception {
        mvc.perform(post(base + "/" + deliveryId + "/pickup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickedUpByName\":\"" + "a".repeat(151) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRecordReturn() throws Exception {
        mvc.perform(post(base + "/" + deliveryId + "/return")).andExpect(status().isOk());
        verify(service).transition(condominiumId, deliveryId, DeliveryStatus.RETURNED, null);
    }

    @Test
    void shouldRecordLoss() throws Exception {
        mvc.perform(post(base + "/" + deliveryId + "/lost")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Missing\"}"))
                .andExpect(status().isOk());
        verify(service).transition(condominiumId, deliveryId, DeliveryStatus.LOST, "Missing");
    }

    @Test
    void shouldRecordCancellation() throws Exception {
        mvc.perform(post(base + "/" + deliveryId + "/cancel")).andExpect(status().isOk());
        verify(service).transition(condominiumId, deliveryId, DeliveryStatus.CANCELLED, null);
    }
}
