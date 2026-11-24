package com.BuildMyPC.msvc_auth_service.Services;

import com.BuildMyPC.msvc_auth_service.Models.Auth;
import com.BuildMyPC.msvc_auth_service.Repositories.AuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.bootstrap.auth-enabled", havingValue = "true")
public class AuthBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthBootstrap.class);

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${BOOTSTRAP_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${BOOTSTRAP_TECH_EMAIL:}")
    private String techEmail;

    @Value("${BOOTSTRAP_TECH_PASSWORD:}")
    private String techPassword;

    public AuthBootstrap(AuthRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearSiNoExiste(adminEmail, adminPassword, "ADMINISTRADOR");
        crearSiNoExiste(techEmail, techPassword, "TECNICO");
    }

    private void crearSiNoExiste(String email, String password, String rol) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No se inicializó la cuenta bootstrap {} porque falta su configuración.", rol);
            return;
        }

        if (repository.findByEmail(email).isPresent()) {
            return;
        }

        Auth auth = new Auth();
        auth.setEmail(email.trim());
        auth.setPasswordHash(passwordEncoder.encode(password));
        auth.setRol(rol);
        auth.setEstado("ACTIVO");
        repository.save(auth);
        log.info("Se creó la cuenta bootstrap para el rol {}.", rol);
    }
}
