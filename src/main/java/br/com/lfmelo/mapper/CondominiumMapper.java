package br.com.lfmelo.mapper;

import br.com.lfmelo.dto.condominium.CondominiumDto;
import br.com.lfmelo.entities.Condominium;

/** Pure, stateless conversion between {@link Condominium} and {@link CondominiumDto}. */
public final class CondominiumMapper {

    private CondominiumMapper() {
    }

    public static CondominiumDto toDto(Condominium condominium) {
        return new CondominiumDto(condominium.getId(), condominium.getName(), condominium.getCreatedAt());
    }
}
