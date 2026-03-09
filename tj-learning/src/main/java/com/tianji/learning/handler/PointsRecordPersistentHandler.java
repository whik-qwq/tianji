package com.tianji.learning.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.service.PointsBoardSeasonService;
import com.tianji.learning.service.PointsRecordService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PointsRecordPersistentHandler {
    private final PointsBoardSeasonService seasonService;
    private final PointsRecordService pointsRecordService;
    @XxlJob("createTableJob")// 每月1号，凌晨3点执行
    public void createPointsRecordTableOfLastSeason(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.查询赛季id
        Integer season = seasonService.querySeasonByTime(time);
        if (season == null) {
            // 赛季不存在
            return;
        }
        // 3.创建表
        pointsRecordService.createPointsRecordTableBySeason(season);
    }
    @XxlJob("savePointsRecord2DB")
    public void savePointsRecord2DB(){
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        Integer season = seasonService.querySeasonByTime(time);
        TableInfoContext.setInfo("points_record_"+season);
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        int pageNo = shardIndex+1;
        int pageSize = 100;
        while(true){
            List<PointsRecord> list = pointsRecordService.queryCurrentRecordList(pageNo, pageSize);
            if(CollUtils.isEmpty(list)){
                break;
            }
            pointsRecordService.saveBatch(list);
            pageNo+=shardTotal;
        }
        TableInfoContext.clear();
    }
}
