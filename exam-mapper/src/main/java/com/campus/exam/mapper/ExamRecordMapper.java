package com.campus.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.exam.model.entity.ExamRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    @Select("SELECT * FROM exam_record WHERE exam_id = #{examId} AND user_id = #{userId} LIMIT 1")
    ExamRecord selectByExamAndUser(@Param("examId") Long examId, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT * FROM exam_record
            WHERE exam_id = #{examId}
            AND status IN
            <foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>
            </script>
            """)
    List<ExamRecord> selectByExamAndStatus(@Param("examId") Long examId, @Param("statuses") List<Integer> statuses);

    @Select("SELECT COUNT(1) FROM exam_record WHERE exam_id = #{examId} AND status IN (2,4,5,6,7)")
    long countSubmitted(@Param("examId") Long examId);
}
