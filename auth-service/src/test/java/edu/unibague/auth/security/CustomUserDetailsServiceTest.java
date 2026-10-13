package edu.unibague.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import edu.unibague.auth.model.Credential;
import edu.unibague.auth.model.Role;
import edu.unibague.auth.repository.CredentialRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private CredentialRepository repository;

    @Test
    void loadsUserWithItsRoleAndPasswordHash() {
        Credential credential = new Credential();
        credential.setId(UUID.randomUUID());
        credential.setUsername("student");
        credential.setEmail("student@taller.local");
        credential.setPasswordHash("$2a$10$hash-de-prueba");
        credential.setRole(Role.USER);
        when(repository.findByUsername("student")).thenReturn(Optional.of(credential));

        CustomUserDetailsService service = new CustomUserDetailsService(repository);
        UserDetails user = service.loadUserByUsername("student");

        assertThat(user.getUsername()).isEqualTo("student");
        assertThat(user.getPassword()).isEqualTo("$2a$10$hash-de-prueba");
        assertThat(user.getAuthorities()).singleElement().isEqualTo(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void unknownUsernameThrowsUsernameNotFound() {
        when(repository.findByUsername("desconocido")).thenReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(repository);

        assertThatThrownBy(() -> service.loadUserByUsername("desconocido"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
