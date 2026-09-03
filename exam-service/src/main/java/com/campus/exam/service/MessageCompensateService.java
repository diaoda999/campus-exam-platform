package com.campus.exam.service;

import com.campus.exam.common.constant.MQConstants;
import com.campus.exam.mapper.LocalMessageMapper;
import com.campus.exam.model.entity.LocalMessage;
import com.campus.exam.service.support.ReliableMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表补偿：
 * PENDING（事务提交后发送失败）或 SENT 后长时间未确认（消费端宕机/丢消息）都会被重新投递，
 * 超过最大发送次数置 DEAD，对应死信告警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCompensateService {

    /** 投递侧最大重试（区别于消费侧重试） */
    private static final int MAX_SEND_RETRY = 5;
    private static final int BATCH = 100;

    private final LocalMessageMapper messageMapper;
    private final ReliableMessageSender sender;

    public void compensate() {
        List<LocalMessage> messages = messageMapper.selectCompensable(LocalDateTime.now(), MAX_SEND_RETRY, BATCH);
        for (LocalMessage msg : messages) {
            int retry = (msg.getRetryCount() == null ? 0 : msg.getRetryCount()) + 1;
            if (retry > MAX_SEND_RETRY) {
                sender.markDead(msg.getId());
                log.error("消息超过最大投递次数，标记 DEAD，需人工介入 msgKey={}", msg.getMsgKey());
                continue;
            }
            LocalMessage update = new LocalMessage();
            update.setId(msg.getId());
            update.setRetryCount(retry);
            // 线性退避：30s * retry
            update.setNextRetryTime(LocalDateTime.now().plusSeconds(30L * retry));
            messageMapper.updateById(update);
            sender.doSend(msg);
        }
        if (!messages.isEmpty()) {
            log.info("消息补偿扫描完成，本轮处理 {} 条", messages.size());
        }
    }
}
