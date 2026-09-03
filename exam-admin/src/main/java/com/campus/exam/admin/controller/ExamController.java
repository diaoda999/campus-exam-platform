package com.campus.exam.admin.controller;

import com.campus.exam.common.context.UserContext;
import com.campus.exam.common.result.PageResult;
import com.campus.exam.common.result.Result;
import com.campus.exam.model.dto.AutoSaveDTO;
import com.campus.exam.model.dto.ExamDTO;
import com.campus.exam.model.dto.SubmitExamDTO;
import com.campus.exam.model.dto.ViolationDTO;
import com.campus.exam.model.vo.ExamManageVO;
import com.campus.exam.model.vo.ResumeVO;
import com.campus.exam.model.vo.StudentExamVO;
import com.campus.exam.model.vo.SubmitResultVO;
import com.campus.exam.service.ExamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "考试：进入/快照/心跳/续考/交卷")
@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // ---------------- 教师端 ----------------

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ExamDTO dto) {
        return Result.ok(examService.create(dto));
    }

    @PostMapping("/page")
    public Result<PageResult<ExamManageVO>> teacherPage(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(examService.teacherPage(pageNum, pageSize));
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        examService.delete(id);
        return Result.ok();
    }

    // ---------------- 学生端 ----------------

    @GetMapping("/student/list")
    public Result<List<StudentExamVO>> studentList() {
        var user = UserContext.get();
        return Result.ok(examService.studentList(user.getUserId(), user.getClassId()));
    }

    @GetMapping("/enter")
    public Result<ResumeVO> enter(@RequestParam Long examId) {
        return Result.ok(examService.enter(examId, UserContext.getUserId()));
    }

    @PostMapping("/autosave")
    public Result<Void> autoSave(@RequestHeader(value = "Exam-Token", required = false) String examToken,
                                 @Valid @RequestBody AutoSaveDTO dto) {
        examService.autoSave(examToken, dto, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@RequestHeader(value = "Exam-Token", required = false) String examToken,
                                  @RequestParam Long examId) {
        examService.heartbeat(examToken, examId, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/violation")
    public Result<Void> violation(@RequestHeader(value = "Exam-Token", required = false) String examToken,
                                  @Valid @RequestBody ViolationDTO dto) {
        examService.reportViolation(examToken, dto, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/submit")
    public Result<SubmitResultVO> submit(@RequestHeader(value = "Exam-Token", required = false) String examToken,
                                         @RequestBody SubmitExamDTO dto) {
        return Result.ok(examService.submit(examToken, dto, UserContext.getUserId()));
    }
}
