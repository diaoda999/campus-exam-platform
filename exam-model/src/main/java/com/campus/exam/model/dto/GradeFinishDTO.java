package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教师确认某考生的主观题已全部批改，触发异步汇总
 */
@Data
public class GradeFinishDTO {

    @NotNull
    private Long examId;

    @NotNull
    private Long userId;
}
