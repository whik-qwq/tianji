package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【coupon(优惠券的规则信息)】的数据库操作Mapper
* @createDate 2026-02-24 23:00:53
* @Entity generator.domain.Coupon
*/
public interface CouponMapper extends BaseMapper<Coupon> {
    @Update("update coupon set issue_num=issue_num+1 where id=#{couponId} and issue_num<total_num")
    int incrIssueNum(@Param("couponId") Long couponId);

    int incrUseNum(@Param("couponIds") List<Long> couponIds, @Param("count") int count);
}




