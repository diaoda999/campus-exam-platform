package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeScoreDTO {

    @NotNull
    private Long examId;

    @NotNull
    private Long userId;

    @NotNull
    private Long questionId;

    @NotNull
    private BigDecimal score;

    private String comment;
}
