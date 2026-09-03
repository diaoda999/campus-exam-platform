package com.campus.exam.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 本地消息表状态，支撑“本地事务 + MQ 最终一致性”
 */
@Getter
@AllArgsConstructor
public enum LocalMessageStatusEnum {

    PENDING("PENDING", "待投递"),
    SENT("SENT", "已投递待消费"),
    CONSUMED("CONSUMED", "已消费完成"),
    DEAD("DEAD", "超过最大重试次数，进入死信");

    private final String code;
    private final String desc;
}
