package com.campus.exam.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生视角的考试列表项
 */
@Data
public class StudentExamVO {

    private Long examId;
    private String examName;
    private Long paperId;
    private String paperName;
    private Integer totalScore;
    private Integer suggestDuration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 本人考试记录状态，null 表示未进入 */
    private Integer recordStatus;
    private String recordStatusDesc;

    /** 服务端当前时间，前端据此与 start/end 计算状态，避免改本地时钟作弊 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime serverNow;
}
