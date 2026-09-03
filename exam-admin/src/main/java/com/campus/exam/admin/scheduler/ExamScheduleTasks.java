package com.campus.exam.admin.scheduler;

import com.campus.exam.service.ExamCollectService;
import com.campus.exam.service.MessageCompensateService;
import com.campus.exam.service.SessionDetectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 平台定时任务：
 * - 每分钟扫描到点考试，异步自动收卷；
 * - 每 30s 补偿未确认的本地消息；
 * - 每 30s 检测异常断线的考生会话。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamScheduleTasks {

    private final ExamCollectService examCollectService;
    private final MessageCompensateService compensateService;
    private final SessionDetectService sessionDetectService;

    @Scheduled(cron = "0 * * * * ?")
    public void autoCollect() {
        try {
            examCollectService.scanAndPrepare();
        } catch (Exception e) {
            log.error("自动收卷扫描异常", e);
        }
    }

    @Scheduled(fixedDelay = 30_000L, initialDelay = 10_000L)
    public void compensateMessages() {
        try {
            compensateService.compensate();
        } catch (Exception e) {
            log.error("消息补偿扫描异常", e);
        }
    }

    @Scheduled(fixedDelay = 30_000L, initialDelay = 20_000L)
    public void detectAbnormalSession() {
        try {
            sessionDetectService.detectAbnormal();
        } catch (Exception e) {
            log.error("异常会话检测异常", e);
        }
    }
}
