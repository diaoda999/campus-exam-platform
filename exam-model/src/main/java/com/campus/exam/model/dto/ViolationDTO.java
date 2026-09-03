package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ViolationDTO {

    @NotNull
    private Long examId;

    /** SWITCH_TAB / LEAVE */
    private String type;

    private String detail;
}
