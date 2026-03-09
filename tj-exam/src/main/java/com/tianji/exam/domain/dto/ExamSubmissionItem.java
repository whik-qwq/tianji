package com.tianji.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "题目答案详情")
public class ExamSubmissionItem {
    // 对应 JSON 中的 "questionId"
    @ApiModelProperty(value = "题目id", example = "16758457283457")
    private Long questionId;

    // 对应 JSON 中的 "questionType"
    @ApiModelProperty(value = "题目类型(1:单选, 2:多选, 3:判断)", example = "1")
    private Integer questionType;

    // 对应 JSON 中的 "answer"
    @ApiModelProperty(value = "题目答案(多选逗号分隔)", example = "A")
    private String answer;

}