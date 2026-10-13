package edu.unibague.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unibague.users.dto.CreateUserRequest;
import edu.unibague.users.dto.UpdateUserRequest;
import edu.unibague.users.dto.UserResponse;
import edu.unibague.users.exception.ConflictException;
import edu.unibague.users.exception.ResourceNotFoundException;
import edu.unibague.users.model.UserProfile;
import edu.unibague.users.repository.UserProfileRepository;

/**
 * Prueba la lógica del CRUD de perfiles: identidad desde el token,
 * duplicados (409), inexistencia (404), actualización y borrado.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    private UserProfileService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UserProfileService(repository);
    }

    @Test
    void createUsesIdentityFromTokenNotFromBody() {
        when(repository.existsById(userId)).thenReturn(false);
        when(repository.existsByUsername("student")).thenReturn(false);
        when(repository.existsByEmail("student@taller.local")).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.create(userId, "student", "student@taller.local",
                new CreateUserRequest("Estudiante", "Demo", "+57 300 111 2233"));

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("student");
        assertThat(response.email()).isEqualTo("student@taller.local");
        assertThat(response.firstName()).isEqualTo("Estudiante");
    }

    @Test
    void createRejectsDuplicatedProfileForTheSameUser() {
        when(repository.existsById(userId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(userId, "student", "student@taller.local",
                new CreateUserRequest("Estudiante", "Demo", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya existe");
    }

    @Test
    void createRejectsUsernameOrEmailAlreadyUsed() {
        when(repository.existsById(userId)).thenReturn(false);
        when(repository.existsByUsername("student")).thenReturn(true);

        assertThatThrownBy(() -> service.create(userId, "student", "student@taller.local",
                new CreateUserRequest("Estudiante", "Demo", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya pertenece a otro perfil");
    }

    @Test
    void findByIdThrows404WhenProfileDoesNotExist() {
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void findAllMapsEveryProfileToItsDto() {
        when(repository.findAll()).thenReturn(List.of(storedProfile()));

        List<UserResponse> profiles = service.findAll();

        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).id()).isEqualTo(userId);
        assertThat(profiles.get(0).username()).isEqualTo("student");
    }

    @Test
    void updateChangesNamesPhoneAndTimestamp() {
        UserProfile stored = storedProfile();
        when(repository.findById(userId)).thenReturn(Optional.of(stored));
        when(repository.save(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.update(userId,
                new UpdateUserRequest("Nuevo", "Apellido", "3001234567"));

        assertThat(response.firstName()).isEqualTo("Nuevo");
        assertThat(response.lastName()).isEqualTo("Apellido");
        assertThat(response.phone()).isEqualTo("3001234567");
        assertThat(response.updatedAt()).isAfterOrEqualTo(response.createdAt());
    }

    @Test
    void deleteRemovesTheProfileWhenItExists() {
        UserProfile stored = storedProfile();
        when(repository.findById(userId)).thenReturn(Optional.of(stored));

        service.delete(userId);

        org.mockito.Mockito.verify(repository).delete(stored);
    }

    private UserProfile storedProfile() {
        UserProfile profile = new UserProfile();
        profile.setId(userId);
        profile.setUsername("student");
        profile.setEmail("student@taller.local");
        profile.setFirstName("Estudiante");
        profile.setLastName("Demo");
        profile.setPhone("+57 300 111 2233");
        return profile;
    }
}
