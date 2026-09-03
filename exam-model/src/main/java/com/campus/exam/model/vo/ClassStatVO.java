package com.campus.exam.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 班级成绩统计看板
 */
@Data
public class ClassStatVO {

    private Long examId;
    private Long classId;
    private String examName;
    private Integer attendCount;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private BigDecimal passRate;
    /** 分数段：[0-60,60-70,70-80,80-90,90-100] 人数 */
    private List<Integer> scoreSegments;
    /** 每道题得分率：questionId -> 0~1 */
    private Map<Long, BigDecimal> questionCorrectRate;
    private Boolean fromCache;
}
