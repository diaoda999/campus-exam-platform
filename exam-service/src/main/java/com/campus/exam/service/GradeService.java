package com.campus.exam.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.common.constant.MQConstants;
import com.campus.exam.common.constant.RedisKeys;
import com.campus.exam.common.enums.ExamRecordStatusEnum;
import com.campus.exam.common.enums.ExamStatusEnum;
import com.campus.exam.common.enums.QuestionTypeEnum;
import com.campus.exam.common.exception.BizException;
import com.campus.exam.common.result.ResultCode;
import com.campus.exam.mapper.ExamAnswerMapper;
import com.campus.exam.mapper.ExamMapper;
import com.campus.exam.mapper.ExamRecordMapper;
import com.campus.exam.mapper.PaperMapper;
import com.campus.exam.mapper.PaperQuestionMapper;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.mapper.SysUserMapper;
import com.campus.exam.model.dto.GradeFinishDTO;
import com.campus.exam.model.dto.GradeScoreDTO;
import com.campus.exam.model.entity.Exam;
import com.campus.exam.model.entity.ExamAnswer;
import com.campus.exam.model.entity.ExamRecord;
import com.campus.exam.model.entity.Paper;
import com.campus.exam.model.entity.PaperQuestion;
import com.campus.exam.model.entity.Question;
import com.campus.exam.model.entity.SysUser;
import com.campus.exam.model.mq.ExamMqMessage;
import com.campus.exam.model.vo.GradeDetailVO;
import com.campus.exam.model.vo.GradeTodoVO;
import com.campus.exam.service.support.ReliableMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final ReliableMessageSender messageSender;

    /** 待批/已批考生列表 */
    public List<GradeTodoVO> todoList(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(ResultCode.NOT_FOUND, "考试不存在");
        }
        List<SysUser> students = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getClassId, exam.getClassId()));
        List<ExamRecord> records = recordMapper.selectByExamAndStatus(examId, List.of(
                ExamRecordStatusEnum.GRADING.getCode(),
                ExamRecordStatusEnum.GRADED.getCode(),
                ExamRecordStatusEnum.PUBLISHED.getCode()));
        Map<Long, ExamRecord> recordMap = new HashMap<>();
        records.forEach(r -> recordMap.put(r.getUserId(), r));

        List<GradeTodoVO> result = new ArrayList<>();
        for (SysUser s : students) {
            ExamRecord r = recordMap.get(s.getId());
            if (r == null) {
                continue;
            }
            GradeTodoVO vo = new GradeTodoVO();
            vo.setExamId(examId);
            vo.setUserId(s.getId());
            vo.setStudentName(s.getRealName());
            vo.setUsername(s.getUsername());
            vo.setRecordStatus(r.getStatus());
            vo.setRecordStatusDesc(ExamRecordStatusEnum.values()[r.getStatus()].getDesc());
            vo.setObjectiveScore(r.getObjectiveScore());
            vo.setSubjectiveScore(r.getSubjectiveScore());
            vo.setTotalScore(r.getTotalScore());
            vo.setRemainSubjectiveCount(countUngradedSubjective(examId, s.getId()));
            result.add(vo);
        }
        return result;
    }

    private int countUngradedSubjective(Long examId, Long userId) {
        List<GradeDetailVO.AnswerItem> items = answerMapper.selectGradeItems(examId, userId);
        return (int) items.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getObjective()))
                .filter(i -> i.getScore() == null).count();
    }

    /** 某考生整卷批改详情 */
    public GradeDetailVO detail(Long examId, Long userId) {
        ExamRecord record = recordMapper.selectByExamAndUser(examId, userId);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND, "无该考生考试记录");
        }
        SysUser student = userMapper.selectById(userId);
        List<GradeDetailVO.AnswerItem> items = answerMapper.selectGradeItems(examId, userId);
        List<Question> questions = questionMapper.selectBatchIds(items.stream()
                .map(GradeDetailVO.AnswerItem::getQuestionId).toList());
        Map<Long, Question> qMap = new HashMap<>();
        questions.forEach(q -> qMap.put(q.getId(), q));
        items.forEach(i -> {
            QuestionTypeEnum type = QuestionTypeEnum.of(i.getType());
            i.setTypeDesc(type.getDesc());
            i.setObjective(type.getObjective());
            Question q = qMap.get(i.getQuestionId());
            if (q != null && i.getStandardAnswer() == null) {
                i.setStandardAnswer(q.getAnswer());
            }
        });
        GradeDetailVO vo = new GradeDetailVO();
        vo.setExamId(examId);
        vo.setUserId(userId);
        vo.setStudentName(student == null ? null : student.getRealName());
        vo.setRecordStatus(record.getStatus());
        vo.setObjectiveScore(record.getObjectiveScore());
        vo.setSubjectiveScore(record.getSubjectiveScore());
        vo.setItems(items);
        return vo;
    }

    /** 逐题打分（同步更新 Redis 临时成绩，断电不丢批改进度） */
    @Transactional
    public void score(GradeScoreDTO dto) {
        Paper paper = paperOfExam(dto.getExamId());
        PaperQuestion pq = paperQuestionMapper.selectOne(Wrappers.<PaperQuestion>lambdaQuery()
                .eq(PaperQuestion::getPaperId, paper.getId())
                .eq(PaperQuestion::getQuestionId, dto.getQuestionId()));
        if (pq == null) {
            throw new BizException(ResultCode.NOT_FOUND, "试卷中不存在该题");
        }
        if (dto.getScore() == null || dto.getScore().compareTo(BigDecimal.ZERO) < 0
                || dto.getScore().compareTo(BigDecimal.valueOf(pq.getScore())) > 0) {
            throw new BizException(ResultCode.GRADE_SCORE_ILLEGAL);
        }
        ExamAnswer answer = answerMapper.selectOne(Wrappers.<ExamAnswer>lambdaQuery()
                .eq(ExamAnswer::getExamId, dto.getExamId())
                .eq(ExamAnswer::getUserId, dto.getUserId())
                .eq(ExamAnswer::getQuestionId, dto.getQuestionId()));
        if (answer == null) {
            throw new BizException(ResultCode.NOT_FOUND, "无该题作答记录");
        }
        answer.setScore(dto.getScore());
        answer.setComment(dto.getComment());
        answer.setUpdateTime(LocalDateTime.now());
        answerMapper.updateById(answer);

        String key = RedisKeys.gradeTemp(dto.getExamId(), dto.getUserId());
        redisTemplate.opsForHash().put(key, String.valueOf(dto.getQuestionId()),
                dto.getScore().toPlainString());
        redisTemplate.expire(key, 24, java.util.concurrent.TimeUnit.HOURS);
    }

    /** 教师确认某考生主观题批改完成，异步汇总总分 */
    @Transactional
    public void finish(GradeFinishDTO dto) {
        int remain = countUngradedSubjective(dto.getExamId(), dto.getUserId());
        if (remain > 0) {
            throw new BizException("还有 " + remain + " 道主观题未打分");
        }
        ExamMqMessage msg = ExamMqMessage.builder()
                .bizType(MQConstants.BIZ_GRADE)
                .examId(dto.getExamId())
                .userId(dto.getUserId())
                .occurTime(LocalDateTime.now())
                .build();
        messageSender.prepare(MQConstants.BIZ_GRADE, MQConstants.EXAM_EXCHANGE,
                MQConstants.GRADE_ROUTING_KEY, msg);
    }

    /** 成绩汇总消费者：客观分 + 主观分 = 最终成绩，状态推进到 GRADED */
    @Transactional
    public void consumeGrade(String payload) {
        ExamMqMessage msg = JSONUtil.toBean(payload, ExamMqMessage.class);
        ExamRecord record = recordMapper.selectByExamAndUser(msg.getExamId(), msg.getUserId());
        if (record == null) {
            messageSender.markConsumed(msg.getMsgKey());
            return;
        }
        List<ExamAnswer> answers = answerMapper.selectByExamAndUser(msg.getExamId(), msg.getUserId());
        BigDecimal subjective = BigDecimal.ZERO;
        for (ExamAnswer a : answers) {
            Question q = questionMapper.selectById(a.getQuestionId());
            if (q != null && !QuestionTypeEnum.isObjective(q.getType()) && a.getScore() != null) {
                subjective = subjective.add(a.getScore());
            }
        }
        BigDecimal objective = record.getObjectiveScore() == null ? BigDecimal.ZERO : record.getObjectiveScore();
        ExamRecord update = new ExamRecord();
        update.setId(record.getId());
        update.setSubjectiveScore(subjective);
        update.setTotalScore(objective.add(subjective));
        update.setStatus(ExamRecordStatusEnum.GRADED.getCode());
        recordMapper.updateById(update);
        redisTemplate.delete(RedisKeys.gradeTemp(msg.getExamId(), msg.getUserId()));
        messageSender.markConsumed(msg.getMsgKey());
        log.info("成绩汇总完成 examId={} userId={} total={}", msg.getExamId(), msg.getUserId(),
                objective.add(subjective));
    }

    /** 发布整场考试成绩，并触发异步统计 */
    @Transactional
    public void publish(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(ResultCode.NOT_FOUND, "考试不存在");
        }
        List<ExamRecord> grading = recordMapper.selectByExamAndStatus(examId,
                List.of(ExamRecordStatusEnum.GRADING.getCode()));
        if (!grading.isEmpty()) {
            throw new BizException("仍有考生主观题未批改完成，不能发布");
        }
        List<ExamRecord> graded = recordMapper.selectByExamAndStatus(examId,
                List.of(ExamRecordStatusEnum.GRADED.getCode()));
        for (ExamRecord r : graded) {
            ExamRecord u = new ExamRecord();
            u.setId(r.getId());
            u.setStatus(ExamRecordStatusEnum.PUBLISHED.getCode());
            recordMapper.updateById(u);
        }
        Exam eu = new Exam();
        eu.setId(examId);
        eu.setStatus(ExamStatusEnum.PUBLISHED.getCode());
        examMapper.updateById(eu);

        // 成绩变更，失效旧统计缓存
        redisTemplate.delete(RedisKeys.classStat(exam.getClassId(), examId));

        ExamMqMessage statMsg = ExamMqMessage.builder()
                .bizType(MQConstants.BIZ_STAT)
                .examId(examId)
                .occurTime(LocalDateTime.now())
                .build();
        messageSender.prepare(MQConstants.BIZ_STAT, MQConstants.EXAM_EXCHANGE,
                MQConstants.STAT_ROUTING_KEY, statMsg);
    }

    /** 学生查看自己已发布成绩 */
    public ExamRecord myResult(Long examId, Long userId) {
        return recordMapper.selectByExamAndUser(examId, userId);
    }

    private Paper paperOfExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(ResultCode.NOT_FOUND, "考试不存在");
        }
        return paperMapper.selectById(exam.getPaperId());
    }
}
