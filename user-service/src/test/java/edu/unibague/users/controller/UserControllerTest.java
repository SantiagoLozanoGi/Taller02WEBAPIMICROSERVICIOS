package edu.unibague.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.unibague.users.config.SecurityConfig;
import edu.unibague.users.dto.CreateUserRequest;
import edu.unibague.users.dto.UserResponse;
import edu.unibague.users.exception.ConflictException;
import edu.unibague.users.exception.ResourceNotFoundException;
import edu.unibague.users.service.UserProfileService;
import edu.unibague.users.support.TestJwtFactory;
import edu.unibague.users.support.TestRsaKeys;

/**
 * Matriz de autorización del contrato del taller con tokens RS256 reales:
 * 401 sin token / token inválido, 403 autenticado sin permiso,
 * y 200/201/204/400/409 para los casos correspondientes.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID OTHER_PROFILE_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    /**
     * Igual que en auth-service: el slice @WebMvcTest de Boot 4 no importa la
     * autoconfiguración de seguridad, y el SecurityConfig real necesita la
     * infraestructura de @EnableWebSecurity (HttpSecurity, filtros) para
     * construirse. Así la validación de firma, roles y propiedad se prueba
     * con la cadena de filtros completa.
     */
    @TestConfiguration
    @EnableWebSecurity
    static class WebSecurityTestConfiguration {
    }

    private String studentToken;
    private String adminToken;

    @DynamicPropertySource
    static void rsaKey(DynamicPropertyRegistry registry) throws Exception {
        // La llave pública de producción (classpath:keys/public.pem) no se
        // versiona; la prueba genera su propio par y expone solo la pública,
        // exactamente como ocurre en el despliegue real.
        Path publicPem = Files.createTempFile("taller02-users-test-public", ".pem");
        Files.writeString(publicPem, TestRsaKeys.publicPem());
        registry.add("app.jwt.public-key", () -> publicPem.toUri().toString());
    }

    @BeforeEach
    void mintTokens() {
        studentToken = TestJwtFactory.validToken(TestRsaKeys.privateKey(), STUDENT_ID.toString(),
                "student", "student@taller.local", List.of("ROLE_USER"));
        adminToken = TestJwtFactory.validToken(TestRsaKeys.privateKey(), ADMIN_ID.toString(),
                "admin", "admin@taller.local", List.of("ROLE_ADMIN"));
    }

    @Test
    void getMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeWithStudentTokenReturnsTheProfileOfTheTokenSubject() throws Exception {
        when(userProfileService.findById(STUDENT_ID)).thenReturn(profile(STUDENT_ID, "student"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.username").value("student"));
    }

    @Test
    void createWithStudentTokenReturns201() throws Exception {
        when(userProfileService.create(eq(STUDENT_ID), eq("student"), eq("student@taller.local"),
                any(CreateUserRequest.class)))
                .thenReturn(profile(STUDENT_ID, "student"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Estudiante",
                                  "lastName": "Demo",
                                  "phone": "+57 300 111 2233"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("student"));
    }

    @Test
    void createDuplicatedProfileReturns409() throws Exception {
        when(userProfileService.create(any(), any(), any(), any()))
                .thenThrow(new ConflictException("El perfil del usuario ya existe"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Estudiante",
                                  "lastName": "Demo"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createWithInvalidPayloadReturns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "Demo",
                                  "phone": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.firstName").exists())
                .andExpect(jsonPath("$.validationErrors.phone").exists());
    }

    @Test
    void listAllProfilesAsUserReturns403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllProfilesAsAdminReturns200() throws Exception {
        when(userProfileService.findAll()).thenReturn(List.of(
                profile(STUDENT_ID, "student"),
                profile(ADMIN_ID, "admin")));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("student"))
                .andExpect(jsonPath("$[1].username").value("admin"));
    }

    @Test
    void readSomeoneElsesProfileAsUserReturns403() throws Exception {
        mockMvc.perform(get("/api/users/{id}", OTHER_PROFILE_ID)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void readAnyProfileAsAdminReturns200() throws Exception {
        when(userProfileService.findById(OTHER_PROFILE_ID)).thenReturn(profile(OTHER_PROFILE_ID, "student"));

        mockMvc.perform(get("/api/users/{id}", OTHER_PROFILE_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OTHER_PROFILE_ID.toString()));
    }

    @Test
    void updateOwnProfileAsUserReturns200() throws Exception {
        when(userProfileService.update(eq(STUDENT_ID), any())).thenReturn(new UserResponse(
                STUDENT_ID, "student", "student@taller.local",
                "Estudiante", "Actualizado", "3001234567",
                Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-02T10:00:00Z")));

        mockMvc.perform(put("/api/users/{id}", STUDENT_ID)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Estudiante",
                                  "lastName": "Actualizado",
                                  "phone": "3001234567"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.lastName").value("Actualizado"))
                .andExpect(jsonPath("$.phone").value("3001234567"));
    }

    @Test
    void updateMissingProfileReturns404() throws Exception {
        when(userProfileService.update(any(), any()))
                .thenThrow(new ResourceNotFoundException("Perfil no encontrado: " + STUDENT_ID));

        mockMvc.perform(put("/api/users/{id}", STUDENT_ID)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Estudiante",
                                  "lastName": "Actualizado"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteAsUserReturns403() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", STUDENT_ID)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAsAdminReturns204() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", OTHER_PROFILE_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void tokenSignedWithAnUnknownKeyReturns401() throws Exception {
        String forgedToken = TestJwtFactory.validToken(TestRsaKeys.untrustedPrivateKey(),
                STUDENT_ID.toString(), "student", "student@taller.local", List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        String expired = TestJwtFactory.token(TestRsaKeys.privateKey(), TestJwtFactory.ISSUER,
                STUDENT_ID.toString(), "student", "student@taller.local", List.of("ROLE_USER"),
                Instant.now().minusSeconds(60));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithForeignIssuerReturns401() throws Exception {
        String foreign = TestJwtFactory.token(TestRsaKeys.privateKey(), "issuer-no-confiable",
                STUDENT_ID.toString(), "student", "student@taller.local", List.of("ROLE_USER"),
                Instant.now().plusSeconds(600));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + foreign))
                .andExpect(status().isUnauthorized());
    }

    private UserResponse profile(UUID id, String username) {
        return new UserResponse(id, username, username + "@taller.local",
                "Estudiante", "Demo", "+57 300 111 2233",
                Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z"));
    }
}
