package com.campus.exam.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionVO {

    private Long id;
    private Integer type;
    private String typeDesc;
    private String stem;
    private String options;
    /** 教师视角返回答案与解析；学生考试场景由专门接口剔除 */
    private String answer;
    private String analysis;
    private Integer difficulty;
    private Integer useCount;
    private Integer status;
    private List<Long> knowledgeIds;
    private List<String> knowledgeNames;
    private LocalDateTime createTime;
}
