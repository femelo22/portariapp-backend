package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.CreateUserRequest;
import br.com.lfmelo.dto.UpdateUserRequest;
import br.com.lfmelo.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** CRUD contract for the {@code /api/v1/users} resource. */
@RequestMapping("/api/v1/users")
public interface UserController {

    @GetMapping
    ResponseEntity<List<UserDto>> findAll();

    @GetMapping("/{id}")
    ResponseEntity<UserDto> findById(@PathVariable UUID id);

    @PostMapping
    ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request);

    @PutMapping("/{id}")
    ResponseEntity<UserDto> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
