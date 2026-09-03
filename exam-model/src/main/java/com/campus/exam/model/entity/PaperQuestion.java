package com.campus.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 试卷-题目关联，按题型分组并记录单题分值
 */
@Data
@TableName("paper_question")
public class PaperQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long questionId;

    /** 冗余题型，便于分组渲染 */
    private Integer groupType;

    /** 题号（组内顺序） */
    private Integer sortNo;

    /** 该题在本卷中的分值 */
    private Integer score;
}
