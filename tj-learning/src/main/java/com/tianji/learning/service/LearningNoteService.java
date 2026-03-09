package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.LearningNoteDTO;
import com.tianji.learning.domain.po.LearningNote;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.NoteAdminPageQuery;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.LearningNoteVO;

import javax.validation.Valid;

/**
* @author xuhe8
* @description 针对表【learning_note(学习笔记表)】的数据库操作Service
* @createDate 2026-03-04 08:59:24
*/
public interface LearningNoteService extends IService<LearningNote> {

    void saveLearningNote(@Valid LearningNoteDTO dto);

    void gatherNote(Long id);

    void updateNote(Long id, LearningNoteDTO dto);

    void deleteNote(Long id);

    PageDTO<LearningNoteVO> queryNotePage(@Valid NotePageQuery query);

    PageDTO<LearningNoteVO> queryAdminNotePage(@Valid NoteAdminPageQuery query);

    LearningNoteVO queryNoteDetail(Long id);

    void updateNoteHidden(Long id, Boolean hidden);
}
