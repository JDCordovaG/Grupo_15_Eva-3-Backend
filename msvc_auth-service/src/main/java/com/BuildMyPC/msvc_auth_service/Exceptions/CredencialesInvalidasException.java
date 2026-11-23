package com.BuildMyPC.msvc_auth_service.Exceptions;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos, o cuenta inactiva.");
    }
}
