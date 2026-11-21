package com.BuildMyPC.msvc_build_service.Services;

import com.BuildMyPC.msvc_build_service.Clients.CompatibilityClient;
import com.BuildMyPC.msvc_build_service.Clients.AuthClient;
import com.BuildMyPC.msvc_build_service.Exceptions.BuildException;
import com.BuildMyPC.msvc_build_service.Models.Build;
import com.BuildMyPC.msvc_build_service.Models.Dtos.BuildDTO;
import com.BuildMyPC.msvc_build_service.Repositories.BuildRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class BuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildService.class);
    private final BuildRepository repository;
    private final AuthClient authClient;
    private final CompatibilityClient compatibilityClient;

    public BuildService(BuildRepository repository, AuthClient authClient, CompatibilityClient compatibilityClient) {
        this.repository = repository;
        this.authClient = authClient;
        this.compatibilityClient = compatibilityClient;
    }

    @Transactional
    public Build crearBuild(BuildDTO dto) {
        log.info("Iniciando creación de Build con id de usuario: {}", dto.getUsuarioId());

        try {
            authClient.obtenerCuenta(dto.getUsuarioId());
        } catch (Exception e) {
            log.error("No se pudo validar la cuenta {} en auth-service", dto.getUsuarioId(), e);
            throw new BuildException("La cuenta no existe o auth-service no está disponible");
        }

        Build build = new Build();
        build.setUsuarioId(dto.getUsuarioId());
        build.setCpuId(dto.getCpuId());
        build.setGpuId(dto.getGpuId());
        build.setMotherboardId(dto.getMotherboardId());
        build.setRamId(dto.getRamId());
        build.setFuenteId(dto.getFuenteId());
        build.setEstado("BORRADOR");
        LocalDateTime ahora = LocalDateTime.now();
        build.setFechaCreacion(ahora);
        build.setFechaActualizacion(ahora);

        Build guardada = repository.save(build);

        try {
            Map<String, Object> resultado = compatibilityClient.validarBuild(Map.of(
                    "buildId", guardada.getId(),
                    "cpuId", guardada.getCpuId(),
                    "gpuId", guardada.getGpuId(),
                    "motherboardId", guardada.getMotherboardId(),
                    "ramId", guardada.getRamId(),
                    "fuenteId", guardada.getFuenteId()
            ));

            boolean esCompatible = (boolean) resultado.getOrDefault("compatible", false);
            guardada.setEstado(esCompatible ? "VALIDADA" : "INCOMPATIBLE");
            guardada.setFechaActualizacion(LocalDateTime.now());
            guardada = repository.save(guardada);

            log.info("Resultado de compatibilidad procesado. Nuevo estado: {}", guardada.getEstado());
        } catch (Exception e) {
            log.error("Error al validar compatibilidad: {}. Se mantiene en BORRADOR.", e.getMessage());
        }

        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Build> listarTodas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Build> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public Build buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BuildException("Build no encontrada con ID: " + id));
    }

    @Transactional
    public Build actualizarBuild(Long id, BuildDTO dto) {
        Build build = buscarPorId(id);

        build.setCpuId(dto.getCpuId());
        build.setGpuId(dto.getGpuId());
        build.setMotherboardId(dto.getMotherboardId());
        build.setRamId(dto.getRamId());
        build.setFuenteId(dto.getFuenteId());
        build.setEstado("BORRADOR"); // Al modificarse, pierde su validación anterior.

        return repository.save(build);
    }

    @Transactional
    public Build archivarBuild(Long id) {
        Build build = buscarPorId(id);
        build.setEstado("ARCHIVADA");
        build.setFechaActualizacion(LocalDateTime.now());
        return repository.save(build);
    }

    @Transactional
    public Build desarchivarBuild(Long id) {
        Build build = buscarPorId(id);
        if (!"ARCHIVADA".equals(build.getEstado())) {
            throw new BuildException("Solo se pueden desarchivar builds que estén archivadas.");
        }
        build.setEstado("BORRADOR");
        build.setFechaActualizacion(LocalDateTime.now());
        return repository.save(build);
    }

    @Transactional
    public void eliminarBuild(Long id) {
        log.info("Intentando eliminar Build con ID: {}", id);
        Build build = buscarPorId(id);
        repository.delete(build);
        log.info("Build eliminada exitosamente");
    }
}
