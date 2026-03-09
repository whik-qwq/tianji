package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoardSeason;

import java.time.LocalDateTime;

/**
* @author xuhe8
* @description 针对表【points_board_season】的数据库操作Service
* @createDate 2026-02-18 15:02:45
*/
public interface PointsBoardSeasonService extends IService<PointsBoardSeason> {

    Integer querySeasonByTime(LocalDateTime time);
}
