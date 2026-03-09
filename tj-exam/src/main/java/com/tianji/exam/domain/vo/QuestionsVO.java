package com.tianji.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
@Data
@ApiModel(description = "题目详情信息")
public class QuestionsVO {

    @ApiModelProperty(value = "题目ID", example = "101")
    private Long id;

    @ApiModelProperty(value = "题目名称/题干", example = "Java中int占用几个字节？")
    private String name;

    @ApiModelProperty(value = "选项列表", example = "[\"A. 1个\", \"B. 4个\"]")
    private List<String> options;

    @ApiModelProperty(value = "题目类型 1:单选, 2:多选, 3:判断", example = "1")
    private Integer type;

    @ApiModelProperty(value = "难易度 1:简单 2:中等 3:困难", example = "1")
    private Integer difficulty;

    @ApiModelProperty(value = "本题分值", example = "5")
    private Integer score;
}