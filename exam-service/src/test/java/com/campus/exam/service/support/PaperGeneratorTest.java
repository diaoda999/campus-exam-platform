package com.campus.exam.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperGeneratorTest {

    /** 构造 count 道指定题型的候选，难度循环 1-5，按序号挂知识点 */
    private List<CandidateQuestion> buildPool(int type, int count) {
        List<CandidateQuestion> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            CandidateQuestion q = new CandidateQuestion();
            q.setQuestionId((long) (type * 1000 + i));
            q.setType(type);
            q.setDifficulty((i % 5) + 1);
            q.setUseCount(i % 3);
            q.setKnowledgeIds(Set.of((long) (i % 4 + 1)));
            list.add(q);
        }
        return list;
    }

    @Test
    @DisplayName("按题型数量正常组卷，选题数与需求一致且无重复")
    void shouldPickExactCount() {
        List<CandidateQuestion> pool = new ArrayList<>();
        pool.addAll(buildPool(1, 10));
        pool.addAll(buildPool(2, 8));

        List<PaperGenerator.GroupReq> groups = List.of(
                PaperGenerator.GroupReq.builder().questionType(1).count(5).build(),
                PaperGenerator.GroupReq.builder().questionType(2).count(3).build());

        PaperGenerator.GenResult result = PaperGenerator.generate(pool, groups, 3.0, 0.5, null, 50);

        assertTrue(result.isSuccess());
        assertEquals(8, result.getQuestionIds().size());
        assertEquals(8, Set.copyOf(result.getQuestionIds()).size());
    }

    @Test
    @DisplayName("知识点覆盖硬约束优先满足")
    void shouldMeetKnowledgeCoverage() {
        List<CandidateQuestion> pool = buildPool(1, 12);
        List<PaperGenerator.GroupReq> groups = List.of(
                PaperGenerator.GroupReq.builder().questionType(1).count(6).build());
        // 知识点 1 要求至少 3 题：候选中知识点 = (i%4)+1，i=4,8,12 命中知识点1
        PaperGenerator.GenResult result = PaperGenerator.generate(pool, groups, null, 0.3,
                Map.of(1L, 3), 50);

        long hitKp1 = result.getQuestionIds().stream()
                .map(id -> pool.stream().filter(q -> q.getQuestionId().equals(id)).findFirst().orElseThrow())
                .filter(q -> q.getKnowledgeIds().contains(1L))
                .count();
        assertTrue(result.isSuccess(), result.getUnmetConstraints()::toString);
        assertTrue(hitKp1 >= 3, "知识点1至少3题，实际" + hitKp1);
    }

    @Test
    @DisplayName("候选池题量不足时返回未满足约束而不是强行出卷")
    void shouldReportUnmetWhenPoolShort() {
        List<CandidateQuestion> pool = buildPool(1, 3);
        List<PaperGenerator.GroupReq> groups = List.of(
                PaperGenerator.GroupReq.builder().questionType(1).count(8).build());

        PaperGenerator.GenResult result = PaperGenerator.generate(pool, groups, null, 0.3, null, 10);

        assertFalse(result.isSuccess());
        assertFalse(result.getUnmetConstraints().isEmpty());
        assertEquals(3, result.getQuestionIds().size());
    }

    @Test
    @DisplayName("难度回溯后平均难度向目标收敛")
    void shouldConvergeDifficulty() {
        // 全是难度 1 的候选 10 道
        List<CandidateQuestion> easy = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            easy.add(new CandidateQuestion((long) i, 1, 1, 0, Set.of(1L)));
        }
        List<PaperGenerator.GroupReq> groups = List.of(
                PaperGenerator.GroupReq.builder().questionType(1).count(5).build());
        PaperGenerator.GenResult result = PaperGenerator.generate(easy, groups, 5.0, 0.3, null, 50);

        // 候选全为难度1，无法逼近目标5，应给出未满足说明，平均难度仍如实反映为1
        assertEquals(1.0, result.getAvgDifficulty());
        assertFalse(result.isSuccess());
    }
}
