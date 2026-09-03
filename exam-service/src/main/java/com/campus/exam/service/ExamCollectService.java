package com.campus.exam.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.common.constant.MQConstants;
import com.campus.exam.common.constant.RedisKeys;
import com.campus.exam.common.enums.ExamRecordStatusEnum;
import com.campus.exam.common.enums.ExamStatusEnum;
import com.campus.exam.common.enums.LocalMessageStatusEnum;
import com.campus.exam.mapper.ExamMapper;
import com.campus.exam.mapper.ExamRecordMapper;
import com.campus.exam.model.entity.Exam;
import com.campus.exam.model.entity.ExamRecord;
import com.campus.exam.model.mq.ExamMqMessage;
import com.campus.exam.service.support.ExamAnswerPersister;
import com.campus.exam.service.support.ReliableMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自动收卷：定时扫描到点考试 -> 本地事务写记录与消息表 -> MQ 异步落库。
 * 扫描生产与 MQ 消费解耦，消费逻辑幂等，支持重复投递。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamCollectService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerPersister answerPersister;
    private final ReliableMessageSender messageSender;
    private final StringRedisTemplate redisTemplate;
    private final ExamService examService;

    /**
     * 定时任务每分钟调用：
     * 1) 把到结束时间的考试置为 FINISHED；
     * 2) 对仍在答题/异常中断的考生，事务内置 AUTO_COLLECTING 并写本地消息表，提交后投递 MQ。
     */
    public void scanAndPrepare() {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> dueExams = examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                .le(Exam::getEndTime, now)
                .in(Exam::getStatus, ExamStatusEnum.NOT_STARTED.getCode(),
                        ExamStatusEnum.IN_PROGRESS.getCode()));
        for (Exam exam : dueExams) {
            finishExamAndDispatch(exam);
        }
    }

    @Transactional
    public void finishExamAndDispatch(Exam exam) {
        Exam examUpdate = new Exam();
        examUpdate.setId(exam.getId());
        examUpdate.setStatus(ExamStatusEnum.FINISHED.getCode());
        examMapper.updateById(examUpdate);

        List<ExamRecord> pending = recordMapper.selectByExamAndStatus(exam.getId(), List.of(
                ExamRecordStatusEnum.IN_PROGRESS.getCode(),
                ExamRecordStatusEnum.ABNORMAL_INTERRUPTED.getCode()));
        for (ExamRecord record : pending) {
            ExamRecord ru = new ExamRecord();
            ru.setId(record.getId());
            ru.setStatus(ExamRecordStatusEnum.AUTO_COLLECTING.getCode());
            recordMapper.updateById(ru);

            ExamMqMessage mq = ExamMqMessage.builder()
                    .bizType(MQConstants.BIZ_COLLECT)
                    .examId(exam.getId())
                    .userId(record.getUserId())
                    .occurTime(LocalDateTime.now())
                    .build();
            String msgKey = messageSender.prepare(MQConstants.BIZ_COLLECT,
                    MQConstants.EXAM_EXCHANGE, MQConstants.COLLECT_ROUTING_KEY, mq);
            log.info("考试{}考生{}收卷消息已准备 msgKey={}", exam.getId(), record.getUserId(), msgKey);
        }
    }

    /**
     * 收卷消费者入口：幂等校验 -> 快照落库判分 -> 状态推进 -> 确认本地消息。
     * 抛异常由 MQ 重试/死信机制承接。
     */
    @Transactional
    public void consumeCollect(String payload) {
        ExamMqMessage msg = JSONUtil.toBean(payload, ExamMqMessage.class);
        String msgKey = msg.getMsgKey();
        if (!acquireIdempotent(msgKey)) {
            log.info("收卷消息重复投递，直接跳过 msgKey={}", msgKey);
            return;
        }
        Exam exam = examMapper.selectById(msg.getExamId());
        ExamRecord record = recordMapper.selectByExamAndUser(msg.getExamId(), msg.getUserId());
        if (exam == null || record == null) {
            log.warn("收卷消息对应数据不存在，确认消费避免卡死 msgKey={}", msgKey);
            messageSender.markConsumed(msgKey);
            return;
        }
        ExamRecordStatusEnum status = ExamRecordStatusEnum.values()[record.getStatus()];
        if (status.isFinal() || status == ExamRecordStatusEnum.GRADING) {
            messageSender.markConsumed(msgKey);
            return;
        }
        examService.forceFinish(exam, record, ExamRecordStatusEnum.AUTO_COLLECTING);
        redisTemplate.delete(RedisKeys.examSession(exam.getId(), record.getUserId()));
        redisTemplate.delete(RedisKeys.answerSnapshot(exam.getId(), record.getUserId()));
        messageSender.markConsumed(msgKey);
        log.info("自动收卷完成 examId={} userId={}", msg.getExamId(), msg.getUserId());
    }

    /** Redis SETNX 消费幂等：首次返回 true */
    private boolean acquireIdempotent(String msgKey) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(RedisKeys.mqIdempotent(msgKey), "1",
                RedisKeys.MQ_IDEM_TTL_MINUTES, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(ok);
    }
}
