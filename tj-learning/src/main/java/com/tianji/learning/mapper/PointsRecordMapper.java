package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.tianji.learning.domain.po.PointsRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【points_record(学习积分记录，每个月底清零)】的数据库操作Mapper
* @createDate 2026-02-18 15:02:45
* @Entity com.tianji.learning.domain.po.PointsRecord
*/
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {
    @Select("SELECT SUM(points) FROM points_record ${ew.customSqlSegment}")
    Integer queryUserPointsByTypeAndDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);

    @Select("SELECT type,SUM(points) As points FROM points_record ${ew.customSqlSegment} GROUP BY type")
    List<PointsRecord> queryUserPointsByDate(@Param(Constants.WRAPPER)QueryWrapper<PointsRecord> wrapper);

    void createPointsRecordTableBySeason(String tableName);
}




