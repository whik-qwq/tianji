package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.NoteAdminPageQuery;
import com.tianji.learning.domain.vo.LearningNoteVO;
import com.tianji.learning.service.LearningNoteService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin/notes")
@RequiredArgsConstructor
public class LearningNoteAdminController {
    private LearningNoteService noteService;

    @GetMapping("/page")
    @ApiOperation("管理端分页查询笔记")
    public PageDTO<LearningNoteVO> queryNotePage(@Valid NoteAdminPageQuery query){
        return noteService.queryAdminNotePage(query);
    }
    @GetMapping("/{id}")
    @ApiOperation("管理端查询笔记详情")
    public LearningNoteVO queryNoteDetail(@PathVariable Long id){
        return noteService.queryNoteDetail(id);
    }

    @PutMapping("/{id}/hidden/{hidden}")
    @ApiOperation("管理端修改笔记的隐藏状态")
    public void updateNoteHidden(@PathVariable Long id ,@PathVariable Boolean hidden){
        noteService.updateNoteHidden(id,hidden);
    }
}
