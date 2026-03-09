package com.tianji.learning.controller;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.PointsBoardSeasonService;
import com.tianji.learning.service.PointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards/season")
@Api(tags = "积分相关接口")
public class PointsBoardSeasonController {

    private final PointsBoardSeasonService PointsBoardSeasonService;

    @GetMapping
    @ApiOperation("分页查询指定赛季的积分排行榜")
    public List<PointsBoardSeason> queryPointsBoardBySeason(PointsBoardQuery query){
        return PointsBoardSeasonService.list();
    }
}
