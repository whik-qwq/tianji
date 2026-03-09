package com.tianji.learning.domain.query;


import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "管理端笔记分页查询参数")
public class NoteAdminPageQuery extends PageQuery {

    @ApiModelProperty("课程名称关键字")
    private String name; // 对应 JSON 中的课程名称模糊搜索

    @ApiModelProperty("笔记状态：false-显示，true-隐藏")
    private Boolean hidden; // 对应数据库 is_hidden 字段

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间区间的开始时间")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间区间的结束时间")
    private LocalDateTime endTime;
}