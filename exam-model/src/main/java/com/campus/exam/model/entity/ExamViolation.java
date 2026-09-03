package com.campus.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试违规记录（切屏/离开页面等）
 */
@Data
@TableName("exam_violation")
public class ExamViolation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long userId;

    /** SWITCH_TAB / LEAVE / FORCE_SUBMIT */
    private String type;

    private String detail;

    private LocalDateTime createTime;
}
