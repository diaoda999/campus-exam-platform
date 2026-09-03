package com.campus.exam.model.vo;

import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private Long classId;
    private String className;
    private Integer status;
}
