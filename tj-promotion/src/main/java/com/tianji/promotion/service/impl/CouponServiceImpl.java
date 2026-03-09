package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.service.CouponScopeService;
import com.tianji.promotion.service.CouponService;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ExchangeCodeService;
import com.tianji.promotion.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author xuhe8
* @description 针对表【coupon(优惠券的规则信息)】的数据库操作Service实现
* @createDate 2026-02-24 23:00:53
*/
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon>
    implements CouponService{
    private final CouponScopeService scopeService;
    private final ExchangeCodeService exchangeCodeService;
    private final UserCouponService userCouponService;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        Coupon coupon = BeanUtils.copyProperties(dto, Coupon.class);
        save(coupon);
        if(!dto.getSpecific()){
            return;
        }
        List<Long> scopes = dto.getScopes();
        if(CollUtils.isEmpty(scopes)){
            return;
        }
        List<CouponScope> lists =
                scopes.stream().map(bizId -> new CouponScope().setBizId(bizId)
                        .setCouponId(coupon.getId()))
                        .collect(Collectors.toList());

        scopeService.saveBatch(lists);
    }

    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Integer type = query.getType();
        Integer status = query.getStatus();
        String name = query.getName();


        Page<Coupon> page = lambdaQuery().eq(type != null, Coupon::getType, type)
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Coupon> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        List<CouponPageVO> list = BeanUtils.copyList(records, CouponPageVO.class);
        return PageDTO.of(page, list);
    }

    @Override
    @Transactional
    public void beginIssue(CouponIssueFormDTO dto) {
        Coupon coupon = getById(dto.getId());
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        // 2.判断优惠券状态，是否是暂停或待发放
        if(coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE){
            throw new BizIllegalException("优惠券状态错误！");
        }
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        boolean isBegin = issueBeginTime==null||!issueBeginTime.isAfter(now);
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        if(isBegin){
            c.setStatus(CouponStatus.ISSUING);
            issueBeginTime=now;
        }else{
            c.setStatus(CouponStatus.UN_ISSUE);
        }
        updateById(c);
        if(isBegin){
            c.setTotalNum(coupon.getTotalNum());
            c.setUserLimit(coupon.getUserLimit());
            cacheCouponInfo(c);
        }


        if(coupon.getObtainWay()== ObtainType.ISSUE&&coupon.getStatus()==CouponStatus.DRAFT){
            coupon.setIssueEndTime(c.getIssueEndTime());
            exchangeCodeService.asyncGenerateCode(coupon);
        }
    }

    private void cacheCouponInfo(Coupon coupon) {
        Map<String, String> map = new HashMap<>(4);
        map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
        map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
        map.put("totalNum", String.valueOf(coupon.getTotalNum()));
        map.put("userLimit", String.valueOf(coupon.getUserLimit()));
        redisTemplate.opsForHash().putAll(PromotionConstants.COUPON_CACHE_KEY_PREFIX+coupon.getId(),map);
    }

    @Override
    public List<CouponVO> queryIssuingCoupons() {
        List<Coupon> coupons = lambdaQuery().eq(Coupon::getStatus, CouponStatus.ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC).list();
        if(CollUtils.isEmpty(coupons)){
            return CollUtils.emptyList();
        }
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery().eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIds).list();
        Map<Long, Long> issuedMap =
                userCoupons.stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        Map<Long, Long> unusedMap  = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        List<CouponVO> list = new ArrayList<>(coupons.size());
        for(Coupon coupon : coupons){
            CouponVO couponVO = BeanUtils.copyBean(coupon, CouponVO.class);
            list.add(couponVO);
            couponVO.setAvailable(coupon.getIssueNum()<coupon.getTotalNum()
                    && issuedMap.getOrDefault(coupon.getId(),0L)<coupon.getUserLimit());
            couponVO.setReceived(unusedMap.getOrDefault(coupon.getId(),0L)>0);
        }
        return list;
    }

    @Override
    public void updateCoupon(CouponFormDTO dto) {
        Coupon coupon = BeanUtils.copyProperties(dto, Coupon.class);
        if(coupon.getStatus()!=CouponStatus.DRAFT){
            return;
        }
        updateById(coupon);
        if(!dto.getSpecific()){
            return;
        }
        List<Long> scopeIds = dto.getScopes();
        if(CollUtils.isEmpty(scopeIds)){
            return;
        }
        List<Long> ids = scopeService.lambdaQuery()
                .select(CouponScope::getId).eq(CouponScope::getCouponId, dto.getId()).list()
                .stream().map(CouponScope::getId).collect(Collectors.toList());
        scopeService.removeByIds(ids);
        // 删除成功后，并且有范围再插入
        if (CollUtils.isNotEmpty(scopeIds)) {
            List<CouponScope> lis = scopeIds.stream()
                    .map(i -> new CouponScope().setCouponId(dto.getId()).setType(1).setBizId(i))
                    .collect(Collectors.toList());
            scopeService.saveBatch(lis);
        }
    }

    @Override
    public void deleteCoupon(Long id) {
        removeById(id);
    }

    @Override
    public CouponDetailVO queryCouponById(Long id) {
        Coupon coupon = getById(id);
        CouponDetailVO detailVO = BeanUtils.copyBean(coupon, CouponDetailVO.class);
        return detailVO;
    }

    @Override
    @Transactional
    public void pauseIssue(Long id) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        if(coupon.getStatus() != CouponStatus.ISSUING){
            throw new BizIllegalException("优惠券状态错误！");
        }
        coupon.setStatus(CouponStatus.PAUSE);
        updateById(coupon);
        redisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX+id);
    }

}




