package com.tianji.learning.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.PointsBoardSeasonService;
import com.tianji.learning.service.PointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {

    private final PointsBoardSeasonService seasonService;

    private final PointsBoardService pointsBoardService;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("createTableJob")// 每月1号，凌晨3点执行
    public void createPointsBoardTableOfLastSeason(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.查询赛季id
        Integer season = seasonService.querySeasonByTime(time);
        if (season == null) {
            // 赛季不存在
            return;
        }
        // 3.创建表
        pointsBoardService.createPointsBoardTableBySeason(season);
    }
    @XxlJob("savePointsRecord2DB")
    public void savePointsBoard2DB(){
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        Integer season = seasonService.querySeasonByTime(time);
        TableInfoContext.setInfo("points_board_"+season);
       String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        int pageNo = shardIndex+1;
       int pageSize = 100;
       while(true){
           List<PointsBoard> list = pointsBoardService.queryMyCurrentBoardList(key, pageNo, pageSize);
           if(CollUtils.isEmpty(list)){
               break;
           }
            list.forEach(b->{
                b.setId(b.getRank().longValue());
                b.setRank(null);
            });
           pointsBoardService.saveBatch(list);
           pageNo+=shardTotal;
       }
       TableInfoContext.clear();
    }
    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.计算key
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 3.删除
        redisTemplate.unlink(key);
    }

}