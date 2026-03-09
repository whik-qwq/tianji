package com.tianji.learning.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "学习笔记表单实体")
public class LearningNoteDTO {

    @NotBlank(message = "笔记内容不能为空")
    @Size(max = 2000, message = "笔记内容最多2000字")
    @ApiModelProperty("笔记内容")
    private String content;

    @ApiModelProperty("是否是隐私笔记：true-私密，false-公开（不填默认公开）")
    private Boolean isPrivate; // 删掉 NotNull，后端逻辑可以给默认值 false

    @Min(value = 0, message = "视频时间点不能小于0")
    @ApiModelProperty("记录笔记时视频播放的时间点，单位：秒（非视频笔记可不填）")
    private Integer noteMoment; // 删掉 NotNull，考虑到以后可能有非视频笔记（如文档笔记）

    @NotNull(message = "课程id不能为空")
    @ApiModelProperty("课程id")
    private Long courseId; // 核心关联字段，必须保留

    @NotNull(message = "章id不能为空")
    @ApiModelProperty("章id")
    private Long chapterId; // 核心关联字段，必须保留

    @NotNull(message = "节id不能为空")
    @ApiModelProperty("节id")
    private Long sectionId; // 核心关联字段，必须保留


}