package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学霸天梯榜
 * @TableName points_board
 */
@TableName(value ="points_board")
@Data
public class PointsBoard {
    /**
     * 榜单id
     */
    @TableId(value = "id",type = IdType.INPUT)
    private Long id;

    /**
     * 学生id
     */
    private Long userId;

    /**
     * 积分值
     */
    private Integer points;

    /**
     * 名次，只记录赛季前100
     */
    private Integer rank;

    /**
     * 赛季，例如 1,就是第一赛季，2-就是第二赛季
     */
    private Integer season;
}