package com.campus.exam.admin.controller;

import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.LoginDTO;
import com.campus.exam.model.vo.LoginVO;
import com.campus.exam.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号密码登录，返回 JWT")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }
}
