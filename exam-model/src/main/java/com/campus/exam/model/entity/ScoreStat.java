package com.campus.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级统计结果落库快照（主读 Redis，表用于审计与缓存重建）
 */
@Data
@TableName("score_stat")
public class ScoreStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long classId;

    /** 统计指标 JSON */
    private String statJson;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
