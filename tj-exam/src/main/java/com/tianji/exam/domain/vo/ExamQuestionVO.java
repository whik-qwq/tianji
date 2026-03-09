package com.tianji.exam.domain.vo;

import com.tianji.api.dto.exam.QuestionDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "考试试卷及题目数据")
public class ExamQuestionVO {
    // value = 字段说明, example = 示例值
    @ApiModelProperty(value = "考试记录id", example = "6564eb06d8...")
    private String id;

    @ApiModelProperty(value = "题目列表")
    private List<QuestionDTO> questions;

}

