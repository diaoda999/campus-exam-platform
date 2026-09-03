package com.campus.exam.common.constant;

/**
 * RabbitMQ 资源统一规划
 */
public final class MQConstants {

    private MQConstants() {
    }

    public static final String EXAM_EXCHANGE = "exam.exchange";
    public static final String DLX_EXCHANGE = "exam.exchange.dlx";

    /** 自动/强制收卷队列 */
    public static final String COLLECT_QUEUE = "exam.collect.queue";
    public static final String COLLECT_ROUTING_KEY = "exam.collect";

    /** 主观题批改完成、汇总成绩队列 */
    public static final String GRADE_QUEUE = "exam.grade.queue";
    public static final String GRADE_ROUTING_KEY = "exam.grade";

    /** 班级统计异步计算队列 */
    public static final String STAT_QUEUE = "exam.stat.queue";
    public static final String STAT_ROUTING_KEY = "exam.stat";

    /** 死信队列 */
    public static final String COLLECT_DLQ = "exam.collect.dlq";
    public static final String GRADE_DLQ = "exam.grade.dlq";
    public static final String STAT_DLQ = "exam.stat.dlq";

    /** 最大消费重试次数 */
    public static final int MAX_CONSUME_RETRY = 3;

    /** 业务类型：收卷 */
    public static final String BIZ_COLLECT = "COLLECT";
    /** 业务类型：成绩汇总 */
    public static final String BIZ_GRADE = "GRADE";
    /** 业务类型：统计 */
    public static final String BIZ_STAT = "STAT";
}
