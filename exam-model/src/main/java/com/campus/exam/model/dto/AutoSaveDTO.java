package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 自动保存单题作答（前端防抖 3 秒调用）
 */
@Data
public class AutoSaveDTO {

    @NotNull
    private Long examId;

    @NotNull
    private Long questionId;

    /** 作答内容：选择题为选项 key 拼接，填空/简答为文本 */
    private String content;
}
