package com.campus.exam.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交卷结果：客观题即时出分预览
 */
@Data
@Builder
public class SubmitResultVO {

    private Long recordId;
    private Integer recordStatus;
    private String recordStatusDesc;
    private BigDecimal objectiveScore;
    private BigDecimal totalScore;
    /** 是否还有主观题待教师批改 */
    private Boolean waitingGrade;
}
