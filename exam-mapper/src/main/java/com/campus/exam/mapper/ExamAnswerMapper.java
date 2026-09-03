package com.campus.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.exam.model.entity.ExamAnswer;
import com.campus.exam.model.vo.GradeDetailVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {

    /**
     * 考生整卷作答 + 题目信息 + 单题满分（来自试卷），供教师批改详情使用
     */
    @Select("""
            SELECT a.question_id        AS questionId,
                   q.type               AS type,
                   q.stem               AS stem,
                   q.options            AS options,
                   q.answer             AS standardAnswer,
                   a.content            AS content,
                   pq.score             AS fullScore,
                   a.score              AS score,
                   a.comment            AS comment
            FROM exam_answer a
            JOIN question q        ON q.id = a.question_id
            JOIN exam e            ON e.id = a.exam_id
            JOIN paper_question pq ON pq.paper_id = e.paper_id AND pq.question_id = a.question_id
            WHERE a.exam_id = #{examId} AND a.user_id = #{userId}
            ORDER BY q.type, pq.sort_no
            """)
    List<GradeDetailVO.AnswerItem> selectGradeItems(@Param("examId") Long examId, @Param("userId") Long userId);

    @Select("SELECT * FROM exam_answer WHERE exam_id = #{examId} AND user_id = #{userId}")
    List<ExamAnswer> selectByExamAndUser(@Param("examId") Long examId, @Param("userId") Long userId);

    /**
     * 每题平均得分率（得分/满分），用于班级统计看板
     */
    @Select("""
            SELECT a.question_id AS questionId, AVG(a.score / pq.score) AS rate
            FROM exam_answer a
            JOIN exam e ON e.id = a.exam_id
            JOIN paper_question pq ON pq.paper_id = e.paper_id AND pq.question_id = a.question_id
            WHERE a.exam_id = #{examId} AND a.score IS NOT NULL
            GROUP BY a.question_id
            """)
    List<java.util.Map<String, Object>> selectQuestionRates(@Param("examId") Long examId);
}
