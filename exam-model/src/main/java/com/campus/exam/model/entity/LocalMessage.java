package com.campus.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表：业务数据与 MQ 投递的最终一致性锚点
 */
@Data
@TableName("local_message")
public class LocalMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务幂等键，唯一 */
    private String msgKey;

    /** COLLECT / GRADE / STAT */
    private String bizType;

    private String exchange;

    private String routingKey;

    private String payload;

    /** PENDING / SENT / CONSUMED / DEAD */
    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
