package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.LearningLessonService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/lessons")
public class LearningLessonController {
    @Autowired
    private LearningLessonService learningLessonService;

    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
        return learningLessonService.queryMyLessons(pageQuery);
    }
    @DeleteMapping("/{courseId}")
    public void deleteMyLessons(@PathVariable("courseId") Long courseId) {
        learningLessonService.deleteUserLessons(UserContext.getUser(), courseId);
    }
    @GetMapping("/now")
    @ApiOperation("查询我正在学习的课程")
    public LearningLessonVO queryMyCurrentLesson() {
        return learningLessonService.queryMyCurrentLesson();
    }

    @GetMapping("/lessons/{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId") Long courseId){
        return  learningLessonService.isLessonValid(courseId);
    }
    @ApiOperation("查询用户课表中指定课程状态")
    @GetMapping("/{courseId}")
    public LearningLessonVO queryLessonByCourseId(@PathVariable("courseId") Long courseId){
        return learningLessonService.queryLessonByCourseId(courseId);
    }
    @PostMapping("/plans")
    public void createLearningPlans(@Valid @RequestBody LearningPlanDTO learningPlanDTO) {
        learningLessonService.createLearningPlans(learningPlanDTO);
    }

    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlans(PageQuery pageQuery){
        return learningLessonService.queryMyPlans(pageQuery);
    }

}
