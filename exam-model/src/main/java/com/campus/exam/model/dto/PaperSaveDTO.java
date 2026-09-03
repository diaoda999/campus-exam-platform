package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 手动组卷 / 草稿微调后保存
 */
@Data
public class PaperSaveDTO {

    private Long id;

    @NotNull
    private String name;

    @NotNull
    private Integer suggestDuration;

    /** 试卷题目顺序：questionId + 分值 */
    @NotNull
    private List<PaperItem> items;

    /** true 时保存为正式试卷 */
    private Boolean publish = false;

    @Data
    public static class PaperItem {
        @NotNull
        private Long questionId;
        @NotNull
        private Integer score;
    }
}
