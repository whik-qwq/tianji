package com.tianji.remark.task;

import com.tianji.remark.service.LikedRecordService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class LikedTimesCheckTask {

    private static final List<String>BIZ_TYPES=List.of("QA","NOTE");
    private final LikedRecordService likedRecordService;
    private static final int MAX_BIZ_SIZE=50;

    @Scheduled(fixedRate = 60000)
    public void checkLikedTimes(){
        for (String bizType : BIZ_TYPES) {
            likedRecordService.readLikedTimesAndSendMessage(bizType,MAX_BIZ_SIZE);
        }
    }
}
