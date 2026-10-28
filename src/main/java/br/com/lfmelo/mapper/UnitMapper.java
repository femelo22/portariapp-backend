package br.com.lfmelo.mapper;

import br.com.lfmelo.dto.unit.UnitDto;
import br.com.lfmelo.entities.Unit;

/** Pure, stateless conversion between {@link Unit} and {@link UnitDto}. */
public final class UnitMapper {

    private UnitMapper() {
    }

    public static UnitDto toDto(Unit unit) {
        return new UnitDto(
                unit.getId(), unit.getCondominium().getId(), unit.getIdentifier(), unit.getCreatedAt());
    }
}
