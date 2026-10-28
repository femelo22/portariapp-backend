package br.com.lfmelo.service;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.user.CreateUserRequest;
import br.com.lfmelo.dto.user.UpdateUserRequest;
import br.com.lfmelo.dto.user.UserDto;
import br.com.lfmelo.entities.User;

public interface UserService {

    List<UserDto> findAll();

    UserDto findById(UUID id);

    UserDto create(CreateUserRequest request);

    UserDto update(UUID id, UpdateUserRequest request);

    void delete(UUID id);

    /**
     * Returns the managed {@link User} entity or throws {@code ResourceNotFoundException}. Reused
     * by services that optionally link a resident/staff member to a user account.
     */
    User getOrThrow(UUID id);
}
