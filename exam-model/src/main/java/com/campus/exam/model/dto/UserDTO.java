package com.campus.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserDTO {

    private Long id;

    private String username;

    private String password;

    private String realName;

    private String role;

    private Long classId;

    private Integer status;
}
