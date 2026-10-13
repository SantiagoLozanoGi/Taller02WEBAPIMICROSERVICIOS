package edu.unibague.auth.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import edu.unibague.auth.model.Credential;
import edu.unibague.auth.repository.CredentialRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CredentialRepository repository;

    public CustomUserDetailsService(CredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Credential credential = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(credential.getUsername())
                .password(credential.getPasswordHash())
                .roles(credential.getRole().name())
                .build();
    }
}
