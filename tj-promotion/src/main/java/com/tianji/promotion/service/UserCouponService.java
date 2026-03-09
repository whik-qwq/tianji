package com.tianji.promotion.service;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.OrderCouponDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【user_coupon(用户领取优惠券的记录，是真正使用的优惠券信息)】的数据库操作Service
* @createDate 2026-02-25 23:14:29
*/
@Service
public interface UserCouponService extends IService<UserCoupon> {

    void receiveCoupon(Long couponId);

    @Transactional
    void checkAndCreateCoupon(UserCouponDTO uc);

    void exchangeCoupon(String code);

    PageDTO<CouponVO> queryMyCouponByPage(UserCouponQuery query);

    void writeOffCoupon(List<Long> userCouponIds);
}
