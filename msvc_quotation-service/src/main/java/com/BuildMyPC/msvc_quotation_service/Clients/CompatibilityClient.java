package com.BuildMyPC.msvc_quotation_service.Clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "msvc-compatibility", url = "http://localhost:8088")
public interface CompatibilityClient {
    @GetMapping("/api/v1/compatibilities/build/{buildId}")
    Map<String, Object> obtenerEstadoCompatibilidad(@PathVariable("buildId") Long buildId);
}
