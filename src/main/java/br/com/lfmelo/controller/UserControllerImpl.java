package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.user.CreateUserRequest;
import br.com.lfmelo.dto.user.UpdateUserRequest;
import br.com.lfmelo.dto.user.UserDto;
import br.com.lfmelo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserControllerImpl implements UserController {

    private final UserService userService;

    public UserControllerImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<List<UserDto>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Override
    public ResponseEntity<UserDto> findById(UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Override
    public ResponseEntity<UserDto> create(CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Override
    public ResponseEntity<UserDto> update(UUID id, UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
