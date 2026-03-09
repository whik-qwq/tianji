package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.LearningLessonService;
import com.tianji.learning.service.LearningRecordService;
import com.tianji.learning.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【learning_record(学习记录表)】的数据库操作Service实现
* @createDate 2026-02-02 10:27:13
*/
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord>
    implements LearningRecordService{


    private final LearningLessonService learningLessonService;
    private final CourseClient courseClient;
    private final LearningRecordDelayTaskHandler taskHandler;
    private final RabbitMqHelper rabbitMqHelper;

    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson= learningLessonService.queryByUserIdAndCourseId(userId,courseId);
        if(lesson!=null){
            return null;
        }
        List<LearningRecord> records = lambdaQuery().eq(LearningRecord::getLessonId, lesson.getId()).list();
        LearningLessonDTO dto = new LearningLessonDTO();
        dto.setId(lesson.getId());
        dto.setLatestSectionId(lesson.getLatestSectionId());
        dto.setRecords(BeanUtils.copyList(records, LearningRecordDTO.class));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLearningRecord(LearningRecordFormDTO learningRecordFormDTO) {
        Long userId = UserContext.getUser();

        boolean finished=false;

        if(learningRecordFormDTO.getSectionType()==SectionType.VIDEO){
            finished=handleVideoRecord(userId,learningRecordFormDTO);
        }else{
            finished=handleExamRecord(userId,learningRecordFormDTO);
        }
        if(!finished){
            return;
        }
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.LEARN_SECTION,
                SignInMessage.of(userId, null));
        handlerLearningLessonsChanges(learningRecordFormDTO);
    }

    private void handlerLearningLessonsChanges(LearningRecordFormDTO learningRecordFormDTO) {

        LearningLesson lesson = learningLessonService.getById(learningRecordFormDTO.getLessonId());

        boolean allLearned=false;
        CourseFullInfoDTO course = courseClient.getCourseInfoById(lesson.getCourseId(),false,false);
        allLearned=lesson.getLearnedSections()+1>=course.getSectionNum();

        learningLessonService.lambdaUpdate().set(allLearned,LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
                .setSql("learned_sections=learned_sections+1")
                .set(lesson.getLearnedSections()==0,LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .eq(LearningLesson::getId,lesson.getId())
                .update();
    }

    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO learningRecordFormDTO) {
        LearningRecord old = getLearningRecord(learningRecordFormDTO.getLessonId(),learningRecordFormDTO.getSectionId());
        if(old==null){
            LearningRecord learningRecord = BeanUtils.copyBean(learningRecordFormDTO, LearningRecord.class);

            learningRecord.setUserId(userId);

            save(learningRecord);
            return false;
        }
        boolean finished= learningRecordFormDTO.getMoment()*2>=learningRecordFormDTO.getDuration()&& !old.getFinished();

        if(!finished){
            LearningRecord record = new LearningRecord();
            record.setLessonId(learningRecordFormDTO.getLessonId());
            record.setSectionId(learningRecordFormDTO.getSectionId());
            record.setMoment(learningRecordFormDTO.getMoment());
            record.setId(old.getId());
            record.setFinished(true);
            taskHandler.addLearningRecordTask(record);
            return false;
        }
        lambdaUpdate().set(LearningRecord::getMoment,learningRecordFormDTO.getMoment())
                .set(LearningRecord::getFinished,true)
                .set(LearningRecord::getFinishTime,learningRecordFormDTO.getCommitTime())
                .eq(LearningRecord::getId,old.getId()).update();

        taskHandler.cleanRecordCache(learningRecordFormDTO.getLessonId(),learningRecordFormDTO.getSectionId());
        return true;
    }

    private LearningRecord getLearningRecord(Long lessonId,Long sectionId) {
        LearningRecord record = taskHandler.readRecordCache(lessonId, sectionId);
        if(record!=null){
            return record;
        }
        record = lambdaQuery().eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        taskHandler.writeRecordCache(record);
        return record;
    }

    private boolean handleExamRecord(Long userId, LearningRecordFormDTO learningRecordFormDTO) {
        LearningRecord learningRecord = BeanUtils.copyBean(learningRecordFormDTO, LearningRecord.class);

        learningRecord.setUserId(userId);
        learningRecord.setFinished(true);
        learningRecord.setFinishTime(learningRecordFormDTO.getCommitTime());

        save(learningRecord);
        return true;
    }
}




