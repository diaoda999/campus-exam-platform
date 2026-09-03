package com.campus.exam.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师批改列表：按考生聚合
 */
@Data
public class GradeTodoVO {

    private Long examId;
    private Long userId;
    private String studentName;
    private String username;
    private Integer recordStatus;
    private String recordStatusDesc;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal totalScore;
    /** 待批改主观题数量 */
    private Integer remainSubjectiveCount;
}
