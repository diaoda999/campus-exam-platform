package com.campus.exam.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 考试（整场）状态
 */
@Getter
@AllArgsConstructor
public enum ExamStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    FINISHED(2, "已结束"),
    PUBLISHED(3, "成绩已发布");

    private final Integer code;
    private final String desc;
}
