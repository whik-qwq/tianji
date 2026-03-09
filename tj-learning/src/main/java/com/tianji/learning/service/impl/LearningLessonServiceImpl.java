package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CatalogueDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.AssertUtils;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.LearningLessonService;
import com.tianji.learning.mapper.LearningLessonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author xuhe8
* @description 针对表【learning_lesson(学生课程表)】的数据库操作Service实现
* @createDate 2026-01-27 14:50:14
*/
@Service
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson>
    implements LearningLessonService{

    @Autowired
    private CourseClient courseClient;

    @Autowired
    private CatalogueClient catalogueClient;

    @Autowired
    private LearningRecordMapper learningRecordMapper;

    @Override
    @Transactional()
    public void addUserLessons(Long userId, List<Long> courseIds) {
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(simpleInfoList)){
            return;
        }
        List<LearningLesson> list= new ArrayList<LearningLesson>(simpleInfoList.size());
        for (CourseSimpleInfoDTO simpleInfo : simpleInfoList) {
            LearningLesson learningLesson = new LearningLesson();

            Integer validDuration = simpleInfo.getValidDuration();
            if(validDuration != null&&validDuration>0){
                LocalDateTime now = LocalDateTime.now();
                learningLesson.setCreateTime(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
                learningLesson.setExpireTime(Date.from(now.plusMonths(validDuration).atZone(ZoneId.systemDefault()).toInstant()));
            }
            learningLesson.setUserId(userId);
            learningLesson.setCourseId(simpleInfo.getId());
            list.add(learningLesson);
        }

        saveBatch(list);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
        Long userId = UserContext.getUser();
        Page<LearningLesson> page = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .page(pageQuery.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();

        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }

        Map<Long, CourseSimpleInfoDTO> cmap = getCourseSimpleInfoDTOMap(records);

        List<LearningLessonVO> voList = new ArrayList<>(records.size());
        for (LearningLesson learningLesson : records) {
           LearningLessonVO learningLessonVO = BeanUtils.copyBean(learningLesson, LearningLessonVO.class);
            CourseSimpleInfoDTO cInfo = cmap.get(learningLesson.getCourseId());
            learningLessonVO.setCourseName(cInfo.getName());
            learningLessonVO.setCourseCoverUrl(cInfo.getCoverUrl());
            learningLessonVO.setSections(cInfo.getSectionNum());
            voList.add(learningLessonVO);
        }
        return PageDTO.of(page, voList);
    }

    private Map<Long, CourseSimpleInfoDTO> getCourseSimpleInfoDTOMap(List<LearningLesson> records) {
        Set<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);

        if(CollUtils.isEmpty(simpleInfoList)){
            throw new BadRequestException("LearningLesson don't exist");
        }

        Map<Long, CourseSimpleInfoDTO> cmap =
                simpleInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        return cmap;
    }

    @Override
    public void deleteUserLessons(Long userId, Long courseId) {
        QueryWrapper<LearningLesson> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("course_id", courseId);
        remove(wrapper);
    }

    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        Long userId = UserContext.getUser();
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum());
        // 5.统计课表中的课程数量 select count(1) from xxx where user_id = #{userId}
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(courseAmount);
        // 6.查询小节信息
        List<CataSimpleInfoDTO> cataInfos =
                catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataInfos)) {
            CataSimpleInfoDTO cataInfo = cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;


    }

    @Override
    public Long isLessonValid(Long courseId) {
        LearningLesson lesson = lambdaQuery().eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, courseId).one();
        if(lesson == null){
            return null;
        }
        Date expireTime = lesson.getExpireTime();
        Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());;
        if (expireTime != null && now.after(expireTime)) {
            return null;
        }
        return lesson.getId();
    }

    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        LearningLessonVO vo = BeanUtils.copyProperties(lesson, LearningLessonVO.class);
        return vo;
    }

    @GetMapping("/lessons/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
        return lambdaQuery().eq(LearningLesson::getCourseId, courseId).count();
    }

    @Override
    public LearningLesson queryByUserIdAndCourseId(Long userId, Long courseId) {
        return lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
    }

    @Override
    public void createLearningPlans(LearningPlanDTO learningPlanDTO) {
        Long userId = UserContext.getUser();
        LearningLesson learninglesson = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, learningPlanDTO.getCourseId())
                .one();

        LearningLesson l=new LearningLesson();
        l.setId(learninglesson.getId());
        l.setWeekFreq(learningPlanDTO.getFreq());
        l.setPlanStatus(PlanStatus.PLAN_RUNNING);
        updateById(l);
    }

    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery pageQuery) {
        LearningPlanPageVO result = new LearningPlanPageVO();

        Long userId = UserContext.getUser();
        Integer finished = learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId).eq(LearningRecord::getFinished, true));
        result.setWeekFinished(finished);

        Integer weekTotalPlan= getBaseMapper().queryTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);

        Page<LearningLesson> page = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(pageQuery.toMpPage());
        List<LearningLesson> records= page.getRecords();
        if(CollUtils.isEmpty(records)) {
            return result;
        }

        Map<Long, CourseSimpleInfoDTO> cmap = getCourseSimpleInfoDTOMap(records);

        List<IdAndNumDTO> list=learningRecordMapper.countLearnedSections(userId);
        Map<Long, Integer> countMap = IdAndNumDTO.toMap(list);

        List<LearningPlanVO> voList=new ArrayList<>(records.size());
        for (LearningLesson l : records) {
            LearningPlanVO learningPlanVO = BeanUtils.copyProperties(l, LearningPlanVO.class);
            CourseSimpleInfoDTO cInfo = cmap.get(l.getCourseId());
            if (cInfo != null) {
                learningPlanVO.setCourseName(cInfo.getName());
                learningPlanVO.setSections(cInfo.getSectionNum());
            }

            learningPlanVO.setWeekLearnedSections(countMap.getOrDefault(l.getId(), 0));
            voList.add(learningPlanVO);
        }
        return result.pageInfo(page.getTotal(), page.getPages(), voList);
    }
}




