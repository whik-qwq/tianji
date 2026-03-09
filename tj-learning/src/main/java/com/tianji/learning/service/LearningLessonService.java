package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.validation.Valid;
import java.util.List;

/**
* @author xuhe8
* @description 针对表【learning_lesson(学生课程表)】的数据库操作Service
* @createDate 2026-01-27 14:50:14
*/
public interface LearningLessonService extends IService<LearningLesson> {

    void addUserLessons(Long userId, List<Long> courseIds);

    PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery);

    void deleteUserLessons(Long userId, Long courseId);

    LearningLessonVO queryMyCurrentLesson();

    Long isLessonValid(Long courseId);

    LearningLessonVO queryLessonByCourseId(Long courseId);

    Integer countLearningLessonByCourse(Long courseId);

    LearningLesson queryByUserIdAndCourseId(Long userId, Long courseId);

    void createLearningPlans(@Valid LearningPlanDTO learningPlanDTO);

    LearningPlanPageVO queryMyPlans(PageQuery pageQuery);
}
