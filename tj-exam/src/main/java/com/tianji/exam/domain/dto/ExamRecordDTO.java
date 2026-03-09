package com.tianji.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "考试记录表单实体")
public class ExamRecordDTO {
    @ApiModelProperty("课程id")
    private Long courseId;
    @ApiModelProperty("小节id")
    private Long sectionId;
    @ApiModelProperty("考试类型")
    private Integer type;
}