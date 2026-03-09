package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.LearningNoteDTO;
import com.tianji.learning.domain.po.LearningNote;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.LearningNoteVO;
import com.tianji.learning.service.LearningNoteService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class LearningNoteController {
    private final LearningNoteService noteService;

    @PostMapping
    @ApiOperation("新增学习笔记")
    public void saveLearningNote(@RequestBody @Valid LearningNoteDTO dto){
        noteService.saveLearningNote(dto);
    }
    @PostMapping("/gathers/{id}")
    @ApiOperation("采集笔记")
    public void gatherNote(@PathVariable Long id){
        noteService.gatherNote(id);
    }
    @PutMapping("/{id}")
    @ApiOperation("修改笔记")
    public void updateNote(@PathVariable Long id,@RequestBody  LearningNoteDTO dto){
        noteService.updateNote(id , dto);
    }
    @DeleteMapping("/{id}")
    @ApiOperation("删除笔记")
    public void deleteNote(@PathVariable Long id){
        noteService.deleteNote(id);
    }
    @GetMapping("/page")
    @ApiOperation("分页查询笔记")
    public PageDTO<LearningNoteVO> queryNotePage(@Valid NotePageQuery query){
        return noteService.queryNotePage(query);
    }

}
