package com.tianji.learning.controller;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.LearningRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/learning-records")
public class LearningRecordController {

    @Autowired
    private LearningRecordService learningRecordService;

    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId")Long courseId){
        return learningRecordService.queryLearningRecordByCourse(courseId);
    }

    @PostMapping
    public void addLearningRecord(@RequestBody LearningRecordFormDTO learningRecordFormDTO){
        learningRecordService.addLearningRecord(learningRecordFormDTO);
    }
}
