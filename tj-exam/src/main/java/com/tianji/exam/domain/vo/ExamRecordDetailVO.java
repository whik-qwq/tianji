package com.tianji.exam.domain.vo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "考试记录详情(含题目信息)")
public class ExamRecordDetailVO {

    @ApiModelProperty("学员答案")
    private String answer;
    @ApiModelProperty("老师评语")
    private String comment;

    @ApiModelProperty("是否正确")
    private Boolean correct;

    @ApiModelProperty("学员得分")
    private Integer score;
    @ApiModelProperty("题目详细信息")
    private QuestionDetailVO question;

}