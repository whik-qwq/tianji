package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

import javax.validation.Valid;

/**
* @author xuhe8
* @description 针对表【interaction_question(互动提问的问题表)】的数据库操作Service
* @createDate 2026-02-08 19:37:52
*/
public interface InteractionQuestionService extends IService<InteractionQuestion> {

    void saveQuestion(@Valid QuestionFormDTO questionFormDTO);

    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);

    QuestionVO queryQuestionById(Long id);

    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);

    void updateQuestion(Long id, @Valid QuestionFormDTO questionFormDTO);

    void deleteQuestionById(Long id);

    void hiddenQuestion(Long id, Boolean hidden);

    QuestionAdminVO adminQueryQuestionById(Long id);
}
