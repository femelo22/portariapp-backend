package br.com.lfmelo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {

    List<StaffMember> findByCondominiumId(UUID condominiumId);

    Optional<StaffMember> findByIdAndCondominiumId(UUID id, UUID condominiumId);

    Optional<StaffMember> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndIdNot(UUID userId, UUID id);
}
