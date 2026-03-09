package com.tianji.promotion.handler;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.CouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScheduleIssuePromotion {
    private final CouponService couponService;
    @XxlJob("beginIssuePromotion")
    public void beginIssuePromotion(){
        couponService.lambdaUpdate().eq(Coupon::getStatus, CouponStatus.DRAFT)
                .ge(Coupon::getIssueBeginTime, LocalDateTime.now())
                .set(Coupon::getStatus,CouponStatus.ISSUING).update();
    }

    @XxlJob("endIssuePromotion")
    public void endIssuePromotion(){
        couponService.lambdaUpdate().eq(Coupon::getStatus, CouponStatus.DRAFT)
                .le(Coupon::getIssueEndTime, LocalDateTime.now())
                .set(Coupon::getStatus,CouponStatus.ISSUING).update();
    }
}
