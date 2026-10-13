package edu.unibague.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.unibague.auth.model.Credential;
import edu.unibague.auth.model.Role;
import edu.unibague.auth.repository.CredentialRepository;

/**
 * Crea los usuarios de demostración del taller unicamente en el entorno
 * academico. Las contraseñas nunca se registran en logs.
 */
@Configuration
public class DemoDataConfig {

    @Bean
    CommandLineRunner demoUsers(CredentialRepository repository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.demo-users.enabled:true}") boolean enabled) {
        return args -> {
            if (!enabled) {
                return;
            }
            createIfMissing(repository, passwordEncoder, "admin", "admin@taller.local", "Admin123!", Role.ADMIN);
            createIfMissing(repository, passwordEncoder, "student", "student@taller.local", "Student123!", Role.USER);
        };
    }

    private void createIfMissing(CredentialRepository repository, PasswordEncoder encoder,
                                 String username, String email, String password, Role role) {
        if (repository.existsByUsername(username)) {
            return;
        }
        Credential credential = new Credential();
        credential.setUsername(username);
        credential.setEmail(email);
        credential.setPasswordHash(encoder.encode(password));
        credential.setRole(role);
        repository.save(credential);
    }
}
