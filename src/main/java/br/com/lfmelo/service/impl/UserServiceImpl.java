package br.com.lfmelo.service.impl;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.user.CreateUserRequest;
import br.com.lfmelo.dto.user.UpdateUserRequest;
import br.com.lfmelo.dto.user.UserDto;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.exception.ConflictException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.mapper.UserMapper;
import br.com.lfmelo.repository.UserRepository;
import br.com.lfmelo.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(UserMapper::toDto).toList();
    }

    @Override
    public UserDto findById(UUID id) {
        return UserMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto update(UUID id, UpdateUserRequest request) {
        User user = getOrThrow(id);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        user.setEmail(request.email());
        user.setActive(request.active());
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = getOrThrow(id);
        userRepository.delete(user);
    }

    @Override
    public User getOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
