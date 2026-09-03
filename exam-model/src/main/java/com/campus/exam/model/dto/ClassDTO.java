package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassDTO {

    private Long id;

    @NotBlank
    private String className;
}
