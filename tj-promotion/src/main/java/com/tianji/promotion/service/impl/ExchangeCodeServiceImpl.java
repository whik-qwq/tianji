package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;
import com.tianji.promotion.service.ExchangeCodeService;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.utils.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.promotion.constants.PromotionConstants.*;
/**
* @author xuhe8
* @description 针对表【exchange_code(兑换码)】的数据库操作Service实现
* @createDate 2026-02-24 23:00:53
*/
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode>
    implements ExchangeCodeService{
    private final  StringRedisTemplate redisTemplate;
    private BoundValueOperations<String, String> serialOps;
    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps = redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }

    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        Integer totalNum = coupon.getTotalNum();
        List<ExchangeCode> exchangeCodeList = new ArrayList<>();
        Long maxSerialNum = serialOps.increment(totalNum);
        if (maxSerialNum == null) {
            return;
        }
        for (int serialNum = maxSerialNum.intValue()-totalNum+1; serialNum <= maxSerialNum; serialNum++) {

            String code = CodeUtil.generateCode(serialNum, coupon.getId());
            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode.setCode(code);
            exchangeCode.setId(serialNum);
            exchangeCode.setExchangeTargetId(coupon.getId());
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());
            exchangeCodeList.add(exchangeCode);
        }
        saveBatch(exchangeCodeList);
    }

    @Override
    public boolean updateExchangeMark(long serialNum, boolean b) {
        Boolean b1 = redisTemplate.opsForValue().setBit(COUPON_CODE_MAP_KEY, serialNum, b);
        return b1!=null && b1;
    }

    @Override
    public PageDTO<ExchangeCodeVO> queryCodeByPage(CodeQuery query) {
        Page<ExchangeCode> page = lambdaQuery().eq(ExchangeCode::getExchangeTargetId, query.getCouponId())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ExchangeCode> records = page.getRecords();
        List<ExchangeCodeVO> vos = BeanUtils.copyList(records, ExchangeCodeVO.class);
        return PageDTO.of(page, vos);
    }
}




