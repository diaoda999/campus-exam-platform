package com.campus.exam.service;

import cn.hutool.crypto.digest.BCrypt;
import com.campus.exam.common.exception.BizException;
import com.campus.exam.common.result.ResultCode;
import com.campus.exam.common.utils.JwtUtils;
import com.campus.exam.mapper.SysUserMapper;
import com.campus.exam.model.dto.LoginDTO;
import com.campus.exam.model.entity.SysUser;
import com.campus.exam.model.vo.LoginVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;

    @Value("${exam.jwt.secret:" + JwtUtils.DEFAULT_SECRET + "}")
    private String jwtSecret;

    @Value("${exam.jwt.ttl-millis:28800000}")
    private Long ttlMillis;

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        // data.sql 中演示账号初始为明文，由 DataInitializer 启动时加密；此处两种形态都兼容
        boolean ok = user.getPassword().startsWith("$2")
                ? BCrypt.checkpw(dto.getPassword(), user.getPassword())
                : dto.getPassword().equals(user.getPassword());
        if (!ok) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = JwtUtils.generate(user.getId(), user.getUsername(), user.getRole(), jwtSecret, ttlMillis);
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .classId(user.getClassId())
                .build();
    }
}
