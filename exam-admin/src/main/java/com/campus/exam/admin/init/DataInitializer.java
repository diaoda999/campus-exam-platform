package com.campus.exam.admin.init;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.exam.mapper.SysUserMapper;
import com.campus.exam.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 演示数据兼容：sql/data.sql 中初始密码为明文，启动时统一升级为 BCrypt 存储。
 * 生产环境用户均由 SysUserService 创建（本就是 BCrypt），此 Runner 不会重复处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;

    @Override
    public void run(String... args) {
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .notLikeRight(SysUser::getPassword, "$2"));
        for (SysUser u : users) {
            SysUser update = new SysUser();
            update.setId(u.getId());
            update.setPassword(BCrypt.hashpw(u.getPassword()));
            userMapper.updateById(update);
        }
        if (!users.isEmpty()) {
            log.info("初始化完成：{} 个演示账号密码已升级为 BCrypt", users.size());
        }
    }
}
