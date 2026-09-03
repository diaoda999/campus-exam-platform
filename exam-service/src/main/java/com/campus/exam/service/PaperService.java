package com.campus.exam.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exam.common.context.UserContext;
import com.campus.exam.common.enums.PaperGenTypeEnum;
import com.campus.exam.common.enums.PaperStatusEnum;
import com.campus.exam.common.enums.QuestionTypeEnum;
import com.campus.exam.common.exception.BizException;
import com.campus.exam.common.result.PageResult;
import com.campus.exam.common.result.ResultCode;
import com.campus.exam.mapper.PaperMapper;
import com.campus.exam.mapper.PaperQuestionMapper;
import com.campus.exam.mapper.QuestionKnowledgeMapper;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.model.dto.GeneratePaperDTO;
import com.campus.exam.model.dto.PaperSaveDTO;
import com.campus.exam.model.dto.ReplaceQuestionDTO;
import com.campus.exam.model.entity.Paper;
import com.campus.exam.model.entity.PaperQuestion;
import com.campus.exam.model.entity.Question;
import com.campus.exam.model.vo.GenerateResultVO;
import com.campus.exam.model.vo.PaperDetailVO;
import com.campus.exam.service.support.CandidateQuestion;
import com.campus.exam.service.support.PaperGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper qkMapper;

    /**
     * 智能组卷：一次 SQL 捞候选池 -> 内存约束求解 -> 落草稿试卷
     */
    @Transactional
    public GenerateResultVO autoGenerate(GeneratePaperDTO dto) {
        List<Integer> types = dto.getGroups().stream().map(GeneratePaperDTO.GroupSpec::getQuestionType).distinct().toList();
        List<Question> questions = questionMapper.selectCandidates(types);
        if (questions.isEmpty()) {
            throw new BizException(ResultCode.QUESTION_NOT_ENOUGH);
        }
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        List<CandidateQuestion> pool = questions.stream().map(q -> {
            CandidateQuestion c = new CandidateQuestion();
            c.setQuestionId(q.getId());
            c.setType(q.getType());
            c.setDifficulty(q.getDifficulty());
            c.setUseCount(q.getUseCount());
            c.setKnowledgeIds(new HashSet<>(qkMapper.selectKnowledgeIds(q.getId())));
            return c;
        }).toList();

        List<PaperGenerator.GroupReq> groupReqs = dto.getGroups().stream()
                .map(g -> PaperGenerator.GroupReq.builder()
                        .questionType(g.getQuestionType()).count(g.getCount()).build())
                .toList();
        Map<Long, Integer> coverage = new HashMap<>();
        if (dto.getCoverage() != null) {
            dto.getCoverage().forEach(c -> coverage.put(c.getKnowledgeId(), c.getMinCount()));
        }

        PaperGenerator.GenResult result = PaperGenerator.generate(
                pool, groupReqs, dto.getTargetDifficulty(),
                dto.getDifficultyTolerance() == null ? 0.3d : dto.getDifficultyTolerance(),
                coverage, 200);

        // 落库草稿试卷
        Paper paper = new Paper();
        paper.setName(dto.getName() == null ? "智能组卷-" + System.currentTimeMillis() : dto.getName());
        paper.setSuggestDuration(dto.getSuggestDuration());
        paper.setGenType(PaperGenTypeEnum.AUTO.getCode());
        paper.setStatus(PaperStatusEnum.DRAFT.getCode());
        paper.setConstraintSnapshot(JSONUtil.toJsonStr(dto));
        paper.setCreatorId(UserContext.getUserId());
        int totalScore = 0;
        paperMapper.insert(paper);

        Map<Integer, Integer> scorePerMap = dto.getGroups().stream()
                .collect(Collectors.toMap(GeneratePaperDTO.GroupSpec::getQuestionType,
                        GeneratePaperDTO.GroupSpec::getScorePer));
        Map<Integer, Integer> sortCounter = new HashMap<>();
        for (Long qid : result.getQuestionIds()) {
            Question q = questionMap.get(qid);
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(qid);
            pq.setGroupType(q.getType());
            pq.setSortNo(sortCounter.merge(q.getType(), 1, Integer::sum));
            pq.setScore(scorePerMap.get(q.getType()));
            paperQuestionMapper.insert(pq);
            totalScore += pq.getScore();
            // 引用次数 +1，服务于后续组卷的均衡策略
            Question update = new Question();
            update.setId(qid);
            update.setUseCount((q.getUseCount() == null ? 0 : q.getUseCount()) + 1);
            questionMapper.updateById(update);
        }
        paper.setTotalScore(totalScore);
        paperMapper.updateById(paper);

        return GenerateResultVO.builder()
                .paperId(paper.getId())
                .totalScore(totalScore)
                .avgDifficulty(result.getAvgDifficulty())
                .success(result.isSuccess())
                .unmetConstraints(result.getUnmetConstraints())
                .build();
    }

    /**
     * 手动组卷 / 草稿微调后保存
     */
    @Transactional
    public Long saveManual(PaperSaveDTO dto) {
        Paper paper;
        if (dto.getId() == null) {
            paper = new Paper();
            paper.setGenType(PaperGenTypeEnum.MANUAL.getCode());
            paper.setCreatorId(UserContext.getUserId());
            paperMapper.insert(paper);
        } else {
            paper = paperMapper.selectById(dto.getId());
            if (paper == null) {
                throw new BizException(ResultCode.NOT_FOUND, "试卷不存在");
            }
            paperQuestionMapper.delete(Wrappers.<PaperQuestion>lambdaQuery()
                    .eq(PaperQuestion::getPaperId, paper.getId()));
        }
        paper.setName(dto.getName());
        paper.setSuggestDuration(dto.getSuggestDuration());
        paper.setStatus(Boolean.TRUE.equals(dto.getPublish())
                ? PaperStatusEnum.OFFICIAL.getCode() : PaperStatusEnum.DRAFT.getCode());
        int total = rebuildItems(paper.getId(), dto.getItems());
        paper.setTotalScore(total);
        paperMapper.updateById(paper);
        return paper.getId();
    }

    private int rebuildItems(Long paperId, List<PaperSaveDTO.PaperItem> items) {
        Map<Long, Question> qMap = questionMapper.selectBatchIds(
                items.stream().map(PaperSaveDTO.PaperItem::getQuestionId).toList())
                .stream().collect(Collectors.toMap(Question::getId, q -> q));
        Map<Integer, Integer> sortCounter = new HashMap<>();
        int total = 0;
        for (PaperSaveDTO.PaperItem item : items) {
            Question q = qMap.get(item.getQuestionId());
            if (q == null) {
                continue;
            }
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paperId);
            pq.setQuestionId(item.getQuestionId());
            pq.setGroupType(q.getType());
            pq.setSortNo(sortCounter.merge(q.getType(), 1, Integer::sum));
            pq.setScore(item.getScore());
            paperQuestionMapper.insert(pq);
            total += item.getScore();
        }
        return total;
    }

    /**
     * 草稿微调：替换单题。newQuestionId 为空时自动推荐同题型、难度最接近、知识点不重复的候选题
     */
    @Transactional
    public Long replaceQuestion(ReplaceQuestionDTO dto) {
        Paper paper = paperMapper.selectById(dto.getPaperId());
        if (paper == null || !PaperStatusEnum.DRAFT.getCode().equals(paper.getStatus())) {
            throw new BizException(ResultCode.PAPER_NOT_DRAFT);
        }
        PaperQuestion old = paperQuestionMapper.selectOne(Wrappers.<PaperQuestion>lambdaQuery()
                .eq(PaperQuestion::getPaperId, paper.getId())
                .eq(PaperQuestion::getQuestionId, dto.getOldQuestionId()));
        if (old == null) {
            throw new BizException(ResultCode.NOT_FOUND, "试卷中不存在该题");
        }
        Question oldQuestion = questionMapper.selectById(dto.getOldQuestionId());
        Set<Long> existed = paperQuestionMapper.selectList(Wrappers.<PaperQuestion>lambdaQuery()
                        .eq(PaperQuestion::getPaperId, paper.getId())).stream()
                .map(PaperQuestion::getQuestionId).collect(Collectors.toSet());

        Long newId = dto.getNewQuestionId();
        if (newId == null) {
            Question recommend = questionMapper.selectCandidates(List.of(oldQuestion.getType())).stream()
                    .filter(q -> !existed.contains(q.getId()))
                    .min(Comparator.comparingInt(q -> Math.abs(q.getDifficulty() - oldQuestion.getDifficulty())))
                    .orElseThrow(() -> new BizException(ResultCode.QUESTION_NOT_ENOUGH, "没有可替换的候选题"));
            newId = recommend.getId();
        } else if (existed.contains(newId)) {
            throw new BizException("替换题已在试卷中");
        }
        old.setQuestionId(newId);
        paperQuestionMapper.updateById(old);
        return newId;
    }

    @Transactional
    public void publish(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BizException(ResultCode.NOT_FOUND, "试卷不存在");
        }
        Long count = paperQuestionMapper.selectCount(Wrappers.<PaperQuestion>lambdaQuery()
                .eq(PaperQuestion::getPaperId, id));
        if (count == 0) {
            throw new BizException("空试卷不能发布");
        }
        Paper update = new Paper();
        update.setId(id);
        update.setStatus(PaperStatusEnum.OFFICIAL.getCode());
        paperMapper.updateById(update);
    }

    public PaperDetailVO detail(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BizException(ResultCode.NOT_FOUND, "试卷不存在");
        }
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(Wrappers.<PaperQuestion>lambdaQuery()
                .eq(PaperQuestion::getPaperId, id).orderByAsc(PaperQuestion::getGroupType)
                .orderByAsc(PaperQuestion::getSortNo));
        Map<Long, Question> qMap = questionMapper.selectBatchIds(
                pqs.stream().map(PaperQuestion::getQuestionId).toList())
                .stream().collect(Collectors.toMap(Question::getId, q -> q));

        PaperDetailVO vo = new PaperDetailVO();
        vo.setId(paper.getId());
        vo.setName(paper.getName());
        vo.setTotalScore(paper.getTotalScore());
        vo.setSuggestDuration(paper.getSuggestDuration());
        vo.setGenType(paper.getGenType());
        vo.setStatus(paper.getStatus());
        vo.setConstraintSnapshot(paper.getConstraintSnapshot());

        Map<Integer, PaperDetailVO.Group> groupMap = new HashMap<>();
        for (PaperQuestion pq : pqs) {
            Question q = qMap.get(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            PaperDetailVO.Group group = groupMap.computeIfAbsent(pq.getGroupType(), t -> {
                PaperDetailVO.Group g = new PaperDetailVO.Group();
                g.setQuestionType(t);
                g.setTypeDesc(QuestionTypeEnum.of(t).getDesc());
                return g;
            });
            PaperDetailVO.PaperQuestionItem item = new PaperDetailVO.PaperQuestionItem();
            item.setPaperQuestionId(pq.getId());
            item.setQuestionId(q.getId());
            item.setSortNo(pq.getSortNo());
            item.setScore(pq.getScore());
            item.setStem(q.getStem());
            item.setOptions(q.getOptions());
            item.setAnswer(q.getAnswer());
            item.setAnalysis(q.getAnalysis());
            item.setDifficulty(q.getDifficulty());
            group.getItems().add(item);
        }
        vo.setGroups(new ArrayList<>(groupMap.values()));
        vo.getGroups().sort(Comparator.comparingInt(PaperDetailVO.Group::getQuestionType));
        return vo;
    }

    public PageResult<Paper> page(long pageNum, long pageSize, Integer status) {
        Page<Paper> page = paperMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Paper>lambdaQuery()
                        .eq(status != null, Paper::getStatus, status)
                        .orderByDesc(Paper::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Transactional
    public void delete(Long id) {
        paperMapper.deleteById(id);
        paperQuestionMapper.delete(Wrappers.<PaperQuestion>lambdaQuery().eq(PaperQuestion::getPaperId, id));
    }
}
