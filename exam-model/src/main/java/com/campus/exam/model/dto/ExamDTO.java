package com.campus.exam.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamDTO {

    private Long id;

    @NotNull
    private String examName;

    @NotNull
    private Long paperId;

    @NotNull
    private Long classId;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 切屏次数上限，超过强制交卷；null 表示不限制 */
    private Integer switchLimit;
}
