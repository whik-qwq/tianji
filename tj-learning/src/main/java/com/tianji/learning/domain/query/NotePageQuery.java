package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "笔记分页查询")
public class NotePageQuery extends PageQuery {
    @ApiModelProperty("课程id")
    private Long courseId;

    @ApiModelProperty("小节id")
    private Long sectionId;

    @ApiModelProperty("是否只看我的笔记：true-是，false-看全部")
    @NotNull
    private Boolean onlyMine;

}