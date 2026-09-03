package com.campus.exam.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exam.common.enums.RoleEnum;
import com.campus.exam.common.result.PageResult;
import com.campus.exam.mapper.SysClassMapper;
import com.campus.exam.mapper.SysUserMapper;
import com.campus.exam.model.dto.UserDTO;
import com.campus.exam.model.entity.SysClass;
import com.campus.exam.model.entity.SysUser;
import com.campus.exam.model.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;

    @Transactional
    public Long create(UserDTO dto) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword() == null ? "123456" : dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole() == null ? RoleEnum.STUDENT.getCode() : dto.getRole());
        user.setClassId(dto.getClassId());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        userMapper.insert(user);
        return user.getId();
    }

    @Transactional
    public void update(UserDTO dto) {
        SysUser user = new SysUser();
        user.setId(dto.getId());
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        user.setClassId(dto.getClassId());
        user.setStatus(dto.getStatus());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(BCrypt.hashpw(dto.getPassword()));
        }
        userMapper.updateById(user);
    }

    public PageResult<UserVO> page(long pageNum, long pageSize, String role, Long classId) {
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysUser>lambdaQuery()
                        .eq(role != null, SysUser::getRole, role)
                        .eq(classId != null, SysUser::getClassId, classId)
                        .orderByAsc(SysUser::getId));
        Map<Long, String> classNameMap = classMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysClass::getId, SysClass::getClassName));
        List<UserVO> list = page.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setRole(u.getRole());
            vo.setClassId(u.getClassId());
            vo.setClassName(u.getClassId() == null ? null : classNameMap.get(u.getClassId()));
            vo.setStatus(u.getStatus());
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }

    public List<SysUser> listStudents(Long classId) {
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRole, RoleEnum.STUDENT.getCode())
                .eq(classId != null, SysUser::getClassId, classId)
                .eq(SysUser::getStatus, 1));
    }

    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
    }
}
