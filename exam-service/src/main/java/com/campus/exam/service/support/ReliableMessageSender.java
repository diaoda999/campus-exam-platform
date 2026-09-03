package com.campus.exam.service.support;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.campus.exam.common.constant.MQConstants;
import com.campus.exam.common.enums.LocalMessageStatusEnum;
import com.campus.exam.mapper.LocalMessageMapper;
import com.campus.exam.model.entity.LocalMessage;
import com.campus.exam.model.mq.ExamMqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 本地消息表可靠投递：
 * 业务事务内先把消息落库（PENDING），事务提交后再发 MQ；
 * 发送失败保持 PENDING，由补偿任务重投，从而保证“业务库变更”和“消息发出”的最终一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReliableMessageSender {

    private final LocalMessageMapper messageMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 在业务事务内调用。返回 msgKey（同时回填到 ExamMqMessage，作为消费幂等键）。
     * payload 可传字符串或可 JSON 序列化的消息对象。
     */
    public String prepare(String bizType, String exchange, String routingKey, Object payloadObj) {
        String msgKey = bizType + ":" + IdUtil.fastSimpleUUID();
        if (payloadObj instanceof ExamMqMessage mqMessage) {
            mqMessage.setMsgKey(msgKey);
        }
        String payload = payloadObj instanceof String s ? s : JSONUtil.toJsonStr(payloadObj);
        LocalMessage message = new LocalMessage();
        message.setMsgKey(msgKey);
        message.setBizType(bizType);
        message.setExchange(exchange);
        message.setRoutingKey(routingKey);
        message.setPayload(payload);
        message.setStatus(LocalMessageStatusEnum.PENDING.getCode());
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        messageMapper.insert(message);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(message);
                }
            });
        } else {
            doSend(message);
        }
        return message.getMsgKey();
    }

    /** 实际投递（补偿任务也复用），失败只记录不抛出，等待下轮补偿 */
    public void doSend(LocalMessage message) {
        try {
            rabbitTemplate.convertAndSend(message.getExchange(), message.getRoutingKey(), message.getPayload());
            markStatus(message.getId(), LocalMessageStatusEnum.SENT.getCode());
            log.info("消息投递成功 msgKey={}, routingKey={}", message.getMsgKey(), message.getRoutingKey());
        } catch (Exception e) {
            log.error("消息投递失败，等待补偿重投 msgKey={}", message.getMsgKey(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStatus(Long id, String status) {
        LocalMessage update = new LocalMessage();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        messageMapper.updateById(update);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markConsumed(String msgKey) {
        LocalMessage db = messageMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<LocalMessage>lambdaQuery().eq(LocalMessage::getMsgKey, msgKey));
        if (db == null) {
            log.warn("消费确认时未找到本地消息 msgKey={}", msgKey);
            return;
        }
        LocalMessage update = new LocalMessage();
        update.setId(db.getId());
        update.setStatus(LocalMessageStatusEnum.CONSUMED.getCode());
        update.setUpdateTime(LocalDateTime.now());
        messageMapper.updateById(update);
    }

    /** 超过最大重试次数，置 DEAD（对应死信） */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDead(Long id) {
        LocalMessage update = new LocalMessage();
        update.setId(id);
        update.setStatus(LocalMessageStatusEnum.DEAD.getCode());
        update.setUpdateTime(LocalDateTime.now());
        messageMapper.updateById(update);
    }

    public String maxRetry() {
        return String.valueOf(MQConstants.MAX_CONSUME_RETRY);
    }
}
