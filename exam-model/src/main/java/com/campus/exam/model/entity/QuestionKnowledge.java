package com.campus.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目-知识点 多对多关联
 */
@Data
@TableName("question_knowledge")
public class QuestionKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Long knowledgeId;
}
