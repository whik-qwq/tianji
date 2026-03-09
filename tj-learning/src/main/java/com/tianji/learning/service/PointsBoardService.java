package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【points_board(学霸天梯榜)】的数据库操作Service
* @createDate 2026-02-18 15:02:45
*/

public interface PointsBoardService extends IService<PointsBoard> {

    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    void createPointsBoardTableBySeason(Integer season);
    List<PointsBoard> queryMyCurrentBoardList(String key, Integer pageNo, Integer pageSize);
}
