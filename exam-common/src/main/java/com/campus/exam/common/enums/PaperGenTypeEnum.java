package com.campus.exam.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 组卷方式：手动选题 / 智能约束求解
 */
@Getter
@AllArgsConstructor
public enum PaperGenTypeEnum {

    MANUAL("MANUAL", "手动组卷"),
    AUTO("AUTO", "智能组卷");

    private final String code;
    private final String desc;
}
