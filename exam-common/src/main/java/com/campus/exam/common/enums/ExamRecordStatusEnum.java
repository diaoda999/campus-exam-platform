package com.campus.exam.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 考生考试记录状态机：
 * NOT_STARTED -> IN_PROGRESS -> SUBMITTED / ABNORMAL_INTERRUPTED
 * IN_PROGRESS / ABNORMAL -> AUTO_COLLECTING -> GRADING / GRADED
 * GRADING -> GRADED -> PUBLISHED
 */
@Getter
@AllArgsConstructor
public enum ExamRecordStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "答题中"),
    SUBMITTED(2, "已交卷"),
    ABNORMAL_INTERRUPTED(3, "异常中断"),
    AUTO_COLLECTING(4, "自动收卷中"),
    GRADING(5, "待主观题批改"),
    GRADED(6, "阅卷完成"),
    PUBLISHED(7, "成绩已发布");

    private final Integer code;
    private final String desc;

    /** 是否终态：终态记录不允许重复收卷/重复交卷 */
    public boolean isFinal() {
        return this == GRADED || this == PUBLISHED;
    }
}
