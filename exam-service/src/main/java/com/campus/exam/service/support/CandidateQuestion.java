package com.campus.exam.service.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 组卷候选题的内存视图（不直接依赖实体，保证求解器纯 Java 可测）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateQuestion {

    private Long questionId;
    private Integer type;
    private Integer difficulty;
    private Integer useCount;
    private Set<Long> knowledgeIds = new HashSet<>();
}
