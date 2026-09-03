package com.campus.exam.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 试卷详情：按题型分组
 */
@Data
public class PaperDetailVO {

    private Long id;
    private String name;
    private Integer totalScore;
    private Integer suggestDuration;
    private String genType;
    private Integer status;
    private String constraintSnapshot;
    private List<Group> groups = new ArrayList<>();

    @Data
    public static class Group {
        private Integer questionType;
        private String typeDesc;
        private List<PaperQuestionItem> items = new ArrayList<>();
    }

    @Data
    public static class PaperQuestionItem {
        private Long paperQuestionId;
        private Long questionId;
        private Integer sortNo;
        private Integer score;
        private String stem;
        private String options;
        private String answer;
        private String analysis;
        private Integer difficulty;
    }
}
