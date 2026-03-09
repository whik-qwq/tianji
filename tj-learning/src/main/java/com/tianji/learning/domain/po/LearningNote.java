package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 学习笔记表
 * @TableName learning_note
 */
@TableName(value ="learning_note")
@Data
public class LearningNote {
    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 笔记内容
     */
    private String content;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 章id
     */
    private Long chapterId;

    /**
     * 小节id
     */
    private Long sectionId;

    /**
     * 是否私密：0-公开，1-私密
     */
    private Boolean isPrivate;

    /**
     * 视频播放时间点（秒）
     */
    private Integer noteMoment;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否是采集的笔记：0-否，1-是
     */
    private Boolean isGathered;

    /**
     * 笔记作者id（如果是采集的，记录原作者）
     */
    private Long authorId;

    /**
     * 采集人ID集合
     */
    private String gatherIds;

    /**
     * 被采集次数
     */
    private Integer usedTimes;

    /**
     * 是否在用户端隐藏：0-不隐藏(false)，1-隐藏(true)
     */
    private Boolean hidden;

    /**
     * 来源笔记ID，如果是采集的笔记则记录原笔记ID
     */
    private Long originNoteId;
}