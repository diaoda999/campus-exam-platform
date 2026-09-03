package com.campus.exam.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统角色：轻量级单表 role 模型
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    ADMIN("ADMIN", "管理员"),
    TEACHER("TEACHER", "教师"),
    STUDENT("STUDENT", "学生");

    private final String code;
    private final String desc;
}
