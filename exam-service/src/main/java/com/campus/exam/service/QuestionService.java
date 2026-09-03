package com.campus.exam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exam.common.context.UserContext;
import com.campus.exam.common.enums.QuestionTypeEnum;
import com.campus.exam.common.result.PageResult;
import com.campus.exam.mapper.KnowledgePointMapper;
import com.campus.exam.mapper.QuestionKnowledgeMapper;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.model.dto.QuestionDTO;
import com.campus.exam.model.dto.QuestionQuery;
import com.campus.exam.model.entity.KnowledgePoint;
import com.campus.exam.model.entity.Question;
import com.campus.exam.model.entity.QuestionKnowledge;
import com.campus.exam.model.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper qkMapper;
    private final KnowledgePointMapper knowledgeMapper;

    @Transactional
    public Long save(QuestionDTO dto) {
        Question question = new Question();
        question.setId(dto.getId());
        question.setType(dto.getType());
        question.setStem(dto.getStem());
        question.setOptions(dto.getOptions());
        question.setAnswer(dto.getAnswer());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty());
        question.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        question.setCreatorId(UserContext.getUserId());
        if (question.getUseCount() == null) {
            question.setUseCount(0);
        }
        if (dto.getId() == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
            qkMapper.delete(Wrappers.<QuestionKnowledge>lambdaQuery()
                    .eq(QuestionKnowledge::getQuestionId, question.getId()));
        }
        if (dto.getKnowledgeIds() != null) {
            for (Long kpId : dto.getKnowledgeIds()) {
                QuestionKnowledge qk = new QuestionKnowledge();
                qk.setQuestionId(question.getId());
                qk.setKnowledgeId(kpId);
                qkMapper.insert(qk);
            }
        }
        return question.getId();
    }

    public QuestionVO detail(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            return null;
        }
        QuestionVO vo = toVO(question, Collections.emptyMap());
        vo.setKnowledgeIds(qkMapper.selectKnowledgeIds(id));
        return vo;
    }

    public PageResult<QuestionVO> page(QuestionQuery query) {
        Page<Question> page = questionMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<Question>lambdaQuery()
                        .eq(query.getType() != null, Question::getType, query.getType())
                        .eq(query.getDifficulty() != null, Question::getDifficulty, query.getDifficulty())
                        .eq(query.getStatus() != null, Question::getStatus, query.getStatus())
                        .like(query.getKeyword() != null && !query.getKeyword().isBlank(),
                                Question::getStem, query.getKeyword())
                        .orderByDesc(Question::getId));

        Map<Long, String> kpNameMap = knowledgeMapper.selectList(null).stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));

        List<QuestionVO> records = page.getRecords().stream().map(q -> {
            QuestionVO vo = toVO(q, kpNameMap);
            List<Long> kpIds = qkMapper.selectKnowledgeIds(q.getId());
            vo.setKnowledgeIds(kpIds);
            vo.setKnowledgeNames(kpIds.stream().map(kpNameMap::get).filter(java.util.Objects::nonNull).toList());
            // 知识点筛选：在内存中过滤（题量不大，避免多表 join SQL）
            return vo;
        }).filter(vo -> query.getKnowledgeId() == null
                || (vo.getKnowledgeIds() != null && vo.getKnowledgeIds().contains(query.getKnowledgeId())))
                .toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Transactional
    public void delete(Long id) {
        questionMapper.deleteById(id);
        qkMapper.delete(Wrappers.<QuestionKnowledge>lambdaQuery().eq(QuestionKnowledge::getQuestionId, id));
    }

    private QuestionVO toVO(Question q, Map<Long, String> kpNameMap) {
        QuestionVO vo = new QuestionVO();
        vo.setId(q.getId());
        vo.setType(q.getType());
        vo.setTypeDesc(QuestionTypeEnum.of(q.getType()).getDesc());
        vo.setStem(q.getStem());
        vo.setOptions(q.getOptions());
        vo.setAnswer(q.getAnswer());
        vo.setAnalysis(q.getAnalysis());
        vo.setDifficulty(q.getDifficulty());
        vo.setUseCount(q.getUseCount());
        vo.setStatus(q.getStatus());
        vo.setCreateTime(q.getCreateTime());
        return vo;
    }
}
