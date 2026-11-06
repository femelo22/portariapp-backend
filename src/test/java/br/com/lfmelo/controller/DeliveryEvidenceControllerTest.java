package br.com.lfmelo.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceContent;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceDto;
import br.com.lfmelo.enums.EvidenceContentType;
import br.com.lfmelo.service.DeliveryEvidenceService;
import br.com.lfmelo.storage.EvidenceSizeExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DeliveryEvidenceControllerTest {
    private final DeliveryEvidenceService service = mock(DeliveryEvidenceService.class);
    private MockMvc mvc;
    private final UUID condominiumId = UUID.randomUUID();
    private final UUID deliveryId = UUID.randomUUID();
    private final UUID evidenceId = UUID.randomUUID();
    private final String base = "/api/v1/condominiums/" + condominiumId + "/deliveries/" + deliveryId + "/evidences";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DeliveryEvidenceControllerImpl(service))
                .setControllerAdvice(new DeliveryEvidenceExceptionHandler()).build();
    }

    @Test
    void multipartUploadReturnsDtoWithoutStorageKey() throws Exception {
        var dto = new DeliveryEvidenceDto(evidenceId, condominiumId, deliveryId, UUID.randomUUID(), "x.png",
                EvidenceContentType.IMAGE_PNG, 3, null);
        when(service.upload(eq(condominiumId), eq(deliveryId), any())).thenReturn(dto);
        mvc.perform(multipart(base).file(new MockMultipartFile("file", "x.png", "image/png", new byte[] {1, 2, 3})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(evidenceId.toString()))
                .andExpect(jsonPath("$.storageFileKey").doesNotExist());
    }

    @Test
    void missingMultipartPartUsesErrorResponse() throws Exception {
        mvc.perform(multipart(base)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Multipart file is required"));
    }

    @Test
    void oversizedFileReturnsPayloadTooLarge() throws Exception {
        when(service.upload(eq(condominiumId), eq(deliveryId), any()))
                .thenThrow(new EvidenceSizeExceededException());
        mvc.perform(multipart(base).file(new MockMultipartFile("file", "x.png", "image/png", new byte[] {1})))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("Evidence file exceeds the configured limit"));
    }

    @Test
    void listsEvidenceForRequestedDelivery() throws Exception {
        mvc.perform(get(base)).andExpect(status().isOk());
        verify(service).findAll(condominiumId, deliveryId);
    }

    @Test
    void contentUsesSafeAttachmentHeaders() throws Exception {
        when(service.getContent(condominiumId, deliveryId, evidenceId)).thenReturn(
                new DeliveryEvidenceContent(new ByteArrayResource(new byte[] {1, 2}),
                        EvidenceContentType.IMAGE_PNG, 2));
        mvc.perform(get(base + "/" + evidenceId + "/content"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", "attachment"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(new byte[] {1, 2}));
    }
}
