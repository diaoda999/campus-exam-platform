package com.campus.exam.admin.controller;

import com.campus.exam.common.result.PageResult;
import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.GeneratePaperDTO;
import com.campus.exam.model.dto.PaperSaveDTO;
import com.campus.exam.model.dto.ReplaceQuestionDTO;
import com.campus.exam.model.entity.Paper;
import com.campus.exam.model.vo.GenerateResultVO;
import com.campus.exam.model.vo.PaperDetailVO;
import com.campus.exam.service.PaperService;
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

@Tag(name = "试卷与智能组卷")
@RestController
@RequestMapping("/api/paper")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @PostMapping("/auto-generate")
    public Result<GenerateResultVO> autoGenerate(@Valid @RequestBody GeneratePaperDTO dto) {
        return Result.ok(paperService.autoGenerate(dto));
    }

    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody PaperSaveDTO dto) {
        return Result.ok(paperService.saveManual(dto));
    }

    @PostMapping("/replace")
    public Result<Long> replace(@Valid @RequestBody ReplaceQuestionDTO dto) {
        return Result.ok(paperService.replaceQuestion(dto));
    }

    @PostMapping("/publish")
    public Result<Void> publish(@RequestParam Long id) {
        paperService.publish(id);
        return Result.ok();
    }

    @GetMapping("/detail")
    public Result<PaperDetailVO> detail(@RequestParam Long id) {
        return Result.ok(paperService.detail(id));
    }

    @PostMapping("/page")
    public Result<PageResult<Paper>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(paperService.page(pageNum, pageSize, status));
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        paperService.delete(id);
        return Result.ok();
    }
}
