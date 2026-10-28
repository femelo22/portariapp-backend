package br.com.lfmelo.mapper;

import br.com.lfmelo.dto.resident.ResidentDto;
import br.com.lfmelo.entities.Resident;

/** Pure, stateless conversion between {@link Resident} and {@link ResidentDto}. */
public final class ResidentMapper {

    private ResidentMapper() {
    }

    public static ResidentDto toDto(Resident resident) {
        return new ResidentDto(
                resident.getId(),
                resident.getCondominium().getId(),
                resident.getUnit().getId(),
                resident.getUser() != null ? resident.getUser().getId() : null,
                resident.getFullName(),
                resident.getPhone(),
                resident.isActive(),
                resident.getCreatedAt());
    }
}
