package com.campus.exam.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.mapper.SysClassMapper;
import com.campus.exam.model.dto.ClassDTO;
import com.campus.exam.model.entity.SysClass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final SysClassMapper classMapper;

    public List<SysClass> list() {
        return classMapper.selectList(Wrappers.<SysClass>lambdaQuery().orderByAsc(SysClass::getId));
    }

    @Transactional
    public Long save(ClassDTO dto) {
        SysClass clazz = new SysClass();
        clazz.setId(dto.getId());
        clazz.setClassName(dto.getClassName());
        if (dto.getId() == null) {
            classMapper.insert(clazz);
        } else {
            classMapper.updateById(clazz);
        }
        return clazz.getId();
    }

    @Transactional
    public void delete(Long id) {
        classMapper.deleteById(id);
    }
}
