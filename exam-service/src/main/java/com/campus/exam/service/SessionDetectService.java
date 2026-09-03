package com.campus.exam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.common.constant.RedisKeys;
import com.campus.exam.common.enums.ExamRecordStatusEnum;
import com.campus.exam.common.enums.ExamStatusEnum;
import com.campus.exam.mapper.ExamMapper;
import com.campus.exam.mapper.ExamRecordMapper;
import com.campus.exam.model.entity.Exam;
import com.campus.exam.model.entity.ExamRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常断线检测：进行中的考试，若考生会话 Key 已过期（心跳/自动保存停止滑动续期），
 * 将记录置为“异常中断”，考生重新进入时可从 Redis 快照续考。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionDetectService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper recordMapper;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void detectAbnormal() {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> ongoing = examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                .lt(Exam::getStartTime, now)
                .gt(Exam::getEndTime, now)
                .in(Exam::getStatus, ExamStatusEnum.NOT_STARTED.getCode(),
                        ExamStatusEnum.IN_PROGRESS.getCode()));
        for (Exam exam : ongoing) {
            List<ExamRecord> records = recordMapper.selectByExamAndStatus(exam.getId(),
                    List.of(ExamRecordStatusEnum.IN_PROGRESS.getCode()));
            for (ExamRecord r : records) {
                Boolean alive = redisTemplate.hasKey(RedisKeys.examSession(exam.getId(), r.getUserId()));
                if (Boolean.FALSE.equals(alive)) {
                    ExamRecord update = new ExamRecord();
                    update.setId(r.getId());
                    update.setStatus(ExamRecordStatusEnum.ABNORMAL_INTERRUPTED.getCode());
                    recordMapper.updateById(update);
                    log.warn("考生{}考试{}会话超时，标记异常中断", r.getUserId(), exam.getId());
                }
            }
        }
    }
}
