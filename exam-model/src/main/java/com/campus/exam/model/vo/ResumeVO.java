package com.campus.exam.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 断点续考返回：试卷结构 + Redis 快照答案 + 剩余时间
 */
@Data
@Builder
public class ResumeVO {

    private ExamPaperVO paper;

    /** questionId -> 已作答内容 */
    private Map<Long, String> answers;

    /** 服务端计算的剩余秒数 */
    private Long remainSeconds;

    private Integer recordStatus;
}
