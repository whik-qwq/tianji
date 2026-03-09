package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.InteractionQuestionService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final InteractionQuestionService questionService;

    @PostMapping
    public void saveQuestion(@Valid @RequestBody QuestionFormDTO questionFormDTO) {
        questionService.saveQuestion(questionFormDTO);
    }

    @GetMapping("page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
        return questionService.queryQuestionPage(query);
    }

    @GetMapping("{id}")
    public QuestionVO queryQuestionById(@PathVariable Long id){
        return questionService.queryQuestionById(id);
    }
    @PutMapping("{id}")
    public void updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionFormDTO questionFormDTO){
        questionService.updateQuestion(id,questionFormDTO);
    }
    @DeleteMapping("{id}")
    public void deleteQuestionById(@PathVariable Long id){
        questionService.deleteQuestionById(id);
    }
}
