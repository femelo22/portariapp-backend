package br.com.lfmelo.repository;

import java.util.UUID;
import br.com.lfmelo.entities.Condominium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominiumRepository extends JpaRepository<Condominium, UUID> {
}
