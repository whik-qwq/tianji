package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.InteractionQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {

    private final InteractionQuestionService questionService;

    @GetMapping("page")
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query){
        return questionService.queryQuestionPageAdmin(query);
    }

    @PutMapping("{id}/hidden/{hidden}")
    public void hiddenQuestion(@PathVariable Long id, @PathVariable Boolean hidden){
        questionService.hiddenQuestion(id,hidden);
    }

    @GetMapping("{id}")
    public QuestionAdminVO adminQueryQuestionById(@PathVariable Long id){
        return  questionService.adminQueryQuestionById(id);
    }
}
