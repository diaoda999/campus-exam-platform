package com.campus.exam.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 智能组卷结果：草稿试卷 + 求解指标 + 未满足约束说明
 */
@Data
@Builder
public class GenerateResultVO {

    private Long paperId;
    private Integer totalScore;
    private Double avgDifficulty;
    private Boolean success;
    /** 无法满足的约束描述（如“知识点[数组]题量不足”），成功为空 */
    private List<String> unmetConstraints;
}
