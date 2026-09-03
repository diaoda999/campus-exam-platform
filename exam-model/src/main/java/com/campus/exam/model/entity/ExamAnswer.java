package com.campus.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考生单题最终作答（收卷后持久化，答题过程中只在 Redis Hash）
 */
@Data
@TableName("exam_answer")
public class ExamAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long userId;

    private Long questionId;

    /** 学生作答内容 */
    private String content;

    private BigDecimal score;

    /** 教师评语（主观题） */
    private String comment;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
