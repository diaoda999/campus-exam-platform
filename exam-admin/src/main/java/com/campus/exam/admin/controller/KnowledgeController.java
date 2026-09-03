package com.campus.exam.admin.controller;

import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.KnowledgeDTO;
import com.campus.exam.model.entity.KnowledgePoint;
import com.campus.exam.service.KnowledgeService;
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

@Tag(name = "知识点")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/tree")
    public Result<List<KnowledgePoint>> tree() {
        return Result.ok(knowledgeService.tree());
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody KnowledgeDTO dto) {
        return Result.ok(knowledgeService.save(dto));
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }
}
