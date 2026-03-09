package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;

import javax.validation.Valid;
import java.util.List;

/**
* @author xuhe8
* @description 针对表【coupon(优惠券的规则信息)】的数据库操作Service
* @createDate 2026-02-24 23:00:53
*/
public interface CouponService extends IService<Coupon> {

    void saveCoupon(@Valid CouponFormDTO dto);

    PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query);

    void beginIssue(@Valid CouponIssueFormDTO dto);

    List<CouponVO> queryIssuingCoupons();

    void updateCoupon(@Valid CouponFormDTO dto);

    void deleteCoupon(Long id);

    CouponDetailVO queryCouponById(Long id);

    void pauseIssue(Long id);
}
