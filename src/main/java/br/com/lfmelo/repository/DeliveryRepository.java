package br.com.lfmelo.repository;

import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.Delivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {

    Optional<Delivery> findByIdAndCondominiumId(UUID id, UUID condominiumId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.id = :id and d.condominium.id = :condominiumId")
    Optional<Delivery> findByIdAndCondominiumIdForUpdate(
            @Param("id") UUID id, @Param("condominiumId") UUID condominiumId);
}
