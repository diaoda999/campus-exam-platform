package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 交卷请求。answers 为可选兜底：正常情况下以 Redis 快照为准，
 * 仅当快照缺失（如 Redis 故障恢复后）使用请求体合并
 */
@Data
public class SubmitExamDTO {

    @NotNull
    private Long examId;

    /** questionId -> 作答内容 */
    private Map<Long, String> answers;
}
