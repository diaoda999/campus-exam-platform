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
 * 题目主表，五种题型共用一套结构
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 1单选 2多选 3判断 4填空 5简答，见 QuestionTypeEnum */
    private Integer type;

    /** 题干 */
    private String stem;

    /** 选项 JSON，如 [{"key":"A","text":"..."}]，非选择题为空 */
    private String options;

    /**
     * 标准答案：单选"A"；多选"ABD"；判断"TRUE/FALSE"；
     * 填空多答案以 || 分隔；简答为空
     */
    private String answer;

    private String analysis;

    /** 难度系数 1-5 */
    private Integer difficulty;

    /** 被试卷引用次数，用于组卷均衡 */
    private Integer useCount;

    private Long creatorId;

    /** 1 启用 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
