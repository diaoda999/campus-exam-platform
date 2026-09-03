package com.campus.exam.common.enums;

import com.campus.exam.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 题型：1 单选 2 多选 3 判断 4 填空 5 简答
 */
@Getter
@AllArgsConstructor
public enum QuestionTypeEnum {

    SINGLE(1, "单选题", true),
    MULTIPLE(2, "多选题", true),
    JUDGE(3, "判断题", true),
    FILL(4, "填空题", true),
    SHORT_ANSWER(5, "简答题", false);

    private final Integer code;
    private final String desc;
    /** 是否客观题（可由系统自动判分） */
    private final Boolean objective;

    public static QuestionTypeEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new BizException("未知题型: " + code));
    }

    public static boolean isObjective(Integer code) {
        return of(code).getObjective();
    }
}
