package com.campus.exam.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.common.constant.RedisKeys;
import com.campus.exam.common.enums.ExamRecordStatusEnum;
import com.campus.exam.common.exception.BizException;
import com.campus.exam.common.result.ResultCode;
import com.campus.exam.mapper.ExamAnswerMapper;
import com.campus.exam.mapper.ExamMapper;
import com.campus.exam.mapper.ExamRecordMapper;
import com.campus.exam.mapper.PaperMapper;
import com.campus.exam.mapper.ScoreStatMapper;
import com.campus.exam.model.entity.Exam;
import com.campus.exam.model.entity.ExamRecord;
import com.campus.exam.model.entity.Paper;
import com.campus.exam.model.entity.ScoreStat;
import com.campus.exam.model.mq.ExamMqMessage;
import com.campus.exam.model.vo.ClassStatVO;
import com.campus.exam.service.support.ReliableMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 班级成绩统计：Cache-Aside，首次计算后写 Redis（TTL 30min），
 * 成绩发布/变更时主动删缓存，未命中再重算，统计计算本身也可由 MQ 异步触发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final PaperMapper paperMapper;
    private final ScoreStatMapper scoreStatMapper;
    private final StringRedisTemplate redisTemplate;
    private final ReliableMessageSender messageSender;

    public ClassStatVO getStat(Long examId) {
        Exam exam = mustExam(examId);
        String key = RedisKeys.classStat(exam.getClassId(), examId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            ClassStatVO vo = JSONUtil.toBean(cached, ClassStatVO.class);
            vo.setFromCache(true);
            return vo;
        }
        ClassStatVO vo = compute(examId);
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(vo),
                RedisKeys.STAT_TTL_MINUTES, TimeUnit.MINUTES);
        vo.setFromCache(false);
        return vo;
    }

    @Transactional
    public ClassStatVO compute(Long examId) {
        Exam exam = mustExam(examId);
        Paper paper = paperMapper.selectById(exam.getPaperId());
        List<ExamRecord> records = recordMapper.selectByExamAndStatus(examId, List.of(
                ExamRecordStatusEnum.GRADED.getCode(),
                ExamRecordStatusEnum.PUBLISHED.getCode()));

        ClassStatVO vo = new ClassStatVO();
        vo.setExamId(examId);
        vo.setClassId(exam.getClassId());
        vo.setExamName(exam.getExamName());
        vo.setAttendCount(records.size());
        vo.setFromCache(false);

        if (records.isEmpty()) {
            vo.setAvgScore(BigDecimal.ZERO);
            vo.setMaxScore(BigDecimal.ZERO);
            vo.setMinScore(BigDecimal.ZERO);
            vo.setPassRate(BigDecimal.ZERO);
            vo.setScoreSegments(Arrays.asList(0, 0, 0, 0, 0));
            vo.setQuestionCorrectRate(Map.of());
            cacheAndSnapshot(exam, vo);
            return vo;
        }

        BigDecimal sum = BigDecimal.ZERO, max = BigDecimal.ZERO,
                min = BigDecimal.valueOf(Double.MAX_VALUE), passLine;
        int[] seg = new int[5];
        int pass = 0;
        BigDecimal full = BigDecimal.valueOf(paper.getTotalScore());
        passLine = full.multiply(BigDecimal.valueOf(0.6));
        for (ExamRecord r : records) {
            BigDecimal s = r.getTotalScore() == null ? BigDecimal.ZERO : r.getTotalScore();
            sum = sum.add(s);
            max = max.max(s);
            min = min.min(s);
            if (s.compareTo(passLine) >= 0) {
                pass++;
            }
            double ratio = s.doubleValue() / full.doubleValue();
            if (ratio < 0.6) {
                seg[0]++;
            } else if (ratio < 0.7) {
                seg[1]++;
            } else if (ratio < 0.8) {
                seg[2]++;
            } else if (ratio < 0.9) {
                seg[3]++;
            } else {
                seg[4]++;
            }
        }
        int n = records.size();
        vo.setAvgScore(sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
        vo.setMaxScore(max);
        vo.setMinScore(min.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 0 ? BigDecimal.ZERO : min);
        vo.setPassRate(BigDecimal.valueOf(pass).divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP));
        vo.setScoreSegments(Arrays.asList(seg[0], seg[1], seg[2], seg[3], seg[4]));

        Map<Long, BigDecimal> rateMap = new HashMap<>();
        for (Map<String, Object> row : answerMapper.selectQuestionRates(examId)) {
            Long qid = ((Number) row.get("questionId")).longValue();
            BigDecimal rate = row.get("rate") == null ? BigDecimal.ZERO
                    : BigDecimal.valueOf(((Number) row.get("rate")).doubleValue())
                    .setScale(4, RoundingMode.HALF_UP);
            rateMap.put(qid, rate);
        }
        vo.setQuestionCorrectRate(rateMap);
        cacheAndSnapshot(exam, vo);
        return vo;
    }

    /** MQ 消费者：异步重算并刷新缓存 */
    public void consumeStat(String payload) {
        ExamMqMessage msg = JSONUtil.toBean(payload, ExamMqMessage.class);
        ClassStatVO vo = compute(msg.getExamId());
        Exam exam = mustExam(msg.getExamId());
        redisTemplate.opsForValue().set(RedisKeys.classStat(exam.getClassId(), msg.getExamId()),
                JSONUtil.toJsonStr(vo), RedisKeys.STAT_TTL_MINUTES, TimeUnit.MINUTES);
        messageSender.markConsumed(msg.getMsgKey());
        log.info("班级统计异步计算完成 examId={}", msg.getExamId());
    }

    private void cacheAndSnapshot(Exam exam, ClassStatVO vo) {
        ScoreStat exist = scoreStatMapper.selectOne(Wrappers.<ScoreStat>lambdaQuery()
                .eq(ScoreStat::getExamId, exam.getId()).last("LIMIT 1"));
        ScoreStat stat = exist == null ? new ScoreStat() : exist;
        stat.setExamId(exam.getId());
        stat.setClassId(exam.getClassId());
        stat.setStatJson(JSONUtil.toJsonStr(vo));
        stat.setVersion(exist == null ? 1 : (exist.getVersion() == null ? 1 : exist.getVersion() + 1));
        stat.setUpdateTime(LocalDateTime.now());
        if (exist == null) {
            stat.setCreateTime(LocalDateTime.now());
            scoreStatMapper.insert(stat);
        } else {
            scoreStatMapper.updateById(stat);
        }
    }

    private Exam mustExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(ResultCode.NOT_FOUND, "考试不存在");
        }
        return exam;
    }
}
