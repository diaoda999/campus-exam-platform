package com.campus.exam.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生答题页试卷结构（严格剔除标准答案与解析）
 */
@Data
public class ExamPaperVO {

    private Long examId;
    private String examName;
    private Long recordId;
    private Integer totalScore;
    /** 服务端计算的考试截止时间戳（毫秒），前端倒计时以此为准 */
    private Long deadlineMillis;
    /** 一次性考试 Token，后续保存/心跳/交卷请求头携带 */
    private String examToken;
    private Integer switchLimit;
    private List<Group> groups = new ArrayList<>();

    @Data
    public static class Group {
        private Integer questionType;
        private String typeDesc;
        private List<QuestionItem> items = new ArrayList<>();
    }

    @Data
    public static class QuestionItem {
        private Long questionId;
        private Integer sortNo;
        private Integer score;
        private String stem;
        private String options;
    }
}
