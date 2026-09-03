package com.campus.exam.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 试卷状态：草稿可继续微调，正式试卷可被考试绑定
 */
@Getter
@AllArgsConstructor
public enum PaperStatusEnum {

    DRAFT(0, "草稿"),
    OFFICIAL(1, "正式");

    private final Integer code;
    private final String desc;
}
