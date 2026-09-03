package com.campus.exam.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试层：绑定试卷、班级、时间窗口与监考策略
 */
@Data
@TableName("exam")
public class Exam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String examName;

    private Long paperId;

    private Long classId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 监考策略 JSON，如 {"switchLimit":3,"forceSubmit":true} */
    private String monitorConfig;

    /** 0未开始 1进行中 2已结束 3成绩已发布，见 ExamStatusEnum */
    private Integer status;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
