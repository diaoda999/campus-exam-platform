package com.campus.exam.common.constant;

/**
 * Redis Key 统一规划，禁止在业务代码中散落字符串拼接
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 登录 JWT 黑名单/续期预留 */
    public static final String LOGIN_USER = "login:user:%d";

    /** 一次性考试 Token：exam:token:{token} -> userId:examId 绑定 JSON */
    public static final String EXAM_TOKEN = "exam:token:%s";

    /** 考生答题快照 Hash：field=questionId, value=作答内容；__meta__ 存元信息 */
    public static final String ANSWER_SNAPSHOT = "exam:%d:user:%d";

    /** 考试活跃会话，滑动过期；过期即判定异常中断 */
    public static final String EXAM_SESSION = "exam:session:%d:%d";

    /** 主观题批改临时分数 Hash */
    public static final String GRADE_TEMP = "grade:exam:%d:user:%d";

    /** 班级统计结果缓存 */
    public static final String CLASS_STAT = "stat:class:%d:exam:%d";

    /** 交卷分布式锁 */
    public static final String SUBMIT_LOCK = "lock:submit:%d:%d";

    /** MQ 消费幂等标记 */
    public static final String MQ_IDEMPOTENT = "mq:idem:%s";

    /** Hash 中保存元信息的固定 field */
    public static final String META_FIELD = "__meta__";

    /** 会话滑动过期时间（秒） */
    public static final long SESSION_TTL_SECONDS = 120L;

    /** 考试 Token 宽限时间（分钟） */
    public static final long EXAM_TOKEN_GRACE_MINUTES = 10L;

    /** 统计缓存 TTL（分钟） */
    public static final long STAT_TTL_MINUTES = 30L;

    /** MQ 幂等标记 TTL（分钟） */
    public static final long MQ_IDEM_TTL_MINUTES = 10L;

    public static String examToken(String token) {
        return String.format(EXAM_TOKEN, token);
    }

    public static String answerSnapshot(Long examId, Long userId) {
        return String.format(ANSWER_SNAPSHOT, examId, userId);
    }

    public static String examSession(Long examId, Long userId) {
        return String.format(EXAM_SESSION, examId, userId);
    }

    public static String gradeTemp(Long examId, Long userId) {
        return String.format(GRADE_TEMP, examId, userId);
    }

    public static String classStat(Long classId, Long examId) {
        return String.format(CLASS_STAT, classId, examId);
    }

    public static String submitLock(Long examId, Long userId) {
        return String.format(SUBMIT_LOCK, examId, userId);
    }

    public static String mqIdempotent(String msgKey) {
        return String.format(MQ_IDEMPOTENT, msgKey);
    }
}
