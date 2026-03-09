package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【points_record(学习积分记录，每个月底清零)】的数据库操作Service
* @createDate 2026-02-18 15:02:45
*/

public interface PointsRecordService extends IService<PointsRecord> {

    void addPointsRecord(Long userId, int i, PointsRecordType pointsRecordType);

    List<PointsStatisticsVO> queryMyPointsToday();

    void createPointsRecordTableBySeason(Integer season);

    List<PointsRecord> queryCurrentRecordList(int pageNo, int pageSize);
}
