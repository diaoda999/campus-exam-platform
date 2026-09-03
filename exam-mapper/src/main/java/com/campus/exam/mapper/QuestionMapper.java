package com.campus.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.exam.model.entity.Question;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 按题型集合一次性捞取组卷候选池（status 正常），避免组卷过程中循环查库
     */
    @Select("""
            <script>
            SELECT * FROM question
            WHERE deleted = 0 AND status = 1 AND type IN
            <foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach>
            </script>
            """)
    List<Question> selectCandidates(@Param("types") List<Integer> types);
}
