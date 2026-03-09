package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import com.tianji.learning.enums.PointsRecordType;
import lombok.Data;

/**
 * 学习积分记录，每个月底清零
 * @TableName points_record
 */
@TableName(value ="points_record")
@Data
public class PointsRecord {
    /**
     * 积分记录表id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 积分方式：1-课程学习，2-每日签到，3-课程问答， 4-课程笔记，5-课程评价
     */
    private PointsRecordType type;

    /**
     * 积分值
     */
    private Integer points;

    /**
     * 创建时间
     */
    private Date createTime;
}