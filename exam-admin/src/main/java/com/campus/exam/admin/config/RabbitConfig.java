package com.campus.exam.admin.config;

import com.campus.exam.common.constant.MQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 拓扑：业务 exchange + 三个业务队列（绑定死信交换机）+ 三个死信队列
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange examExchange() {
        return new TopicExchange(MQConstants.EXAM_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(MQConstants.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue collectQueue() {
        return businessQueue(MQConstants.COLLECT_QUEUE, "exam.collect.dlq");
    }

    @Bean
    public Queue gradeQueue() {
        return businessQueue(MQConstants.GRADE_QUEUE, "exam.grade.dlq");
    }

    @Bean
    public Queue statQueue() {
        return businessQueue(MQConstants.STAT_QUEUE, "exam.stat.dlq");
    }

    @Bean
    public Queue collectDlq() {
        return new Queue(MQConstants.COLLECT_DLQ, true);
    }

    @Bean
    public Queue gradeDlq() {
        return new Queue(MQConstants.GRADE_DLQ, true);
    }

    @Bean
    public Queue statDlq() {
        return new Queue(MQConstants.STAT_DLQ, true);
    }

    @Bean
    public Binding bindCollect() {
        return BindingBuilder.bind(collectQueue()).to(examExchange()).with(MQConstants.COLLECT_ROUTING_KEY);
    }

    @Bean
    public Binding bindGrade() {
        return BindingBuilder.bind(gradeQueue()).to(examExchange()).with(MQConstants.GRADE_ROUTING_KEY);
    }

    @Bean
    public Binding bindStat() {
        return BindingBuilder.bind(statQueue()).to(examExchange()).with(MQConstants.STAT_ROUTING_KEY);
    }

    @Bean
    public Binding bindCollectDlq() {
        return BindingBuilder.bind(collectDlq()).to(dlxExchange()).with("exam.collect.dlq");
    }

    @Bean
    public Binding bindGradeDlq() {
        return BindingBuilder.bind(gradeDlq()).to(dlxExchange()).with("exam.grade.dlq");
    }

    @Bean
    public Binding bindStatDlq() {
        return BindingBuilder.bind(statDlq()).to(dlxExchange()).with("exam.stat.dlq");
    }

    private Queue businessQueue(String name, String dlxRoutingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstants.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", dlxRoutingKey);
        return QueueBuilder.durable(name).withArguments(args).build();
    }
}
