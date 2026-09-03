package com.campus.exam.admin.controller;

import com.campus.exam.common.result.PageResult;
import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.UserDTO;
import com.campus.exam.model.vo.UserVO;
import com.campus.exam.service.SysUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Long classId) {
        return Result.ok(userService.page(pageNum, pageSize, role, classId));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserDTO dto) {
        return Result.ok(userService.create(dto));
    }

    @PutMapping
    public Result<Void> update(@RequestBody UserDTO dto) {
        userService.update(dto);
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
