package com.tianji.exam.domain.dto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "考试交卷提交数据")
public class ExamSubmitDTO {

    // 对应 JSON 中的 "id": "12351412312141"
    // 因为 MongoDB 的主键通常是 String 类型，且前端传过来的也是 String
    @ApiModelProperty(value = "考试记录id", required = true, example = "6564eb06d8b...")
    private String id;

    // 对应 JSON 中的 "examDetails":Array
    @ApiModelProperty(value = "答题详情列表", required = true)
    private List<ExamSubmissionItem> examDetails;

}