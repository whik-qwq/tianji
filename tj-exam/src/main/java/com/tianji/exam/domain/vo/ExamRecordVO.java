package com.tianji.exam.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamRecordVO {
    @ApiModelProperty("考试记录id")
    private Long id;

    @ApiModelProperty("考试类型：1-练习，2-考试")
    private Integer type;

    @ApiModelProperty("得分")
    private Double score; // 之前你的实体类是 Double，这里保持一致

    @ApiModelProperty("提交时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 自动格式化时间
    @JsonProperty("commitTime")
    private LocalDateTime submitTime;

    @ApiModelProperty("考试用时")
    private Integer duration;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("小节名称")
    private String sectionName;
}