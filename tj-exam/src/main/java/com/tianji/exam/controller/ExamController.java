package com.tianji.exam.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.exam.domain.dto.ExamRecordDTO;
import com.tianji.exam.domain.dto.ExamSubmitDTO;
import com.tianji.exam.domain.vo.ExamQuestionVO;
import com.tianji.exam.domain.vo.ExamRecordDetailVO;
import com.tianji.exam.domain.vo.ExamRecordVO;
import com.tianji.exam.service.IExamRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "考试评测相关接口")
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {
    private final IExamRecordService examService;

    @ApiOperation("获取试题并开始考试")
    @PostMapping
    public ExamQuestionVO startExam(@RequestBody ExamRecordDTO dto) {
        return examService.startExam(dto);
    }

    @PostMapping("/details")
    @ApiOperation("提交考试结果")
    public void submitExam(@RequestBody ExamSubmitDTO dto) {
        examService.submitExam(dto);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询考试记录")
    public PageDTO<ExamRecordVO> getExamRecordPage(PageQuery query) {
        return examService.getExamRecordPage(query);
    }
    @GetMapping("/{id}")
    @ApiOperation("询考试记录详情")
    public List<ExamRecordDetailVO> getExamRecordById(@PathVariable String id) {
        return examService.getExamRecordById(id);
    }
}