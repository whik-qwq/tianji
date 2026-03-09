package com.tianji.learning.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "笔记信息")
public class LearningNoteVO {

    @ApiModelProperty("笔记id")
    private Long id;

    @ApiModelProperty("课程名称")
    private String courseName; // 新增

    @ApiModelProperty("章名称")
    private String chapterName; // 新增

    @ApiModelProperty("节名称")
    private String sectionName; // 新增

    @ApiModelProperty("多级分类名称（拼接格式）")
    private String categoryNames; // 新增

    @ApiModelProperty("笔记内容")
    private String content;

    @ApiModelProperty("记录笔记时的视频时间点，单位：秒")
    private Integer noteMoment;

    @ApiModelProperty("是否在用户端隐藏")
    private Boolean hidden; // 对应数据库 hidden

    @ApiModelProperty("被采集次数")
    private Integer usedTimes; // 新增

    @ApiModelProperty("作者名称")
    private String authorName;

    @ApiModelProperty("作者电话")
    private String authorPhone; // 新增（注意脱敏逻辑）

    @ApiModelProperty("发布/创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd") // 按照要求格式化
    private LocalDateTime createTime;

    @ApiModelProperty("采集人名称集合")
    private List<String> gathers; // 对应数据库字段处理后的集合

    // 保留原有业务字段（根据需要决定是否给前端返回）
    @ApiModelProperty("是否是隐私笔记")
    private Boolean isPrivate;

    @ApiModelProperty("是否是采集的笔记")
    private Boolean isGathered;

    @ApiModelProperty("作者id")
    private Long authorId;

    @ApiModelProperty("作者头像地址")
    private String authorIcon;



}