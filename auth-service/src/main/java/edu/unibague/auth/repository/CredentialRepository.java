package edu.unibague.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.unibague.auth.model.Credential;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    Optional<Credential> findByUsername(String username);

    Optional<Credential> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
