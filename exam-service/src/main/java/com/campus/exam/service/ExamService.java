package com.campus.exam.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exam.common.constant.RedisKeys;
import com.campus.exam.common.enums.ExamRecordStatusEnum;
import com.campus.exam.common.enums.ExamStatusEnum;
import com.campus.exam.common.enums.QuestionTypeEnum;
import com.campus.exam.common.exception.BizException;
import com.campus.exam.common.result.PageResult;
import com.campus.exam.common.result.ResultCode;
import com.campus.exam.mapper.ExamAnswerMapper;
import com.campus.exam.mapper.ExamMapper;
import com.campus.exam.mapper.ExamRecordMapper;
import com.campus.exam.mapper.ExamViolationMapper;
import com.campus.exam.mapper.PaperMapper;
import com.campus.exam.mapper.PaperQuestionMapper;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.mapper.SysClassMapper;
import com.campus.exam.mapper.SysUserMapper;
import com.campus.exam.model.dto.AutoSaveDTO;
import com.campus.exam.model.dto.ExamDTO;
import com.campus.exam.model.dto.SubmitExamDTO;
import com.campus.exam.model.dto.ViolationDTO;
import com.campus.exam.model.entity.Exam;
import com.campus.exam.model.entity.ExamRecord;
import com.campus.exam.model.entity.ExamViolation;
import com.campus.exam.model.entity.Paper;
import com.campus.exam.model.entity.PaperQuestion;
import com.campus.exam.model.entity.Question;
import com.campus.exam.model.entity.SysClass;
import com.campus.exam.model.entity.SysUser;
import com.campus.exam.model.vo.ExamManageVO;
import com.campus.exam.model.vo.ExamPaperVO;
import com.campus.exam.model.vo.ResumeVO;
import com.campus.exam.model.vo.StudentExamVO;
import com.campus.exam.model.vo.SubmitResultVO;
import com.campus.exam.service.support.ExamAnswerPersister;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final ExamViolationMapper violationMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final SysClassMapper classMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ExamAnswerPersister answerPersister;

    // ========================= 教师端 =========================

    @Transactional
    public Long create(ExamDTO dto) {
        Paper paper = paperMapper.selectById(dto.getPaperId());
        if (paper == null || paper.getStatus() != 1) {
            throw new BizException("只能绑定正式试卷，请先发布试卷");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        Exam exam = new Exam();
        exam.setId(dto.getId());
        exam.setExamName(dto.getExamName());
        exam.setPaperId(dto.getPaperId());
        exam.setClassId(dto.getClassId());
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        JSONObject monitor = new JSONObject();
        monitor.set("switchLimit", dto.getSwitchLimit());
        exam.setMonitorConfig(monitor.toString());
        exam.setStatus(ExamStatusEnum.NOT_STARTED.getCode());
        if (dto.getId() == null) {
            examMapper.insert(exam);
        } else {
            examMapper.updateById(exam);
        }
        return exam.getId();
    }

    @Transactional
    public void delete(Long id) {
        examMapper.deleteById(id);
    }

    public PageResult<ExamManageVO> teacherPage(long pageNum, long pageSize) {
        Page<Exam> page = examMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Exam>lambdaQuery().orderByDesc(Exam::getStartTime));
        Map<Long, String> paperNames = paperMapper.selectBatchIds(
                        page.getRecords().stream().map(Exam::getPaperId).distinct().toList())
                .stream().collect(HashMap::new, (m, p) -> m.put(p.getId(), p.getName()), HashMap::putAll);
        Map<Long, String> classNames = classMapper.selectList(null).stream()
                .collect(HashMap::new, (m, c) -> m.put(c.getId(), c.getClassName()), HashMap::putAll);
        Map<Long, Integer> studentCount = new HashMap<>();
        List<ExamManageVO> list = new ArrayList<>();
        for (Exam e : page.getRecords()) {
            ExamManageVO vo = new ExamManageVO();
            vo.setId(e.getId());
            vo.setExamName(e.getExamName());
            vo.setPaperId(e.getPaperId());
            vo.setPaperName(paperNames.get(e.getPaperId()));
            vo.setClassId(e.getClassId());
            vo.setClassName(classNames.get(e.getClassId()));
            vo.setStartTime(e.getStartTime());
            vo.setEndTime(e.getEndTime());
            vo.setStatus(e.getStatus());
            vo.setStatusDesc(resolveExamStatus(e));
            int total = studentCount.computeIfAbsent(e.getClassId(), cid ->
                    Math.toIntExact(userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getClassId, cid))));
            vo.setTotalStudents(total);
            vo.setSubmittedCount((int) recordMapper.countSubmitted(e.getId()));
            list.add(vo);
        }
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }

    private String resolveExamStatus(Exam e) {
        LocalDateTime now = LocalDateTime.now();
        if (ExamStatusEnum.PUBLISHED.getCode().equals(e.getStatus())) {
            return ExamStatusEnum.PUBLISHED.getDesc();
        }
        if (now.isBefore(e.getStartTime())) {
            return ExamStatusEnum.NOT_STARTED.getDesc();
        }
        if (now.isAfter(e.getEndTime())) {
            return ExamStatusEnum.FINISHED.getDesc();
        }
        return ExamStatusEnum.IN_PROGRESS.getDesc();
    }

    // ========================= 学生端 =========================

    public List<StudentExamVO> studentList(Long userId, Long classId) {
        List<Exam> exams = examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                .eq(classId != null, Exam::getClassId, classId)
                .orderByDesc(Exam::getStartTime));
        LocalDateTime now = LocalDateTime.now();
        List<StudentExamVO> result = new ArrayList<>();
        for (Exam e : exams) {
            Paper paper = paperMapper.selectById(e.getPaperId());
            ExamRecord record = recordMapper.selectByExamAndUser(e.getId(), userId);
            StudentExamVO vo = new StudentExamVO();
            vo.setExamId(e.getId());
            vo.setExamName(e.getExamName());
            vo.setPaperId(e.getPaperId());
            vo.setPaperName(paper == null ? null : paper.getName());
            vo.setTotalScore(paper == null ? null : paper.getTotalScore());
            vo.setSuggestDuration(paper == null ? null : paper.getSuggestDuration());
            vo.setStartTime(e.getStartTime());
            vo.setEndTime(e.getEndTime());
            vo.setServerNow(now);
            if (record != null) {
                vo.setRecordStatus(record.getStatus());
                ExamRecordStatusEnum rs = ExamRecordStatusEnum.values()[record.getStatus()];
                vo.setRecordStatusDesc(rs.getDesc());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 进入考试（含断点续考）：校验时间与名单 -> 初始化/恢复记录 -> 颁发一次性考试 Token ->
     * 返回试卷结构（剔除答案）+ Redis 快照答案 + 剩余秒数
     */
    @Transactional
    public ResumeVO enter(Long examId, Long userId) {
        Exam exam = mustExam(examId);
        SysUser user = userMapper.selectById(userId);
        if (user == null || !exam.getClassId().equals(user.getClassId())) {
            throw new BizException(ResultCode.NOT_EXAM_STUDENT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new BizException(ResultCode.EXAM_NOT_START);
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new BizException(ResultCode.EXAM_FINISHED);
        }

        ExamRecord record = recordMapper.selectByExamAndUser(examId, userId);
        if (record == null) {
            record = new ExamRecord();
            record.setExamId(examId);
            record.setUserId(userId);
            record.setStatus(ExamRecordStatusEnum.IN_PROGRESS.getCode());
            record.setStartTime(now);
            record.setViolationCount(0);
            record.setTokenVersion(1);
            record.setObjectiveScore(BigDecimal.ZERO);
            record.setSubjectiveScore(BigDecimal.ZERO);
            recordMapper.insert(record);
        } else {
            ExamRecordStatusEnum status = ExamRecordStatusEnum.values()[record.getStatus()];
            if (status.isFinal() || status == ExamRecordStatusEnum.SUBMITTED
                    || status == ExamRecordStatusEnum.AUTO_COLLECTING) {
                throw new BizException(ResultCode.EXAM_RECORD_CONFLICT, "你已完成本场考试");
            }
            // 重进：旧 Token 作废，版本号递增
            ExamRecord update = new ExamRecord();
            update.setId(record.getId());
            update.setStatus(ExamRecordStatusEnum.IN_PROGRESS.getCode());
            update.setTokenVersion((record.getTokenVersion() == null ? 1 : record.getTokenVersion()) + 1);
            recordMapper.updateById(update);
            record.setStatus(update.getStatus());
            record.setTokenVersion(update.getTokenVersion());
        }

        String token = issueExamToken(examId, userId, record.getTokenVersion(), exam.getEndTime());
        touchSession(examId, userId);
        ExamPaperVO paperVO = buildPaper(exam, record, token);

        Map<Long, String> snapshot = readSnapshot(examId, userId);
        long remain = Math.max(0, Duration.between(LocalDateTime.now(), exam.getEndTime()).getSeconds());
        return ResumeVO.builder()
                .paper(paperVO).answers(snapshot).remainSeconds(remain)
                .recordStatus(record.getStatus()).build();
    }

    /** 自动保存单题作答：HSET 增量写入 + 滑动续期会话 */
    public void autoSave(String examToken, AutoSaveDTO dto, Long userId) {
        validateExamToken(examToken, dto.getExamId(), userId);
        String key = RedisKeys.answerSnapshot(dto.getExamId(), userId);
        redisTemplate.opsForHash().put(key, String.valueOf(dto.getQuestionId()),
                dto.getContent() == null ? "" : dto.getContent());
        JSONObject meta = new JSONObject();
        meta.set("lastActive", LocalDateTime.now().toString());
        redisTemplate.opsForHash().put(key, RedisKeys.META_FIELD, meta.toString());
        redisTemplate.expire(key, RedisKeys.SESSION_TTL_SECONDS * 30, TimeUnit.SECONDS);
        touchSession(dto.getExamId(), userId);
    }

    /** 心跳：仅滑动续期，用于识别异常断线 */
    public void heartbeat(String examToken, Long examId, Long userId) {
        validateExamToken(examToken, examId, userId);
        touchSession(examId, userId);
    }

    /** 违规上报，达到监考策略阈值则服务端强制收卷 */
    @Transactional
    public void reportViolation(String examToken, ViolationDTO dto, Long userId) {
        validateExamToken(examToken, dto.getExamId(), userId);
        Exam exam = mustExam(dto.getExamId());
        ExamRecord record = recordMapper.selectByExamAndUser(dto.getExamId(), userId);
        if (record == null) {
            throw new BizException(ResultCode.EXAM_RECORD_CONFLICT);
        }
        ExamViolation violation = new ExamViolation();
        violation.setExamId(dto.getExamId());
        violation.setUserId(userId);
        violation.setType(dto.getType() == null ? "SWITCH_TAB" : dto.getType());
        violation.setDetail(dto.getDetail());
        violation.setCreateTime(LocalDateTime.now());
        violationMapper.insert(violation);

        int count = (record.getViolationCount() == null ? 0 : record.getViolationCount()) + 1;
        ExamRecord update = new ExamRecord();
        update.setId(record.getId());
        update.setViolationCount(count);
        recordMapper.updateById(update);

        Integer limit = parseSwitchLimit(exam.getMonitorConfig());
        if (limit != null && count >= limit) {
            log.warn("考生{}切屏达到上限{}，服务端强制收卷 examId={}", userId, limit, dto.getExamId());
            forceFinish(exam, record, ExamRecordStatusEnum.SUBMITTED);
            invalidateExamRuntime(exam.getId(), userId, examToken);
        }
    }

    /**
     * 主动交卷：分布式锁防并发重复提交 -> 以 Redis 快照为准判分落库 -> 清理运行时缓存
     */
    @Transactional
    public SubmitResultVO submit(String examToken, SubmitExamDTO dto, Long userId) {
        validateExamToken(examToken, dto.getExamId(), userId);
        Exam exam = mustExam(dto.getExamId());
        RLock lock = redissonClient.getLock(RedisKeys.submitLock(exam.getId(), userId));
        boolean locked;
        try {
            locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("交卷处理中断，请重试");
        }
        if (!locked) {
            throw new BizException(ResultCode.REPEAT_SUBMIT);
        }
        try {
            ExamRecord record = recordMapper.selectByExamAndUser(exam.getId(), userId);
            if (record == null) {
                throw new BizException(ResultCode.EXAM_RECORD_CONFLICT);
            }
            ExamRecordStatusEnum status = ExamRecordStatusEnum.values()[record.getStatus()];
            // 幂等：已交卷/终态直接返回已有结果
            if (status == ExamRecordStatusEnum.SUBMITTED || status.isFinal()) {
                return buildSubmitResult(record, false);
            }
            SubmitResultVO result = forceFinish(exam, record, ExamRecordStatusEnum.SUBMITTED, dto.getAnswers());
            invalidateExamRuntime(exam.getId(), userId, examToken);
            return result;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 强制/正常交卷的内部统一实现（违规收卷也走这里）
     */
    @Transactional
    public SubmitResultVO forceFinish(Exam exam, ExamRecord record, ExamRecordStatusEnum submittedStatus) {
        return forceFinish(exam, record, submittedStatus, null);
    }

    @Transactional
    public SubmitResultVO forceFinish(Exam exam, ExamRecord record, ExamRecordStatusEnum submittedStatus,
                                      Map<Long, String> requestAnswers) {
        Long userId = record.getUserId();
        Map<Long, String> snapshot = readSnapshot(exam.getId(), userId);
        if (requestAnswers != null) {
            requestAnswers.forEach(snapshot::putIfAbsent);
        }
        ExamAnswerPersister.PersistResult pr = answerPersister.persist(exam, userId, snapshot);

        ExamRecord update = new ExamRecord();
        update.setId(record.getId());
        update.setObjectiveScore(pr.objectiveScore());
        update.setSubmitTime(LocalDateTime.now());
        boolean waiting = pr.hasSubjective();
        if (waiting) {
            update.setStatus(ExamRecordStatusEnum.GRADING.getCode());
            update.setTotalScore(pr.objectiveScore());
        } else {
            update.setStatus(ExamRecordStatusEnum.GRADED.getCode());
            update.setSubjectiveScore(BigDecimal.ZERO);
            update.setTotalScore(pr.objectiveScore());
        }
        recordMapper.updateById(update);
        record.setStatus(update.getStatus());
        record.setObjectiveScore(pr.objectiveScore());
        record.setTotalScore(update.getTotalScore());
        return buildSubmitResult(record, waiting);
    }

    // ========================= 内部方法 =========================

    private SubmitResultVO buildSubmitResult(ExamRecord record, boolean waiting) {
        return SubmitResultVO.builder()
                .recordId(record.getId())
                .recordStatus(record.getStatus())
                .recordStatusDesc(ExamRecordStatusEnum.values()[record.getStatus()].getDesc())
                .objectiveScore(record.getObjectiveScore())
                .totalScore(record.getTotalScore())
                .waitingGrade(waiting)
                .build();
    }

    private void touchSession(Long examId, Long userId) {
        String key = RedisKeys.examSession(examId, userId);
        redisTemplate.opsForValue().set(key, "1", RedisKeys.SESSION_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private String issueExamToken(Long examId, Long userId, Integer version, LocalDateTime endTime) {
        String token = IdUtil.fastSimpleUUID();
        JSONObject bind = new JSONObject();
        bind.set("userId", userId);
        bind.set("examId", examId);
        bind.set("version", version);
        long ttlSeconds = Duration.between(LocalDateTime.now(), endTime).getSeconds()
                + RedisKeys.EXAM_TOKEN_GRACE_MINUTES * 60;
        redisTemplate.opsForValue().set(RedisKeys.examToken(token), bind.toString(),
                Math.max(ttlSeconds, 60), TimeUnit.SECONDS);
        return token;
    }

    /** 双重校验：Token 存在，且绑定的考生/考试与请求完全一致，防止横向越权 */
    private void validateExamToken(String token, Long examId, Long userId) {
        if (token == null || token.isBlank()) {
            throw new BizException(ResultCode.EXAM_TOKEN_INVALID);
        }
        String json = redisTemplate.opsForValue().get(RedisKeys.examToken(token));
        if (json == null) {
            throw new BizException(ResultCode.EXAM_TOKEN_INVALID, "考试凭证已失效，请重新进入考试");
        }
        JSONObject bind = JSONUtil.parseObj(json);
        Long bindUser = bind.getLong("userId");
        Long bindExam = bind.getLong("examId");
        if (!userId.equals(bindUser) || !examId.equals(bindExam)) {
            throw new BizException(ResultCode.EXAM_TOKEN_INVALID, "考试凭证与当前考试不匹配");
        }
    }

    private void invalidateExamRuntime(Long examId, Long userId, String token) {
        redisTemplate.delete(RedisKeys.answerSnapshot(examId, userId));
        redisTemplate.delete(RedisKeys.examSession(examId, userId));
        if (token != null) {
            redisTemplate.delete(RedisKeys.examToken(token));
        }
    }

    private Map<Long, String> readSnapshot(Long examId, Long userId) {
        Map<Object, Object> raw = redisTemplate.opsForHash()
                .entries(RedisKeys.answerSnapshot(examId, userId));
        Map<Long, String> result = new HashMap<>();
        raw.forEach((k, v) -> {
            if (!RedisKeys.META_FIELD.equals(k)) {
                result.put(Long.valueOf(k.toString()), v == null ? null : v.toString());
            }
        });
        return result;
    }

    private ExamPaperVO buildPaper(Exam exam, ExamRecord record, String token) {
        Paper paper = paperMapper.selectById(exam.getPaperId());
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(Wrappers.<PaperQuestion>lambdaQuery()
                .eq(PaperQuestion::getPaperId, paper.getId())
                .orderByAsc(PaperQuestion::getGroupType).orderByAsc(PaperQuestion::getSortNo));
        Map<Long, Question> qMap = questionMapper.selectBatchIds(
                        pqs.stream().map(PaperQuestion::getQuestionId).toList())
                .stream().collect(HashMap::new, (m, q) -> m.put(q.getId(), q), HashMap::putAll);

        ExamPaperVO vo = new ExamPaperVO();
        vo.setExamId(exam.getId());
        vo.setExamName(exam.getExamName());
        vo.setRecordId(record.getId());
        vo.setTotalScore(paper.getTotalScore());
        vo.setExamToken(token);
        vo.setSwitchLimit(parseSwitchLimit(exam.getMonitorConfig()));
        vo.setDeadlineMillis(exam.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        Map<Integer, ExamPaperVO.Group> groups = new HashMap<>();
        for (PaperQuestion pq : pqs) {
            Question q = qMap.get(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            ExamPaperVO.Group group = groups.computeIfAbsent(pq.getGroupType(), t -> {
                ExamPaperVO.Group g = new ExamPaperVO.Group();
                g.setQuestionType(t);
                g.setTypeDesc(QuestionTypeEnum.of(t).getDesc());
                return g;
            });
            ExamPaperVO.QuestionItem item = new ExamPaperVO.QuestionItem();
            item.setQuestionId(q.getId());
            item.setSortNo(pq.getSortNo());
            item.setScore(pq.getScore());
            item.setStem(q.getStem());
            // 关键：学生试卷严格剔除标准答案与解析
            item.setOptions(q.getOptions());
            group.getItems().add(item);
        }
        List<ExamPaperVO.Group> groupList = new ArrayList<>(groups.values());
        groupList.sort(Comparator.comparingInt(ExamPaperVO.Group::getQuestionType));
        vo.setGroups(groupList);
        return vo;
    }

    private Integer parseSwitchLimit(String monitorConfig) {
        if (monitorConfig == null || monitorConfig.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(monitorConfig).getInt("switchLimit");
        } catch (Exception e) {
            return null;
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
