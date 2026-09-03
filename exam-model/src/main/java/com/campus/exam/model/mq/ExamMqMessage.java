package com.campus.exam.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试领域统一 MQ 消息体（COLLECT / GRADE / STAT）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMqMessage implements Serializable {

    /** 与 local_message.msg_key 一致，消费幂等键 */
    private String msgKey;

    private String bizType;

    private Long examId;

    /** 收卷/汇总场景下的考生 ID；STAT 场景为空 */
    private Long userId;

    private LocalDateTime occurTime;
}
