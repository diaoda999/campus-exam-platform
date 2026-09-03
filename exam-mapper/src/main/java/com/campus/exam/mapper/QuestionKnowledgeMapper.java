package com.campus.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.exam.model.entity.QuestionKnowledge;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionKnowledgeMapper extends BaseMapper<QuestionKnowledge> {

    @Select("SELECT knowledge_id FROM question_knowledge WHERE question_id = #{questionId}")
    List<Long> selectKnowledgeIds(@Param("questionId") Long questionId);

    @Select("""
            <script>
            SELECT question_id FROM question_knowledge WHERE knowledge_id IN
            <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<Long> selectQuestionIdsByKnowledge(@Param("ids") List<Long> knowledgeIds);
}
