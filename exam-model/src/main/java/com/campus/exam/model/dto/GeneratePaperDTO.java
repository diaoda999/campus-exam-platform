package com.campus.exam.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 智能组卷请求：题型数量 + 期望难度 + 知识点覆盖
 */
@Data
public class GeneratePaperDTO {

    private String name;

    @NotNull(message = "建议时长不能为空")
    private Integer suggestDuration;

    /** 期望平均难度 1-5，可空 */
    private Double targetDifficulty;

    /** 难度容差，默认 0.3 */
    private Double difficultyTolerance;

    @NotNull
    private List<GroupSpec> groups;

    /** 知识点覆盖硬约束：知识点至少命中多少题 */
    private List<CoverageSpec> coverage;

    @Data
    public static class GroupSpec {
        @NotNull
        private Integer questionType;
        @NotNull
        @Min(1)
        private Integer count;
        @NotNull
        @Min(1)
        private Integer scorePer;
    }

    @Data
    public static class CoverageSpec {
        @NotNull
        private Long knowledgeId;
        @NotNull
        @Min(1)
        private Integer minCount;
    }
}
