package edu.unibague.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.unibague.auth.config.RsaKeyConfig;
import edu.unibague.auth.config.SecurityConfig;
import edu.unibague.auth.dto.AuthResponse;
import edu.unibague.auth.dto.LoginRequest;
import edu.unibague.auth.dto.RegisterRequest;
import edu.unibague.auth.dto.RegisterResponse;
import edu.unibague.auth.exception.ConflictException;
import edu.unibague.auth.security.CustomUserDetailsService;
import edu.unibague.auth.service.AuthService;
import edu.unibague.auth.support.TestRsaKeys;

/**
 * Prueba la API de autenticación a nivel HTTP con la configuración real de
 * seguridad: códigos 201/200 para casos correctos y 400/401/409 para los
 * casos negativos del contrato.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RsaKeyConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    /**
     * El slice @WebMvcTest de Boot 4 no importa la autoconfiguración de
     * seguridad (ServletWebSecurityAutoConfiguration), por lo que la
     * infraestructura de @EnableWebSecurity (HttpSecurity,
     * AuthenticationConfiguration, WebSecurityConfiguration) no existe y el
     * SecurityConfig real no puede construirse. Activándola aquí se prueba la
     * cadena de filtros de seguridad completa y auténtica.
     */
    @TestConfiguration
    @EnableWebSecurity
    static class WebSecurityTestConfiguration {
    }

    @DynamicPropertySource
    static void rsaKeys(DynamicPropertyRegistry registry) throws Exception {
        // Las llaves de producción (classpath:keys/*.pem) no se versionan;
        // para la prueba se genera un par en memoria y se expone como archivos.
        Path dir = Files.createTempDirectory("taller02-auth-test-keys");
        Path publicPem = dir.resolve("public.pem");
        Path privatePem = dir.resolve("private.pem");
        Files.writeString(publicPem, TestRsaKeys.publicPem());
        Files.writeString(privatePem, TestRsaKeys.privatePem());
        registry.add("app.jwt.public-key", () -> publicPem.toUri().toString());
        registry.add("app.jwt.private-key", () -> privatePem.toUri().toString());
    }

    @Test
    void registerReturns201WithTheCreatedCredential() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(
                new RegisterResponse(UUID.randomUUID(), "carlos.demo", "carlos.demo@example.com", "USER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "carlos.demo",
                                  "email": "carlos.demo@example.com",
                                  "password": "Demo12345"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("carlos.demo"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    void registerWithInvalidPayloadReturns400AndFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "no-es-un-correo",
                                  "password": "corto1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void registerWithDuplicatedUsernameReturns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("El nombre de usuario ya está registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "carlos.demo",
                                  "email": "carlos.demo@example.com",
                                  "password": "Demo12345"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El nombre de usuario ya está registrado"));
    }

    @Test
    void loginReturns200WithAccessToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse(
                "Bearer",
                "eyJhbGciOiJSUzI1NiJ9.token-de-prueba",
                Instant.now().plusSeconds(3600),
                UUID.randomUUID(),
                "student",
                "USER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student",
                                  "password": "Student123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("student"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student",
                                  "password": "ContraseniaIncorrecta1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void loginWithBlankFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }
}
