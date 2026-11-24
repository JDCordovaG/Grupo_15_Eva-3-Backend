package com.BuildMyPC.msvc_build_service.Clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "msvc-auth-service", url = "${AUTH_SERVICE_URL:http://localhost:8085}")
public interface AuthClient {
    @GetMapping("/api/v1/auths/{id}")
    Object obtenerCuenta(@PathVariable Long id);
}
