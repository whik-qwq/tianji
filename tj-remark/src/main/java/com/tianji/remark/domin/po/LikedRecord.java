package com.tianji.remark.domin.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 点赞记录表
 * @TableName liked_record
 */
@TableName(value ="liked_record")
@Data
public class LikedRecord {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 点赞的业务id
     */
    private Long bizId;

    /**
     * 点赞的业务类型
     */
    private String bizType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}