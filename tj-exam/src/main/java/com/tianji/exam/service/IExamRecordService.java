package com.tianji.exam.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.exam.domain.dto.ExamRecordDTO;
import com.tianji.exam.domain.dto.ExamSubmitDTO;
import com.tianji.exam.domain.vo.ExamQuestionVO;
import com.tianji.exam.domain.vo.ExamRecordDetailVO;
import com.tianji.exam.domain.vo.ExamRecordVO;

import java.util.List;

public interface IExamRecordService {
    ExamQuestionVO startExam(ExamRecordDTO dto);

    void submitExam(ExamSubmitDTO dto);

    PageDTO<ExamRecordVO> getExamRecordPage(PageQuery query);

    List<ExamRecordDetailVO> getExamRecordById(String id);
}
