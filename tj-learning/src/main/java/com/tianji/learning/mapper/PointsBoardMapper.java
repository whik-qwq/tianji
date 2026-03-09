package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.po.PointsBoard;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【points_board(学霸天梯榜)】的数据库操作Mapper
* @createDate 2026-02-18 15:02:45
* @Entity com.tianji.learning.domain.po.PointsBoard
*/
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    void createPointsBoardTable(@Param("tableName") String s);
    @Select("select * from ${tableName} where id between #{begin} and #{end}")
    List<PointsBoard> queryMyHistoryBoardList(@Param("tableName")String tableName,
                                              @Param("begin") int begin,
                                              @Param("end")int end);
    @Select("select * from ${tableName} where userid=#{userId}")
    PointsBoard queryMyHistoryBoard(@Param("tableName")String tableName, @Param("userId")Long userId);
}




