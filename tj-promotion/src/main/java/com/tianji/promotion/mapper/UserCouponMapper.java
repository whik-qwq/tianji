package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.enums.UserCouponStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【user_coupon(用户领取优惠券的记录，是真正使用的优惠券信息)】的数据库操作Mapper
* @createDate 2026-02-25 23:14:29
* @Entity com.tianji.promotion.domain.po.UserCoupon
*/
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    List<Coupon> queryMyCoupon(Long userId);

    List<Coupon> queryCouponByUserCouponIds(@Param("userCouponIds") List<Long> userCouponIds,
                                            @Param("status") UserCouponStatus userCouponStatus);
}




