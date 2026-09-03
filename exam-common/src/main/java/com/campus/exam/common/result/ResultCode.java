package com.campus.exam.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统级错误码：1xxxx 通用，2xxxx 题库/试卷，3xxxx 考试，4xxxx 阅卷/成绩
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    BIZ_ERROR(10000, "业务处理失败"),
    PARAM_ERROR(10001, "参数校验失败"),
    UNAUTHORIZED(10002, "未登录或登录已过期"),
    FORBIDDEN(10003, "没有访问权限"),
    NOT_FOUND(10004, "资源不存在"),
    REPEAT_SUBMIT(10005, "请勿重复提交"),
    SERVER_ERROR(10500, "服务器内部错误"),

    QUESTION_NOT_ENOUGH(20001, "题库题量不足以完成组卷"),
    CONSTRAINT_UNSATISFIED(20002, "组卷约束无法满足"),
    PAPER_NOT_DRAFT(20003, "仅草稿状态试卷允许该操作"),

    EXAM_NOT_START(30001, "考试尚未开始"),
    EXAM_FINISHED(30002, "考试已结束"),
    EXAM_TOKEN_INVALID(30003, "考试凭证无效或与考生不匹配"),
    EXAM_RECORD_CONFLICT(30004, "考试状态异常，无法执行该操作"),
    NOT_EXAM_STUDENT(30005, "你不在本场考试的参考名单内"),

    GRADE_SCORE_ILLEGAL(40001, "批改分数超出题目分值范围");

    private final Integer code;
    private final String message;
}
