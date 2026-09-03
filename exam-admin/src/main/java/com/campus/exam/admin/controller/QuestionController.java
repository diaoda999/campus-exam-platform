package com.campus.exam.admin.controller;

import com.campus.exam.common.result.PageResult;
import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.QuestionDTO;
import com.campus.exam.model.dto.QuestionQuery;
import com.campus.exam.model.vo.QuestionVO;
import com.campus.exam.service.QuestionService;
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

@Tag(name = "题库管理（五种题型）")
@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/page")
    public Result<PageResult<QuestionVO>> page(@RequestBody QuestionQuery query) {
        return Result.ok(questionService.page(query));
    }

    @GetMapping("/detail")
    public Result<QuestionVO> detail(@RequestParam Long id) {
        return Result.ok(questionService.detail(id));
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody QuestionDTO dto) {
        return Result.ok(questionService.save(dto));
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        questionService.delete(id);
        return Result.ok();
    }
}
