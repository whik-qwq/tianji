package com.tianji.promotion.domain.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel(description = "用户端优惠券信息")
public class ExchangeCodeVO {
    private Integer id;
    private String code;
}
