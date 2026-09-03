package com.campus.exam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.mapper.KnowledgePointMapper;
import com.campus.exam.model.dto.KnowledgeDTO;
import com.campus.exam.model.entity.KnowledgePoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgePointMapper knowledgeMapper;

    public List<KnowledgePoint> tree() {
        return knowledgeMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery().orderByAsc(KnowledgePoint::getId));
    }

    @Transactional
    public Long save(KnowledgeDTO dto) {
        KnowledgePoint point = new KnowledgePoint();
        point.setId(dto.getId());
        point.setName(dto.getName());
        point.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        if (dto.getId() == null) {
            knowledgeMapper.insert(point);
        } else {
            knowledgeMapper.updateById(point);
        }
        return point.getId();
    }

    @Transactional
    public void delete(Long id) {
        knowledgeMapper.deleteById(id);
    }
}
