package com.BuildMyPC.msvc_auth_service.Models.Dtos;

import com.BuildMyPC.msvc_auth_service.Models.Auth;

public record LoginResponseDTO(Long id, String email, String rol, String estado) {
    public static LoginResponseDTO desde(Auth auth) {
        return new LoginResponseDTO(auth.getId(), auth.getEmail(), auth.getRol(), auth.getEstado());
    }
}
