package edu.unibague.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unibague.auth.dto.AuthResponse;
import edu.unibague.auth.dto.LoginRequest;
import edu.unibague.auth.dto.RegisterRequest;
import edu.unibague.auth.dto.RegisterResponse;
import edu.unibague.auth.exception.ConflictException;
import edu.unibague.auth.model.Credential;
import edu.unibague.auth.model.Role;
import edu.unibague.auth.repository.CredentialRepository;

@Service
public class AuthService {

    private final CredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(CredentialRepository repository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new ConflictException("El nombre de usuario ya está registrado");
        }
        if (repository.existsByEmail(request.email())) {
            throw new ConflictException("El correo electrónico ya está registrado");
        }

        Credential credential = new Credential();
        credential.setUsername(request.username());
        credential.setEmail(request.email());
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credential.setRole(Role.USER);

        Credential saved = repository.save(credential);
        return new RegisterResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        Credential credential = repository.findByUsername(request.username())
                .orElseThrow();

        JwtService.TokenResult result = jwtService.generateToken(credential);
        return new AuthResponse(
                "Bearer",
                result.token(),
                result.expiresAt(),
                credential.getId(),
                credential.getUsername(),
                credential.getRole().name());
    }
}
