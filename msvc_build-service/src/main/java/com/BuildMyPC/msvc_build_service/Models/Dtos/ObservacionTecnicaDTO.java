package com.BuildMyPC.msvc_build_service.Models.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ObservacionTecnicaDTO {
    @NotBlank(message = "La observación técnica no puede estar vacía")
    private String observacionesTecnicas;
}
