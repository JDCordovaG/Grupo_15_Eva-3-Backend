package com.BuildMyPC.msvc_gateway;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayStatusController {

    @GetMapping("/")
    public Map<String, String> status() {
        return Map.of(
                "servicio", "BuildMyPC API Gateway",
                "estado", "activo",
                "api", "/api",
                "salud", "/actuator/health"
        );
    }
}
