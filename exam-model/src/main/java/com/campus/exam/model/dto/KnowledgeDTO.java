package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeDTO {

    private Long id;

    @NotBlank
    private String name;

    private Long parentId;
}
