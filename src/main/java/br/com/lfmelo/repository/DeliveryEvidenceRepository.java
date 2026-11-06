package br.com.lfmelo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.DeliveryEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryEvidenceRepository extends JpaRepository<DeliveryEvidence, UUID> {
    List<DeliveryEvidence> findByCondominiumIdAndDeliveryIdOrderByCreatedAtDesc(
            UUID condominiumId, UUID deliveryId);

    Optional<DeliveryEvidence> findByIdAndCondominiumIdAndDeliveryId(
            UUID id, UUID condominiumId, UUID deliveryId);
}
