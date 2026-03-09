package com.tianji.learning.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.PointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
* @author xuhe8
* @description 针对表【points_record(学习积分记录，每个月底清零)】的数据库操作Service实现
* @createDate 2026-02-18 15:02:45
*/
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord>
    implements PointsRecordService{

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType pointsRecordType) {
        int maxPoints = pointsRecordType.getMaxPoints();
        int realPoints=points;
        LocalDateTime now = LocalDateTime.now();
        if(maxPoints>0){

            LocalDateTime beginTime= DateUtils.getDayStartTime(now);
            LocalDateTime endTime= DateUtils.getDayEndTime(now);

            int currentPoints=queryUserPointsByTypeAndDate(userId,pointsRecordType,beginTime,endTime);

            if(currentPoints>=maxPoints){
                return;
            }
            if(currentPoints +points>maxPoints){
                realPoints=maxPoints-currentPoints;
            }
        }
        PointsRecord pointsRecord = new PointsRecord();
        pointsRecord.setPoints(realPoints);
        pointsRecord.setUserId(userId);
        pointsRecord.setType(pointsRecordType);
        save(pointsRecord);

        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        stringRedisTemplate.opsForZSet().incrementScore(key,userId.toString(),realPoints);
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beginTime= DateUtils.getDayStartTime(now);
        LocalDateTime endTime= DateUtils.getDayEndTime(now);

        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, beginTime, endTime);

        List<PointsRecord> list= getBaseMapper().queryUserPointsByDate(wrapper);
        List<PointsStatisticsVO> vos=new ArrayList<>(list.size());
        if(CollUtils.isEmpty(list)){
            return CollUtils.emptyList();
        }
        for (PointsRecord p : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public void createPointsRecordTableBySeason(Integer season) {
        getBaseMapper().createPointsRecordTableBySeason("points_record"+season);
    }

    @Override
    public List<PointsRecord> queryCurrentRecordList(int pageNo, int pageSize) {
        Page<PointsRecord> pointsRecordPage = new Page<>(pageNo, pageSize);
        Page<PointsRecord> page = lambdaQuery().page(pointsRecordPage);
        return page.getRecords();
    }

    private int queryUserPointsByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(type != null, PointsRecord::getType, type)
                .between(begin != null && end != null, PointsRecord::getCreateTime, begin, end);
        Integer points= getBaseMapper().queryUserPointsByTypeAndDate(wrapper);

        return points==null?0:points;
    }
}




