package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.autoconfigure.redisson.annotations.Lock;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.OrderCouponDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ExchangeCodeService;
import com.tianji.promotion.service.UserCouponService;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.utils.MyLock;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tianji.promotion.service.impl.DiscountServiceImpl.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author xuhe8
* @description 针对表【user_coupon(用户领取优惠券的记录，是真正使用的优惠券信息)】的数据库操作Service实现
* @createDate 2026-02-25 23:14:29
*/
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon>
    implements UserCouponService{
    private final CouponMapper couponMapper;
    private final ExchangeCodeService codeService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper  rabbitMqHelper;

    @Override
    @Lock(name = "lock:coupon:#{couponId}")
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long couponId) {
        Coupon coupon = queryCouponByCache(couponId);
        if(coupon == null){
            throw new BadRequestException("优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
            throw new BadRequestException("优惠券发放已经结束或尚未开始");
        }
        // 3.校验库存
        if (coupon.getTotalNum()<=0) {
            throw new BadRequestException("优惠券库存不足");
        }
        Long userId = UserContext.getUser();
        String key=PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX+couponId;
        Long count = redisTemplate.opsForHash().increment(key, userId.toString(),1);
        if(count>coupon.getUserLimit()){

        }
        redisTemplate.opsForHash().increment(PromotionConstants.COUPON_CACHE_KEY_PREFIX+couponId,
                "totalNum",-1);
        UserCouponDTO userCouponDTO = new UserCouponDTO();
        userCouponDTO.setUserId(userId);
        userCouponDTO.setCouponId(couponId);
        rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,
                MqConstants.Key.COUPON_RECEIVE,userCouponDTO);
    }

    private Coupon queryCouponByCache(Long couponId) {
        String key= PromotionConstants.COUPON_CACHE_KEY_PREFIX+couponId;
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        return BeanUtils.mapToBean(map,Coupon.class,false, CopyOptions.create());
    }

    @Override
    public void checkAndCreateCoupon(UserCouponDTO uc) {
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if(coupon == null){

        }
        int result = couponMapper.incrIssueNum(coupon.getId());
        if (result == 0) {
            throw new BizIllegalException("优惠券库存不足");
        }
        saveUserCoupon(uc.getUserId(),coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangeCoupon(String code) {
        long serialNum = CodeUtil.parseCode(code);
        boolean exchanged = codeService.updateExchangeMark(serialNum, true);
        if (exchanged) {
            ExchangeCode exchangeCode = codeService.getById(serialNum);
            if (exchangeCode == null) {
                throw new BizIllegalException("兑换码不存在！");
            }
            // 4.是否过期
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(exchangeCode.getExpiredTime())) {
                throw new BizIllegalException("兑换码已经过期");
            }
            try {
                Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
                Long userId = UserContext.getUser();
                UserCouponDTO uc = new UserCouponDTO();
                uc.setUserId(userId);
                uc.setCouponId(coupon.getId());
                checkAndCreateCoupon(uc);
                codeService.lambdaUpdate()
                        .set(ExchangeCode::getUserId, userId)
                        .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                        .eq(ExchangeCode::getId, exchangeCode.getId())
                        .update();
            } catch (Exception e) {
                codeService.updateExchangeMark(serialNum, false);
                throw e;
            }
        }
    }
    @Override
    public PageDTO<CouponVO> queryMyCouponByPage(UserCouponQuery query) {
        Page<UserCoupon> page = lambdaQuery().eq(UserCoupon::getUserId, UserContext.getUser())
                .eq(UserCoupon::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<UserCoupon> records = page.getRecords();
        List<Long> ids = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = couponMapper.selectBatchIds(ids);
        List<CouponVO> result=new ArrayList<>();
        Map<Long, Coupon> couponMap = coupons.stream().collect(Collectors.toMap(Coupon::getId, uc -> uc));
        for (UserCoupon record : records) {
            CouponVO vo = BeanUtils.copyBean(record, CouponVO.class);
            Coupon coupon = couponMap.get(record.getCouponId());
            vo.setName(coupon.getName());
            vo.setSpecific(coupon.getSpecific());
            vo.setDiscountType(coupon.getDiscountType());
            vo.setThresholdAmount(coupon.getThresholdAmount());
            vo.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
            vo.setDiscountValue(coupon.getDiscountValue());
            result.add(vo);
        }
        return PageDTO.of(page,result);
    }

    @Override
    @Transactional
    public void writeOffCoupon(List<Long> userCouponIds) {
        // 1.查询优惠券
        List<UserCoupon> userCoupons = listByIds(userCouponIds);
        if (CollUtils.isEmpty(userCoupons)) {
            return;
        }
        // 2.处理数据
        List<UserCoupon> list = userCoupons.stream()
                // 过滤无效券
                .filter(coupon -> {
                    if (coupon == null) {
                        return false;
                    }
                    if (UserCouponStatus.UNUSED != coupon.getStatus()) {
                        return false;
                    }
                    LocalDateTime now = LocalDateTime.now();
                    return !now.isBefore(coupon.getTermBeginTime()) && !now.isAfter(coupon.getTermEndTime());
                })
                // 组织新增数据
                .map(coupon -> {
                    UserCoupon c = new UserCoupon();
                    c.setId(coupon.getId());
                    c.setStatus(UserCouponStatus.USED);
                    return c;
                })
                .collect(Collectors.toList());

        // 4.核销，修改优惠券状态
        boolean success = updateBatchById(list);
        if (!success) {
            return;
        }
        // 5.更新已使用数量
        List<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        int c = couponMapper.incrUseNum(couponIds, 1);
        if (c < 1) {
            throw new DbException("更新优惠券使用数量失败！");
        }
    }


    private void saveUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if(termBeginTime==null){
            termBeginTime=LocalDateTime.now();
            termEndTime=termBeginTime.plusDays(coupon.getTermDays());
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);
        save(userCoupon);
    }
}




