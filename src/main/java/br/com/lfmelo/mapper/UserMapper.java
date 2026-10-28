package br.com.lfmelo.mapper;

import br.com.lfmelo.dto.user.UserDto;
import br.com.lfmelo.entities.User;

/** Pure, stateless conversion between {@link User} and {@link UserDto}. */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.isActive(), user.getCreatedAt());
    }
}
