package br.com.lfmelo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, UUID> {

    List<Resident> findByCondominiumId(UUID condominiumId);

    Optional<Resident> findByIdAndCondominiumId(UUID id, UUID condominiumId);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndIdNot(UUID userId, UUID id);
}
