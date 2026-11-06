package br.com.lfmelo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.Delivery;
import br.com.lfmelo.entities.DeliveryEvidence;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.enums.EvidenceContentType;
import br.com.lfmelo.exception.ForbiddenException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.repository.DeliveryEvidenceRepository;
import br.com.lfmelo.repository.DeliveryRepository;
import br.com.lfmelo.repository.StaffMemberRepository;
import br.com.lfmelo.security.AuthenticatedStaff;
import br.com.lfmelo.security.CurrentStaffResolver;
import br.com.lfmelo.service.impl.DeliveryEvidenceServiceImpl;
import br.com.lfmelo.storage.EvidenceStorage;
import br.com.lfmelo.storage.EvidenceSizeExceededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class DeliveryEvidenceServiceTest {
    private final UUID condominiumId = UUID.randomUUID();
    private final UUID deliveryId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final DeliveryEvidenceRepository evidences = mock(DeliveryEvidenceRepository.class);
    private final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    private final StaffMemberRepository staffMembers = mock(StaffMemberRepository.class);
    private final EvidenceStorage storage = mock(EvidenceStorage.class);
    private final DeliveryEvidenceServiceImpl service =
            new DeliveryEvidenceServiceImpl(evidences, deliveries, new CurrentStaffResolver(staffMembers), storage, 1000);
    private final StaffMember staff = new StaffMember();

    @BeforeEach
    void setUp() {
        var principal = new AuthenticatedStaff(userId, staffId, condominiumId, "staff@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        User user = new User();
        user.setId(userId);
        staff.setId(staffId);
        staff.setUser(user);
        when(staffMembers.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(staff));
        Condominium condominium = new Condominium();
        condominium.setId(condominiumId);
        Delivery delivery = mock(Delivery.class);
        when(delivery.getId()).thenReturn(deliveryId);
        when(delivery.getCondominium()).thenReturn(condominium);
        when(deliveries.findByIdAndCondominiumId(deliveryId, condominiumId)).thenReturn(Optional.of(delivery));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackIncludingCommitFailureCleansStoredFileAndDoesNotExposeKey() {
        TransactionSynchronizationManager.initSynchronization();
        String key = UUID.randomUUID().toString();
        when(storage.store(any(), eq(EvidenceContentType.IMAGE_PNG), eq(1000L)))
                .thenReturn(new EvidenceStorage.StoredEvidence(key, EvidenceContentType.IMAGE_PNG, 1));
        when(evidences.saveAndFlush(any())).thenAnswer(invocation -> {
            DeliveryEvidence evidence = invocation.getArgument(0);
            evidence.setId(UUID.randomUUID());
            return evidence;
        });
        var dto = service.upload(condominiumId, deliveryId,
                new MockMultipartFile("file", "..\\secret.png", "image/png", new byte[] {1}));
        assertEquals("secret.png", dto.originalFilename());
        assertEquals(staffId, dto.uploadedByStaffId());
        assertFalse(dto.toString().contains(key));
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).delete(key);
    }

    @Test
    void committedUploadKeepsFile() {
        TransactionSynchronizationManager.initSynchronization();
        String key = UUID.randomUUID().toString();
        when(storage.store(any(), any(), anyLong()))
                .thenReturn(new EvidenceStorage.StoredEvidence(key, EvidenceContentType.IMAGE_PNG, 1));
        when(evidences.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.upload(condominiumId, deliveryId, new MockMultipartFile("file", "x", "image/png", new byte[] {1}));
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage, never()).delete(anyString());
    }

    @Test
    void commitExceptionWithUnknownOutcomeKeepsFileForReconciliation() {
        String key = UUID.randomUUID().toString();
        when(storage.store(any(), any(), anyLong()))
                .thenReturn(new EvidenceStorage.StoredEvidence(key, EvidenceContentType.IMAGE_PNG, 1));
        when(evidences.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var transactionManager = new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                throw new org.springframework.transaction.TransactionSystemException("simulated commit failure");
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
        assertThrows(org.springframework.transaction.TransactionSystemException.class,
                () -> new TransactionTemplate(transactionManager).execute(status ->
                        service.upload(condominiumId, deliveryId,
                                new MockMultipartFile("file", "x", "image/png", new byte[] {1}))));
        verify(storage, never()).delete(key);
    }

    @Test
    void rejectsInactiveOrReassignedStaffAndInactiveUserBeforeAccessingDelivery() {
        staff.setActive(false);
        var file = new MockMultipartFile("file", "x", "image/png", new byte[] {1});
        assertThrows(ForbiddenException.class, () -> service.upload(condominiumId, deliveryId, file));
        staff.setActive(true);
        staff.getUser().setActive(false);
        assertThrows(ForbiddenException.class, () -> service.upload(condominiumId, deliveryId, file));
        staff.getUser().setActive(true);
        staff.getUser().setId(UUID.randomUUID());
        assertThrows(ForbiddenException.class, () -> service.upload(condominiumId, deliveryId, file));
        verify(deliveries, never()).findByIdAndCondominiumId(any(), any());
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsOversizeUploadBeforeStoring() {
        var file = new MockMultipartFile("file", "x.png", "image/png", new byte[1001]);
        assertThrows(EvidenceSizeExceededException.class, () -> service.upload(condominiumId, deliveryId, file));
        verifyNoInteractions(storage);
    }

    @Test
    void contentLookupIncludesTenantDeliveryAndEvidence() {
        UUID evidenceId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.getContent(condominiumId, deliveryId, evidenceId));
        verify(evidences).findByIdAndCondominiumIdAndDeliveryId(evidenceId, condominiumId, deliveryId);
        verifyNoInteractions(storage);
    }
}
