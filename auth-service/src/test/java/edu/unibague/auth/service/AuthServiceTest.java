package edu.unibague.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import edu.unibague.auth.dto.AuthResponse;
import edu.unibague.auth.dto.LoginRequest;
import edu.unibague.auth.dto.RegisterRequest;
import edu.unibague.auth.dto.RegisterResponse;
import edu.unibague.auth.exception.ConflictException;
import edu.unibague.auth.model.Credential;
import edu.unibague.auth.model.Role;
import edu.unibague.auth.repository.CredentialRepository;
import edu.unibague.auth.support.TestRsaKeys;

/**
 * Prueba la lógica de registro y login: unicidad de username/email,
 * hash BCrypt de la contraseña, rol USER por defecto y emisión de token.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CredentialRepository repository;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(realEncoder(), 60, "taller02-auth");
        service = new AuthService(repository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registerSavesCredentialWithBcryptHashAndUserRole() {
        when(repository.existsByUsername("carlos.demo")).thenReturn(false);
        when(repository.existsByEmail("carlos.demo@example.com")).thenReturn(false);
        when(repository.save(any(Credential.class))).thenAnswer(invocation -> {
            Credential credential = invocation.getArgument(0);
            credential.setId(UUID.randomUUID());
            return credential;
        });

        RegisterResponse response = service.register(
                new RegisterRequest("carlos.demo", "carlos.demo@example.com", "Demo12345"));

        assertThat(response.userId()).isNotNull();
        assertThat(response.username()).isEqualTo("carlos.demo");
        assertThat(response.email()).isEqualTo("carlos.demo@example.com");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void registerRejectsDuplicatedUsername() {
        when(repository.existsByUsername("carlos.demo")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("carlos.demo", "otro@example.com", "Demo12345")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("nombre de usuario");
    }

    @Test
    void registerRejectsDuplicatedEmail() {
        when(repository.existsByUsername("nuevo.usuario")).thenReturn(false);
        when(repository.existsByEmail("carlos.demo@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("nuevo.usuario", "carlos.demo@example.com", "Demo12345")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("correo electrónico");
    }

    @Test
    void registerNeverStoresThePlainTextPassword() {
        when(repository.existsByUsername("carlos.demo")).thenReturn(false);
        when(repository.existsByEmail("carlos.demo@example.com")).thenReturn(false);
        when(repository.save(any(Credential.class))).thenAnswer(invocation -> {
            Credential credential = invocation.getArgument(0);
            credential.setId(UUID.randomUUID());
            return credential;
        });

        service.register(new RegisterRequest("carlos.demo", "carlos.demo@example.com", "Demo12345"));

        org.mockito.ArgumentCaptor<Credential> captor = org.mockito.ArgumentCaptor.forClass(Credential.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();
        assertThat(storedHash).isNotEqualTo("Demo12345");
        assertThat(passwordEncoder.matches("Demo12345", storedHash)).isTrue();
    }

    @Test
    void loginReturnsBearerTokenWithUserInformation() {
        Credential credential = credentialStored();
        when(repository.findByUsername("student")).thenReturn(Optional.of(credential));

        AuthResponse response = service.login(new LoginRequest("student", "Student123!"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(response.userId()).isEqualTo(credential.getId());
        assertThat(response.username()).isEqualTo("student");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void loginDelegatesAuthenticationFailureToSpringSecurity() {
        org.mockito.Mockito.doThrow(new BadCredentialsException("credenciales inválidas"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> service.login(new LoginRequest("student", "contrasenia-equivocada")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private Credential credentialStored() {
        Credential credential = new Credential();
        credential.setId(UUID.randomUUID());
        credential.setUsername("student");
        credential.setEmail("student@taller.local");
        credential.setPasswordHash(passwordEncoder.encode("Student123!"));
        credential.setRole(Role.USER);
        return credential;
    }

    private JwtEncoder realEncoder() {
        RSAKey rsaKey = new RSAKey.Builder(TestRsaKeys.publicKey())
                .privateKey(TestRsaKeys.privateKey())
                .keyID("taller02-key")
                .algorithm(JWSAlgorithm.RS256)
                .build();
        JWKSource<SecurityContext> jwkSource = (selector, context) -> selector.select(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }
}
