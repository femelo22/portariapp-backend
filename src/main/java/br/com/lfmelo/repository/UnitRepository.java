package br.com.lfmelo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

    List<Unit> findByCondominiumId(UUID condominiumId);

    Optional<Unit> findByIdAndCondominiumId(UUID id, UUID condominiumId);

    boolean existsByCondominiumIdAndIdentifierIgnoreCase(UUID condominiumId, String identifier);

    boolean existsByCondominiumIdAndIdentifierIgnoreCaseAndIdNot(
            UUID condominiumId, String identifier, UUID id);
}
