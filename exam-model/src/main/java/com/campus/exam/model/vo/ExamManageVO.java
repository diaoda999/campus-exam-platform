package com.campus.exam.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师端考试列表项
 */
@Data
public class ExamManageVO {

    private Long id;
    private String examName;
    private Long paperId;
    private String paperName;
    private Long classId;
    private String className;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer status;
    private String statusDesc;
    /** 应交人数 / 已交人数 */
    private Integer totalStudents;
    private Integer submittedCount;
}
