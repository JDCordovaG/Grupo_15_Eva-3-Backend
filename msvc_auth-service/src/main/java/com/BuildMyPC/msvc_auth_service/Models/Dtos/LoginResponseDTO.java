package com.BuildMyPC.msvc_auth_service.Models.Dtos;

import com.BuildMyPC.msvc_auth_service.Models.Auth;

public record LoginResponseDTO(String token, UsuarioSesionDTO usuario, Long id, String email, String rol, String estado) {
    public static LoginResponseDTO desde(Auth auth, String token) {
        UsuarioSesionDTO usuario = new UsuarioSesionDTO(auth.getId(), auth.getEmail(), auth.getRol(), auth.getEstado());
        return new LoginResponseDTO(token, usuario, auth.getId(), auth.getEmail(), auth.getRol(), auth.getEstado());
    }

    public record UsuarioSesionDTO(Long id, String email, String rol, String estado) {}
}
