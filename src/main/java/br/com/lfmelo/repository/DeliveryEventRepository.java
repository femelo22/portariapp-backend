package br.com.lfmelo.repository;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.entities.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, UUID> {

    List<DeliveryEvent> findByCondominiumIdAndDeliveryIdOrderBySequenceNumberAsc(
            UUID condominiumId, UUID deliveryId);
}
