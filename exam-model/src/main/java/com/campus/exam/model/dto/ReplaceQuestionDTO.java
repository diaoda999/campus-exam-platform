package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 草稿微调：用系统推荐题替换指定题
 */
@Data
public class ReplaceQuestionDTO {

    @NotNull
    private Long paperId;

    @NotNull
    private Long oldQuestionId;

    /** 指定用来替换的新题；为空则由系统按同题型+同难度+知识点不重复推荐一道 */
    private Long newQuestionId;
}
