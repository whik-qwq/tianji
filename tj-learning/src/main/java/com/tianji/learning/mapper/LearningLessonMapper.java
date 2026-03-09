package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author xuhe8
* @description 针对表【learning_lesson(学生课程表)】的数据库操作Mapper
* @createDate 2026-01-27 14:50:14
* @Entity com.tianji.learning.domain.po.LearningLesson
*/
public interface LearningLessonMapper extends BaseMapper<LearningLesson> {

    Integer queryTotalPlan(Long userId);
}




