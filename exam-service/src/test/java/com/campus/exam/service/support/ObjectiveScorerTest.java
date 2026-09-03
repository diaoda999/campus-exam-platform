package com.campus.exam.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectiveScorerTest {

    @Test
    @DisplayName("单选精确匹配")
    void single() {
        assertEquals(new BigDecimal("10"), ObjectiveScorer.score(1, "A", "A", 10));
        assertEquals(BigDecimal.ZERO, ObjectiveScorer.score(1, "A", "B", 10));
        // 小写归一
        assertEquals(new BigDecimal("10"), ObjectiveScorer.score(1, "a", " A ", 10));
    }

    @Test
    @DisplayName("多选排序后比较，漏选错选不得分")
    void multiple() {
        assertEquals(new BigDecimal("8"), ObjectiveScorer.score(2, "ABD", "bda", 8));
        assertEquals(BigDecimal.ZERO, ObjectiveScorer.score(2, "ABD", "AB", 8));
        assertEquals(BigDecimal.ZERO, ObjectiveScorer.score(2, "AB", "ABC", 8));
    }

    @Test
    @DisplayName("判断")
    void judge() {
        assertEquals(new BigDecimal("5"), ObjectiveScorer.score(3, "TRUE", "true", 5));
        assertEquals(BigDecimal.ZERO, ObjectiveScorer.score(3, "FALSE", "TRUE", 5));
    }

    @Test
    @DisplayName("单空填空：多答案命中其一即得分，全角半角归一")
    void fillSingleBlank() {
        assertEquals(new BigDecimal("5"), ObjectiveScorer.score(4, "RR||可重复读", "可重复读", 5));
        // 全角字母归一为半角
        assertEquals(new BigDecimal("5"), ObjectiveScorer.score(4, "ABC", "ＡＢＣ", 5));
        assertEquals(BigDecimal.ZERO, ObjectiveScorer.score(4, "ABC", "ABD", 5));
    }

    @Test
    @DisplayName("多空填空按答对空数比例给分")
    void fillMultiBlanks() {
        // 两空，对一空得一半分数（10/2=5）
        assertEquals(new BigDecimal("5.00"), ObjectiveScorer.score(4, "接口##继承", "接口##组合", 10));
        assertEquals(new BigDecimal("10"), ObjectiveScorer.score(4, "接口##继承", "接口##继承", 10));
    }

    @Test
    @DisplayName("未作答得 0 分")
    void nullAnswer() {
        assertEquals(BigDecimal.ZERO, ObjectiveScorer.score(1, "A", null, 10));
        assertEquals(BigDecimal.ZERO, ObjectiveScorer.score(5, "参考答案", "任意", 10));
    }
}
