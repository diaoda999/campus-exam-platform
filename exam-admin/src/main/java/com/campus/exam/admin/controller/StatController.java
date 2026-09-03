package com.campus.exam.admin.controller;

import com.campus.exam.common.result.Result;
import com.campus.exam.model.vo.ClassStatVO;
import com.campus.exam.service.StatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "班级成绩统计")
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/class")
    public Result<ClassStatVO> classStat(@RequestParam Long examId) {
        return Result.ok(statService.getStat(examId));
    }
}
