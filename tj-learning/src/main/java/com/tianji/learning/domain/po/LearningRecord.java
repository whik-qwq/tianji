package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 学习记录表
 * @TableName learning_record
 */
@TableName(value ="learning_record")
@Data
public class LearningRecord {
    /**
     * 学习记录的id
     */
    @TableId
    private Long id;

    /**
     * 对应课表的id
     */
    private Long lessonId;

    /**
     * 对应小节的id
     */
    private Long sectionId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 视频的当前观看时间点，单位秒
     */
    private Integer moment;

    /**
     * 是否完成学习，默认false
     */
    private Boolean finished;

    /**
     * 第一次观看时间
     */
    private LocalDateTime createTime;

    /**
     * 完成学习的时间
     */
    private LocalDateTime finishTime;

    /**
     * 更新时间（最近一次观看时间）
     */
    private LocalDateTime updateTime;
}