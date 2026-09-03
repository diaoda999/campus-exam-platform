package com.campus.exam.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.common.enums.QuestionTypeEnum;
import com.campus.exam.mapper.ExamAnswerMapper;
import com.campus.exam.mapper.PaperMapper;
import com.campus.exam.mapper.PaperQuestionMapper;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.model.entity.Exam;
import com.campus.exam.model.entity.ExamAnswer;
import com.campus.exam.model.entity.Paper;
import com.campus.exam.model.entity.PaperQuestion;
import com.campus.exam.model.entity.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作答持久化 + 客观题判分。
 * 学生主动交卷与 MQ 自动收卷共用同一套落库逻辑，保证两条链路结果一致。
 */
@Component
@RequiredArgsConstructor
public class ExamAnswerPersister {

    private final ExamAnswerMapper answerMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;

    public record PersistResult(BigDecimal objectiveScore, boolean hasSubjective, int answerCount) {
    }

    /**
     * @param snapshot 题目ID -> 作答内容（来自 Redis Hash，缺失时由调用方用请求体兜底）
     */
    @Transactional
    public PersistResult persist(Exam exam, Long userId, Map<Long, String> snapshot) {
        Paper paper = paperMapper.selectById(exam.getPaperId());
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(Wrappers.<PaperQuestion>lambdaQuery()
                .eq(PaperQuestion::getPaperId, paper.getId())
                .orderByAsc(PaperQuestion::getGroupType).orderByAsc(PaperQuestion::getSortNo));
        List<Question> questions = questionMapper.selectBatchIds(
                pqs.stream().map(PaperQuestion::getQuestionId).toList());
        Map<Long, Question> qMap = new HashMap<>();
        questions.forEach(q -> qMap.put(q.getId(), q));

        // 已有作答（重复收卷场景），按题更新而不是重复插入
        Map<Long, ExamAnswer> existed = new HashMap<>();
        answerMapper.selectByExamAndUser(exam.getId(), userId)
                .forEach(a -> existed.put(a.getQuestionId(), a));

        BigDecimal objectiveScore = BigDecimal.ZERO;
        boolean hasSubjective = false;
        int count = 0;

        for (PaperQuestion pq : pqs) {
            Question q = qMap.get(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            String content = snapshot == null ? null : snapshot.get(q.getId());
            boolean objective = QuestionTypeEnum.isObjective(q.getType());
            BigDecimal score = BigDecimal.ZERO;
            if (objective) {
                score = ObjectiveScorer.score(q.getType(), q.getAnswer(), content, pq.getScore());
                objectiveScore = objectiveScore.add(score);
            } else {
                hasSubjective = true;
            }
            ExamAnswer answer = existed.get(q.getId());
            if (answer == null) {
                answer = new ExamAnswer();
                answer.setExamId(exam.getId());
                answer.setUserId(userId);
                answer.setQuestionId(q.getId());
                answer.setContent(content);
                answer.setScore(score);
                answer.setCreateTime(LocalDateTime.now());
                answer.setUpdateTime(LocalDateTime.now());
                answerMapper.insert(answer);
            } else {
                answer.setContent(content);
                // 主观题已被教师打分时不覆盖分数
                if (objective || answer.getScore() == null) {
                    answer.setScore(score);
                }
                answer.setUpdateTime(LocalDateTime.now());
                answerMapper.updateById(answer);
            }
            count++;
        }
        return new PersistResult(objectiveScore, hasSubjective, count);
    }
}
