package com.campus.exam.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 某考生的批改详情
 */
@Data
public class GradeDetailVO {

    private Long examId;
    private Long userId;
    private String studentName;
    private Integer recordStatus;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private List<AnswerItem> items;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private Integer type;
        private String typeDesc;
        private String stem;
        private String options;
        private String standardAnswer;
        private String content;
        private Integer fullScore;
        private BigDecimal score;
        private String comment;
        private Boolean objective;
    }
}
