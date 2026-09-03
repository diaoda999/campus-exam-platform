package com.campus.exam.admin.controller;

import com.campus.exam.common.context.UserContext;
import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.GradeFinishDTO;
import com.campus.exam.model.dto.GradeScoreDTO;
import com.campus.exam.model.entity.ExamRecord;
import com.campus.exam.model.vo.GradeDetailVO;
import com.campus.exam.model.vo.GradeTodoVO;
import com.campus.exam.service.GradeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "阅卷批改与成绩发布")
@RestController
@RequestMapping("/api/grade")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/todo")
    public Result<List<GradeTodoVO>> todo(@RequestParam Long examId) {
        return Result.ok(gradeService.todoList(examId));
    }

    @GetMapping("/detail")
    public Result<GradeDetailVO> detail(@RequestParam Long examId, @RequestParam Long userId) {
        return Result.ok(gradeService.detail(examId, userId));
    }

    @PostMapping("/score")
    public Result<Void> score(@Valid @RequestBody GradeScoreDTO dto) {
        gradeService.score(dto);
        return Result.ok();
    }

    @PostMapping("/finish")
    public Result<Void> finish(@Valid @RequestBody GradeFinishDTO dto) {
        gradeService.finish(dto);
        return Result.ok();
    }

    @PostMapping("/publish")
    public Result<Void> publish(@RequestParam Long examId) {
        gradeService.publish(examId);
        return Result.ok();
    }

    @GetMapping("/my-result")
    public Result<ExamRecord> myResult(@RequestParam Long examId) {
        return Result.ok(gradeService.myResult(examId, UserContext.getUserId()));
    }
}
