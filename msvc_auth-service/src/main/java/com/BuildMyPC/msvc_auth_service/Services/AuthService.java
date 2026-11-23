package com.BuildMyPC.msvc_auth_service.Services;

import com.BuildMyPC.msvc_auth_service.Exceptions.AuthException;
import com.BuildMyPC.msvc_auth_service.Exceptions.CredencialesInvalidasException;
import com.BuildMyPC.msvc_auth_service.Models.Auth;
import com.BuildMyPC.msvc_auth_service.Models.Dtos.AuthDTO;
import com.BuildMyPC.msvc_auth_service.Repositories.AuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Auth crearAuth(AuthDTO dto) {
        log.info("Iniciando creación de Auth con email: {}", dto.getEmail());

        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            log.error("Fallo al crear: El email {} ya existe", dto.getEmail());
            throw new AuthException("Ya existe una cuenta asociada a este correo.");
        }

        Auth auth = new Auth();
        auth.setEmail(dto.getEmail());
        auth.setPasswordHash(passwordEncoder.encode(dto.getPasswordHash()));
        auth.setRol("CLIENTE");
        auth.setEstado("ACTIVO");

        Auth guardada = repository.save(auth);
        log.info("Auth guardada exitosamente con ID: {}", guardada.getId());
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Auth> listarTodas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Auth buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuthException("Auth no encontrada con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Auth buscarPorEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Auth no encontrada con email: " + email));
    }

    @Transactional
    public Auth autenticar(String email, String password) {
        Auth auth = repository.findByEmail(email)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!"ACTIVO".equals(auth.getEstado()) || !coincideConPassword(auth, password)) {
            throw new CredencialesInvalidasException();
        }
        return auth;
    }

    private boolean coincideConPassword(Auth auth, String password) {
        String hash = auth.getPasswordHash();
        if (hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"))) {
            try {
                return passwordEncoder.matches(password, hash);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        if (hash != null && hash.equals(password)) {
            auth.setPasswordHash(passwordEncoder.encode(password));
            repository.save(auth);
            return true;
        }
        return false;
    }
    @Transactional
    public Auth actualizarAuth(Long id, AuthDTO dto) {
        log.info("Actualizando Auth con ID: {}", id);
        Auth auth = buscarPorId(id);

        if (!auth.getEmail().equals(dto.getEmail()) && repository.findByEmail(dto.getEmail()).isPresent()) {
            throw new AuthException("El nuevo correo ya está en uso por otra cuenta.");
        }

        auth.setEmail(dto.getEmail());
        auth.setPasswordHash(passwordEncoder.encode(dto.getPasswordHash()));
        auth.setRol(dto.getRol());

        return repository.save(auth);
    }

    @Transactional
    public Auth desactivarAuth(Long id) {
        log.info("Intentando desactivar Auth con ID: {}", id);
        Auth auth = buscarPorId(id);
        auth.setEstado("INACTIVO");
        Auth actualizada = repository.save(auth);
        log.info("Auth desactivado exitosamente");
        return actualizada;
    }
}
