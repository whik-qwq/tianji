package com.tianji.promotion.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;
import com.tianji.promotion.service.ExchangeCodeService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/codes")
@Api(tags = "优惠券相关接口")
public class ExchangeCodeController {
    private final ExchangeCodeService exchangeCodeService;

    @GetMapping("/page")
    public PageDTO<ExchangeCodeVO> queryCodeByPage(CodeQuery query){return  exchangeCodeService.queryCodeByPage(query);}
}
