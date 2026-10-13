package edu.unibague.users.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unibague.users.dto.CreateUserRequest;
import edu.unibague.users.dto.UpdateUserRequest;
import edu.unibague.users.dto.UserResponse;
import edu.unibague.users.exception.ConflictException;
import edu.unibague.users.exception.ResourceNotFoundException;
import edu.unibague.users.model.UserProfile;
import edu.unibague.users.repository.UserProfileRepository;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserResponse create(UUID id, String username, String email, CreateUserRequest request) {
        if (repository.existsById(id)) {
            throw new ConflictException("El perfil del usuario ya existe");
        }
        if (repository.existsByUsername(username) || repository.existsByEmail(email)) {
            throw new ConflictException("El nombre de usuario o correo ya pertenece a otro perfil");
        }

        UserProfile profile = new UserProfile();
        profile.setId(id);
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        return toResponse(repository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        UserProfile profile = findEntity(id);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        profile.setUpdatedAt(Instant.now());
        return toResponse(repository.save(profile));
    }

    @Transactional
    public void delete(UUID id) {
        UserProfile profile = findEntity(id);
        repository.delete(profile);
    }

    private UserProfile findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado: " + id));
    }

    private UserResponse toResponse(UserProfile profile) {
        return new UserResponse(
                profile.getId(),
                profile.getUsername(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
