package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 优惠券作用范围信息
 * @TableName coupon_scope
 */
@TableName(value ="coupon_scope")
@Accessors(chain = true)
@Data
public class CouponScope {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 范围限定类型：1-分类，2-课程，等等
     */
    private Integer type;

    /**
     * 优惠券id
     */
    private Long couponId;

    /**
     * 优惠券作用范围的业务id，例如分类id、课程id
     */
    private Long bizId;
}