package com.tianji.exam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.learning.LearningClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.exam.QuestionBizDTO;
import com.tianji.api.dto.exam.QuestionDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.exam.domain.dto.ExamRecordDTO;
import com.tianji.exam.domain.dto.ExamSubmissionItem;
import com.tianji.exam.domain.dto.ExamSubmitDTO;
import com.tianji.exam.domain.po.ExamAnswer;
import com.tianji.exam.domain.po.ExamRecord;
import com.tianji.exam.domain.po.Question;
import com.tianji.exam.domain.po.QuestionDetail;
import com.tianji.exam.domain.vo.ExamQuestionVO;
import com.tianji.exam.domain.vo.ExamRecordDetailVO;
import com.tianji.exam.domain.vo.ExamRecordVO;
import com.tianji.exam.domain.vo.QuestionDetailVO;
import com.tianji.exam.repository.ExamRepository;
import com.tianji.exam.service.IExamRecordService;
import com.tianji.exam.service.IQuestionBizService;
import com.tianji.exam.service.IQuestionDetailService;
import com.tianji.exam.service.IQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamRecordServiceImpl implements IExamRecordService {
    private final LearningClient learningClient;
    private final ExamRepository examRepository;
    private final IQuestionService questionService;
    private final IQuestionBizService questionBizService;
    private final IQuestionDetailService questionDetailService;
    private final MongoTemplate mongoTemplate;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    @Override
    public ExamQuestionVO startExam(ExamRecordDTO dto) {
        Long userId = UserContext.getUser();
        Long lessonId = learningClient.isLessonValid(dto.getCourseId());
        if (lessonId == null) {
            throw new BizIllegalException("没有购买该课程");
        }
        ExamRecord examRecord;
        ExamRecord ongoingRecord = examRepository.findByUserIdAndSectionIdAndFinished(userId, dto.getSectionId(), false);
        if (ongoingRecord != null) {
            examRecord = ongoingRecord;
        } else {
            if (dto.getType() == 2) {
                boolean hasFinished = examRepository.existsByUserIdAndSectionIdAndFinished(userId, dto.getSectionId(), true);
                if (hasFinished) {
                    throw new BizIllegalException("您已完成该考试");
                }
            }
            examRecord = createNewRecord(userId, dto);
            examRepository.save(examRecord);
        }
        List<QuestionBizDTO> list = questionBizService.queryQuestionIdsByBizIds(Collections.singletonList(dto.getSectionId()));
        if (CollUtils.isEmpty(list)) {
            throw new BizIllegalException("");
        }
        List<Long> questionIds = list.stream()
                .map(QuestionBizDTO::getQuestionId)
                .collect(Collectors.toList());
        List<QuestionDTO> questionDTOS = questionService.queryQuestionByIds(questionIds);
        if (CollUtils.isEmpty(questionDTOS)) {
            throw new BizIllegalException("");
        }
        ExamQuestionVO vo = new ExamQuestionVO();
        vo.setId(examRecord.getId());
        vo.setQuestions(BeanUtils.copyList(questionDTOS, QuestionDTO.class));
        return vo;
    }
    @Override
    public void submitExam(ExamSubmitDTO dto) {
        Long userId = UserContext.getUser();

        ExamRecord record = examRepository.findById(dto.getId())
                .orElseThrow(() -> new BizIllegalException("考试记录不存在"));
        if(!record.getUserId().equals(userId)){
            throw new BizIllegalException("考试记录不属于您！");
        }
        if(record.getType() == 2 && record.getFinished()){
            throw new BizIllegalException("考试记录已经提交！");
        }
        List<ExamSubmissionItem> answerDetail = dto.getExamDetails();
        if (CollUtils.isEmpty(answerDetail)) {
            throw new BizIllegalException("没有提交答案");
        }
        Map<Long, String> submitAnswerMap = answerDetail.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(ExamSubmissionItem::getQuestionId,
                        item -> item.getAnswer() == null ? "" : item.getAnswer()));
        List<Long> questionIds = answerDetail.stream()
                .map(ExamSubmissionItem::getQuestionId).collect(Collectors.toList());
        List<QuestionDetail> questionDetails = questionDetailService.getBaseMapper().selectBatchIds(questionIds);
        Map<Long, Integer> scoreMap = questionService.getBaseMapper().selectBatchIds(questionIds)
                .stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(Question::getId, Question::getScore));
        if (CollUtils.isEmpty(scoreMap)) {
            throw new BizIllegalException("该考试没有题目和答案");
        }
        if (CollUtils.isEmpty(questionDetails)) {
            throw new BizIllegalException("该考试没有题目和答案");
        }
        Map<Long, QuestionDetail> questionMap = questionDetails.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(QuestionDetail::getId, q -> q));
        double totalScore = 0.0;
        List<ExamAnswer> recordAnswers = new ArrayList<>();
        for (Long questionId : questionIds) {
            QuestionDetail question = questionMap.get(questionId);
            if (question == null) continue;
            String standardAnswer = question.getAnswer();
            String submitAnswer = submitAnswerMap.get(questionId);
            ExamAnswer item = new ExamAnswer();
            item.setQuestionId(questionId);
            item.setAnswer(submitAnswer);
            boolean isCorrect = Objects.equals(standardAnswer, submitAnswer);
            item.setCorrect(isCorrect);
            int score = isCorrect ? scoreMap.get(questionId) : 0;
            item.setScore(score);
            totalScore += score;
            recordAnswers.add(item);
        }
        record.setScore(totalScore);
        record.setSubmitTime(LocalDateTime.now());
        record.setFinished(true);
        record.setAnswers(recordAnswers);
        examRepository.save(record);
        LearningRecordFormDTO recordFormDTO = new LearningRecordFormDTO();

        recordFormDTO.setSectionId(record.getSectionId());
        recordFormDTO.setSectionType(0);
        Long courseId = record.getCourseId();
        Long lessonId = learningClient.isLessonValid(courseId);
        if (lessonId == null) {
            throw new BizIllegalException("该课程没有课表");
        }
        recordFormDTO.setLessonId(lessonId);
        recordFormDTO.setCommitTime(record.getSubmitTime());
        LocalDateTime createTime = record.getCreateTime();
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createTime, now);
        recordFormDTO.setMoment((int) duration.getSeconds());
        learningClient.addLearningRecord(recordFormDTO);
    }

    @Override
    public PageDTO<ExamRecordVO> getExamRecordPage(PageQuery query) {
        Long userId = UserContext.getUser();
        PageRequest pageable =
                PageRequest.of(query.getPageNo() - 1,
                        query.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createTime"));
        Query mongoQuery = new Query();
        mongoQuery.addCriteria(Criteria.where("userId").is(userId));
        long total = mongoTemplate.count(mongoQuery, ExamRecord.class);
        if(total == 0) return PageDTO.empty(0L,0L);
        mongoQuery.with(pageable);
        List<ExamRecord> records = mongoTemplate.find(mongoQuery, ExamRecord.class);
        if (CollUtils.isEmpty(records)) return PageDTO.empty(0L,0L);

        Set<Long> courseIds = records.stream().filter(Objects::nonNull).map(ExamRecord::getCourseId).collect(Collectors.toSet());
        Set<Long> sectionIds = records.stream().filter(Objects::nonNull).map(ExamRecord::getSectionId).collect(Collectors.toSet());
        Map<Long, String> courseMap = new HashMap<>();

        if (CollUtils.isNotEmpty(courseIds)) {
            List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
            if (CollUtils.isNotEmpty(courseInfos)) {
                courseMap = courseInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));
            } else {
                log.error("未查询到课程信息，ID集合: {}", courseIds);
            }
        }

        Map<Long, String> sectionMap = new HashMap<>();
        if (CollUtils.isNotEmpty(sectionIds)) {
            List<CataSimpleInfoDTO> sectionInfos = catalogueClient.batchQueryCatalogue(sectionIds);
            if (CollUtils.isNotEmpty(sectionInfos)) {
                sectionMap = sectionInfos.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
            } else {
                log.error("未查询到章节信息，ID集合: {}", sectionIds);
            }
        }
        ArrayList<ExamRecordVO> list = new ArrayList<>();
        for (ExamRecord record : records) {
            ExamRecordVO vo = BeanUtils.copyBean(record, ExamRecordVO.class);
            if (record.getSubmitTime() != null && record.getCreateTime() != null) {
                Duration duration = Duration.between(record.getCreateTime(), record.getSubmitTime());
                vo.setDuration((int) duration.getSeconds());
            } else {
                vo.setDuration(0);
            }
            vo.setCourseName(courseMap.get(record.getCourseId()));
            vo.setSectionName(sectionMap.get(record.getSectionId()));
            list.add(vo);
        }
        long pages = (total + query.getPageSize() - 1) / query.getPageSize();
        return new PageDTO<>(total, pages, list);
    }

    @Override
    public List<ExamRecordDetailVO> getExamRecordById(String id) {
        ExamRecord examRecord = examRepository.findById(id)
                .orElseThrow(() -> new BizIllegalException("考试记录不存在"));
        List<ExamAnswer> answers = examRecord.getAnswers();
        if(CollUtils.isEmpty(answers)){
            return Collections.emptyList();
        }
        List<Long> questionIds = answers.stream()
                .map(ExamAnswer::getQuestionId).collect(Collectors.toList());
        //根据题目id查询题目的详情信息
        List<QuestionDTO> questionDTOS = questionService.queryQuestionByIds(questionIds);
        if (CollUtils.isEmpty(questionDTOS)) {
            throw new BizIllegalException("题目数据缺失");
        }
        List<QuestionDetail> questionDetails = questionDetailService.lambdaQuery()
                .in(QuestionDetail::getId, questionIds)
                .list();
        if (CollUtils.isEmpty(questionDetails)) {
            throw new BizIllegalException("题目详情数据缺失");
        }
        Map<Long, QuestionDTO> questionMap = questionDTOS.stream()
                .collect(Collectors.toMap(QuestionDTO::getId, q -> q));
        Map<Long, QuestionDetail> detailMap = questionDetails.stream()
                .collect(Collectors.toMap(QuestionDetail::getId, q -> q));
        List<ExamRecordDetailVO> list = new ArrayList<>();
        for (ExamAnswer answer : answers) {
            // 创建外层 VO (学员答题情况)
            ExamRecordDetailVO vo = new ExamRecordDetailVO();
            vo.setCorrect(answer.getCorrect());
            vo.setAnswer(answer.getAnswer());
            vo.setScore(answer.getScore());
            // 创建内层 VO (题目详情)
            QuestionDetailVO questionDetailVO = new QuestionDetailVO();
            questionDetailVO.setId(answer.getQuestionId());
            QuestionDTO questionDTO = questionMap.get(answer.getQuestionId());
            if (questionDTO != null) {
                BeanUtils.copyProperties(questionDTO, questionDetailVO);
            }
            QuestionDetail questionDetail = detailMap.get(answer.getQuestionId());
            if (questionDetail != null) {
                BeanUtils.copyProperties(questionDetail, questionDetailVO);
            }
            vo.setQuestion(questionDetailVO);
            list.add(vo);
        }
        return list;

    }
    private ExamRecord createNewRecord(Long userId, ExamRecordDTO dto) {
        ExamRecord record = new ExamRecord();
        long id = IdWorker.getId();
        record.setId(String.valueOf(id));
        record.setUserId(userId);
        record.setCourseId(dto.getCourseId());
        record.setSectionId(dto.getSectionId());
        record.setCreateTime(LocalDateTime.now());
        record.setFinished(false);
        record.setScore(0.0);
        record.setType(dto.getType());
        return record;
    }
}
