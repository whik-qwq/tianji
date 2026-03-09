package com.tianji.promotion.service.impl;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.autoconfigure.xxljob.XxlJobProperties;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.dto.OrderCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.CouponScopeService;
import com.tianji.promotion.service.DiscountService;
import com.tianji.promotion.strategy.discount.Discount;
import com.tianji.promotion.strategy.discount.DiscountStrategy;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {
    private final UserCouponMapper userCouponMapper;
    private final CouponScopeService couponScopeService;
    private final Executor discountSolutionExecutor;

    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        List<Coupon> coupons= userCouponMapper.queryMyCoupon(UserContext.getUser());
        if(CollUtils.isEmpty(coupons)){
            return CollUtils.emptyList();
        }
        int totalAmount = orderCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        List<Coupon> availableCoupons = coupons.stream().filter(c -> DiscountStrategy.getDiscount(c.getDiscountType()).canUse(totalAmount, c))
                .collect(Collectors.toList());
        if(CollUtils.isEmpty(availableCoupons)){
            return CollUtils.emptyList();
        }
        Map<Coupon,List<OrderCourseDTO>> availableCouponMap=findAvailableCoupon(availableCoupons,orderCourses);
        availableCoupons=new ArrayList<>(availableCouponMap.keySet());
        List<List<Coupon>> permute = PermuteUtil.permute(availableCoupons);
        for (Coupon availableCoupon : availableCoupons) {
            permute.add(List.of(availableCoupon));
        }
        List<CouponDiscountDTO> list= Collections.synchronizedList(new ArrayList<>(permute.size()));
        CountDownLatch latch = new CountDownLatch(permute.size());

        for (List<Coupon> solution : permute) {
            CompletableFuture.supplyAsync(() -> calculateSolutionDiscount(availableCouponMap,orderCourses, solution))
                    .thenAccept(dto->{
                        list.add(dto);
                        latch.countDown();
                    });
        }
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return findBestSolution(list);
    }

    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        // 1.准备Map记录最优解
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
        // 2.遍历，筛选最优解
        for (CouponDiscountDTO solution : list) {
            // 2.1.计算当前方案的id组合
            String ids = solution.getIds().stream()
                    .sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
            // 2.2.比较用券相同时，优惠金额是否最大
            CouponDiscountDTO best = moreDiscountMap.get(ids);
            if (best != null && best.getDiscountAmount() >= solution.getDiscountAmount()) {
                // 当前方案优惠金额少，跳过
                continue;
            }
            // 2.3.比较金额相同时，用券数量是否最少
            best = lessCouponMap.get(solution.getDiscountAmount());
            int size = solution.getIds().size();
            if (size > 1 && best != null && best.getIds().size() <= size) {
                // 当前方案用券更多，放弃
                continue;
            }
            // 2.4.更新最优解
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        // 3.求交集
        Collection<CouponDiscountDTO> bestSolutions = CollUtils
                .intersection(moreDiscountMap.values(), lessCouponMap.values());
        // 4.排序，按优惠金额降序
        return bestSolutions.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }

    private CouponDiscountDTO calculateSolutionDiscount(Map<Coupon, List<OrderCourseDTO>> couponMap,
                                                        List<OrderCourseDTO> courses,
                                                        List<Coupon> solution) {
        CouponDiscountDTO dto = new CouponDiscountDTO();
        Map<Long, Integer> detailMap = courses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, oc -> 0));
        dto.setDiscountDetail(detailMap);
        for (Coupon coupon : solution) {
            List<OrderCourseDTO> availableCourses = couponMap.get(coupon);
            int totalAmount = availableCourses
                    .stream()
                    .mapToInt(oc->oc.getPrice()-detailMap.get(oc.getId())).sum();
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if(!discount.canUse(totalAmount,coupon)){
                continue;
            }
            int discountAmount = discount.calculateDiscount(totalAmount, coupon);
            calculateDiscountDetails(detailMap, availableCourses, totalAmount, discountAmount);
            dto.getIds().add(coupon.getCreater());
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(discountAmount + dto.getDiscountAmount());
        }
        return dto;
    }

    private void calculateDiscountDetails(Map<Long, Integer> detailMap,
                                          List<OrderCourseDTO> courses,
                                          int totalAmount,
                                          int discountAmount) {
        int times = 0;
        int remainDiscount = discountAmount;
        for (OrderCourseDTO course : courses) {
            // 更新课程已计算数量
            times++;
            int discount = 0;
            // 判断是否是最后一个课程
            if (times == courses.size()) {
                // 是最后一个课程，总折扣金额 - 之前所有商品的折扣金额之和
                discount = remainDiscount;
            } else {
                // 计算折扣明细（课程价格在总价中占的比例，乘以总的折扣）
                discount = discountAmount * course.getPrice() / totalAmount;
                remainDiscount -= discount;
            }
            // 更新折扣明细
            detailMap.put(course.getId(), discount + detailMap.get(course.getId()));
        }
    }

    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(List<Coupon> coupons, List<OrderCourseDTO> courses) {
        Map<Coupon, List<OrderCourseDTO>> map=new HashMap<>();
        for (Coupon coupon : coupons) {
            List<OrderCourseDTO> availableCourses=courses;
            if(coupon.getSpecific()){
                List<CouponScope> scopes = couponScopeService.lambdaQuery()
                        .eq(CouponScope::getCouponId, coupon.getId()).list();
                Set<Long> scopeIds = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
                availableCourses = courses.stream()
                        .filter(c -> scopeIds.contains(c.getId()))
                        .collect(Collectors.toList());
            }
            int totalAmount = availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if(discount.canUse(totalAmount,coupon)){
                map.put(coupon,availableCourses);
            }
        }
        return map;
    }

    @Override
    public CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO) {
        // 1.查询用户优惠券
        List<Long> userCouponIds = orderCouponDTO.getUserCouponIds();
        List<Coupon> coupons = userCouponMapper.queryCouponByUserCouponIds(userCouponIds, UserCouponStatus.UNUSED);
        if (CollUtils.isEmpty(coupons)) {
            return null;
        }
        // 2.查询优惠券对应课程
        Map<Coupon, List<OrderCourseDTO>> availableCouponMap = findAvailableCoupon(coupons, orderCouponDTO.getCourseList());
        if (CollUtils.isEmpty(availableCouponMap)) {
            return null;
        }
        // 3.查询优惠券规则
        return calculateSolutionDiscount(availableCouponMap, orderCouponDTO.getCourseList(), coupons);
    }
}
