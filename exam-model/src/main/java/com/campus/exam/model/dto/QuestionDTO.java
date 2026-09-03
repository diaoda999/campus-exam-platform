package com.campus.exam.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionDTO {

    private Long id;

    @NotNull(message = "题型不能为空")
    private Integer type;

    @NotBlank(message = "题干不能为空")
    private String stem;

    /** 选项 JSON 字符串 */
    private String options;

    private String answer;

    private String analysis;

    @NotNull
    @Min(value = 1, message = "难度为1-5")
    @Max(value = 5, message = "难度为1-5")
    private Integer difficulty;

    private List<Long> knowledgeIds;

    private Integer status;
}
