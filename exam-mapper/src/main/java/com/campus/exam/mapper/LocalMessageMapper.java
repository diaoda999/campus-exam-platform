package com.campus.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.exam.model.entity.LocalMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface LocalMessageMapper extends BaseMapper<LocalMessage> {

    /**
     * 补偿扫描：PENDING，或 SENT 后长时间未被确认消费（消费者宕机/丢消息兜底）
     */
    @Select("""
            SELECT * FROM local_message
            WHERE status IN ('PENDING','SENT')
              AND retry_count < #{maxRetry}
              AND (next_retry_time IS NULL OR next_retry_time <= #{now})
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<LocalMessage> selectCompensable(@Param("now") LocalDateTime now,
                                         @Param("maxRetry") int maxRetry,
                                         @Param("limit") int limit);
}
