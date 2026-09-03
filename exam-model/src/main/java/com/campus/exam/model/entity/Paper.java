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
 * 试卷模板层
 */
@Data
@TableName("paper")
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 总分 */
    private Integer totalScore;

    /** 建议作答时长（分钟） */
    private Integer suggestDuration;

    /** MANUAL / AUTO */
    private String genType;

    /** 0 草稿 1 正式 */
    private Integer status;

    /** 智能组卷约束快照（JSON），让组卷结果可追溯 */
    private String constraintSnapshot;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
