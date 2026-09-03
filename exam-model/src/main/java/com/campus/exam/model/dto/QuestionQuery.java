package com.campus.exam.model.dto;

import lombok.Data;

@Data
public class QuestionQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer type;
    private Integer difficulty;
    private Long knowledgeId;
    private String keyword;
    private Integer status;
}
