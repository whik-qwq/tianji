package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;

/**
* @author xuhe8
* @description 针对表【exchange_code(兑换码)】的数据库操作Service
* @createDate 2026-02-24 23:00:53
*/
public interface ExchangeCodeService extends IService<ExchangeCode> {

    void asyncGenerateCode(Coupon coupon);

    boolean updateExchangeMark(long serialNum, boolean b);

    PageDTO<ExchangeCodeVO> queryCodeByPage(CodeQuery query);
}
