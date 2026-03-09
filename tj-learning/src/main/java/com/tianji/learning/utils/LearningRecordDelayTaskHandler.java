package com.tianji.learning.utils;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.LearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
@Component
@RequiredArgsConstructor
public class LearningRecordDelayTaskHandler {

    private final StringRedisTemplate redisTemplate;

    private final static String Record_KEY_TEMPLATE="learning:record:{}";
    private final DelayQueue<DelayTask<RecordTaskData>> queue = new DelayQueue<>();
    private final LearningRecordMapper learningRecordMapper;
    private final LearningLessonService learningLessonService;

    @PostConstruct
    public void init(){
        CompletableFuture.runAsync(this::handleDelayTask);
    }

    public void handleDelayTask(){
        try {
            while (true){
                DelayTask<RecordTaskData> task = queue.take();
                RecordTaskData data = task.getData();
                LearningRecord record = readRecordCache(data.getLessonId(), data.getSectionId());
                if (record == null){
                    continue;
                }
                if(!Objects.equals(data.getMoment(), record.getMoment())){
                    continue;
                }
                record.setFinished(null);
                learningRecordMapper.updateById(record);

                LearningLesson lesson = new LearningLesson();
                lesson.setId(data.getLessonId());
                lesson.setLatestSectionId(data.getSectionId());
                lesson.setLatestLearnTime(new Date());
                learningLessonService.updateById(lesson);
            }
        } catch (InterruptedException e) {
           e.printStackTrace();
        }
    }

    public void addLearningRecordTask(LearningRecord record){
        writeRecordCache(record);
        queue.add(new DelayTask<RecordTaskData>(new RecordTaskData(record),Duration.ofSeconds(20)));
    }

    public void writeRecordCache(LearningRecord record) {
        String jsonStr = JsonUtils.toJsonStr(new RecordCacheData(record));

        String key= StringUtils.format(Record_KEY_TEMPLATE,record.getLessonId());
        redisTemplate .opsForHash().put(key,record.getSectionId().toString(),jsonStr);

        redisTemplate.expire(key, Duration.ofMinutes(1));
    }

    public LearningRecord readRecordCache(Long lessonId,Long sectionId){
        String Key =StringUtils.format(Record_KEY_TEMPLATE,lessonId);
        Object cacheData = redisTemplate.opsForHash().get(Key, sectionId.toString());
        if(cacheData==null){
            return null;
        }
        return JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
    }
    public void cleanRecordCache(Long lessonId,Long sectionId){
        String Key =StringUtils.format(Record_KEY_TEMPLATE,lessonId);
        redisTemplate.opsForHash().delete(Key, sectionId.toString());
    }

    @Data
    @NoArgsConstructor
    private static class RecordCacheData{
        private Long id;
        private Integer moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record) {
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }
    @Data
    @NoArgsConstructor
    private static class RecordTaskData{
        private Long lessonId;
        private Long sectionId;
        private Integer moment;

        public RecordTaskData(LearningRecord record) {
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
            this.moment = record.getMoment();
        }
    }

}
