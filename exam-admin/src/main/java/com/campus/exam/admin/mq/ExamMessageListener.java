package com.campus.exam.admin.mq;

import cn.hutool.json.JSONUtil;
import com.campus.exam.common.constant.MQConstants;
import com.campus.exam.model.mq.ExamMqMessage;
import com.campus.exam.service.ExamCollectService;
import com.campus.exam.service.GradeService;
import com.campus.exam.service.StatService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 考试领域 MQ 消费者：手动 ACK，消费失败按 msgKey 计数重试，
 * 超过最大次数 basicReject 进入死信队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamMessageListener {

    private static final String RETRY_KEY = "mq:retry:%s";

    private final ExamCollectService collectService;
    private final GradeService gradeService;
    private final StatService statService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = MQConstants.COLLECT_QUEUE)
    public void onCollect(@Payload String payload, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle("收卷", payload, channel, tag, collectService::consumeCollect);
    }

    @RabbitListener(queues = MQConstants.GRADE_QUEUE)
    public void onGrade(@Payload String payload, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle("成绩汇总", payload, channel, tag, gradeService::consumeGrade);
    }

    @RabbitListener(queues = MQConstants.STAT_QUEUE)
    public void onStat(@Payload String payload, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle("统计计算", payload, channel, tag, statService::consumeStat);
    }

    private void handle(String biz, String payload, Channel channel, long tag,
                        Consumer<String> consumer) throws IOException {
        String msgKey;
        try {
            msgKey = JSONUtil.toBean(payload, ExamMqMessage.class).getMsgKey();
        } catch (Exception parseError) {
            log.error("{}消息体无法解析，直接拒绝进死信 payload={}", biz, payload);
            channel.basicReject(tag, false);
            return;
        }
        try {
            consumer.accept(payload);
            channel.basicAck(tag, false);
            redisTemplate.delete(String.format(RETRY_KEY, msgKey));
        } catch (Exception e) {
            String key = String.format(RETRY_KEY, msgKey);
            Long retry = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
            long times = retry == null ? 1 : retry;
            if (times >= MQConstants.MAX_CONSUME_RETRY) {
                log.error("{}消息达到最大重试次数，进入死信 msgKey={}", biz, msgKey, e);
                channel.basicReject(tag, false);
            } else {
                log.warn("{}消息消费失败，第{}次重新入队 msgKey={}", biz, times, msgKey, e);
                channel.basicNack(tag, false, true);
            }
        }
    }
}
