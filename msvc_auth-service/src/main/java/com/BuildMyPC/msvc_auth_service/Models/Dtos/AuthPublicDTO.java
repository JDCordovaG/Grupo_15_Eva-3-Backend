package com.BuildMyPC.msvc_auth_service.Models.Dtos;

import com.BuildMyPC.msvc_auth_service.Models.Auth;

public record AuthPublicDTO(Long id, String email, String rol, String estado) {
    public static AuthPublicDTO desde(Auth auth) {
        return new AuthPublicDTO(auth.getId(), auth.getEmail(), auth.getRol(), auth.getEstado());
    }
}
