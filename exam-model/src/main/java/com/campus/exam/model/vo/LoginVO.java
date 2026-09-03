package com.campus.exam.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Long classId;
}
