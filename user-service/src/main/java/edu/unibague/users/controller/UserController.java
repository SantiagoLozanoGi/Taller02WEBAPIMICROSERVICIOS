package edu.unibague.users.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.unibague.users.dto.CreateUserRequest;
import edu.unibague.users.dto.UpdateUserRequest;
import edu.unibague.users.dto.UserResponse;
import edu.unibague.users.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * CRUD de perfiles. La identidad SIEMPRE proviene del JWT verificado:
 * ni el username ni el email ni el identificador se aceptan del body.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService service;

    public UserController(UserProfileService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crea el perfil del usuario autenticado")
    public ResponseEntity<UserResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateUserRequest request) {
        UUID id = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("username");
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(id, username, email, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtiene el perfil del usuario autenticado")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.findById(UUID.fromString(jwt.getSubject())));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos los perfiles (solo ADMIN)")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('ADMIN')")
    @Operation(summary = "Consulta un perfil propio o, para ADMIN, cualquier perfil")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('ADMIN')")
    @Operation(summary = "Actualiza un perfil propio o, para ADMIN, cualquier perfil")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un perfil (solo ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
