package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author xuhe8
* @description 针对表【learning_record(学习记录表)】的数据库操作Service
* @createDate 2026-02-02 10:27:13
*/
public interface LearningRecordService extends IService<LearningRecord> {

    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    void addLearningRecord(LearningRecordFormDTO learningRecordFormDTO);
}
