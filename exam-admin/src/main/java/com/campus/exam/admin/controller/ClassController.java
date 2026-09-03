package com.campus.exam.admin.controller;

import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.ClassDTO;
import com.campus.exam.model.entity.SysClass;
import com.campus.exam.service.ClassService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "班级管理")
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping("/list")
    public Result<List<SysClass>> list() {
        return Result.ok(classService.list());
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody ClassDTO dto) {
        return Result.ok(classService.save(dto));
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        classService.delete(id);
        return Result.ok();
    }
}
