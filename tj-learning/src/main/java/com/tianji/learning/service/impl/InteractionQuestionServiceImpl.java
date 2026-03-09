package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.InteractionQuestionService;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.InteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author xuhe8
* @description 针对表【interaction_question(互动提问的问题表)】的数据库操作Service实现
* @createDate 2026-02-08 19:37:52
*/
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion>
    implements InteractionQuestionService{

    private final  InteractionReplyService replyService;
    private final  UserClient userClient;
    private final  CourseClient courseClient;
    private final  SearchClient searchClient;
    private final  CategoryClient categoryClient;
    private final  CategoryCache  categoryCache;
    private final  CatalogueClient catalogueClient;
    private final  InteractionReplyService interactionReplyService;
    private final  InteractionQuestionMapper interactionQuestionMapper;
    private final RabbitMqHelper rabbitMqHelper;

    @Override
    public void saveQuestion(QuestionFormDTO questionFormDTO) {
        Long userId = UserContext.getUser();
        InteractionQuestion question = BeanUtils.copyBean(questionFormDTO, InteractionQuestion.class);
        question.setUserId(userId);
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.WRITE_REPLY,
                SignInMessage.of(userId, null));
        save(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if(courseId == null && sectionId == null){
            throw new BadRequestException("");
        }
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class,info -> !info.getProperty().equals("description"))
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();

        for (InteractionQuestion q : records) {
            if(!q.getAnonymity()) {
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        answerIds.remove(null);
        Map<Long,InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if(CollUtils.isNotEmpty(answerIds)){
            List<InteractionReply> replies = replyService.listByIds(answerIds);
            for(InteractionReply reply : replies){
                replyMap.put(reply.getId(), reply);
                if(!reply.getAnonymity()){
                    userIds.add(reply.getUserId());
                }
            }
        }
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        List<QuestionVO> questionVOS = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            QuestionVO questionVO = BeanUtils.copyBean(r, QuestionVO.class);
            questionVOS.add(questionVO);

            if(!r.getAnonymity()){
                UserDTO userDTO = userMap.get(r.getUserId());
                if(userDTO != null){
                    questionVO.setUserName(userDTO.getName());
                    questionVO.setUserIcon(userDTO.getIcon());
                }
            }

            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if(reply != null){
                questionVO.setLatestReplyContent(reply.getContent());
                if(!reply.getAnonymity()){
                    UserDTO user = userMap.get(reply.getUserId());
                    questionVO.setUserName(user.getName());
                }
            }
        }
        return PageDTO.of(page, questionVOS);
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        InteractionQuestion question = getById(id);
        if(question == null || question.getHidden()){
            return null;
        }
        UserDTO user=null;
        if(!question.getAnonymity()){
             user = userClient.queryUserById(question.getUserId());
        }
        QuestionVO questionVO = BeanUtils.copyBean(question, QuestionVO.class);
        if(user != null){
            questionVO.setUserName(user.getName());
            questionVO.setUserIcon(user.getIcon());
        }
        return questionVO;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        List<Long> courseIds=null;
        if(StringUtils.isNotBlank(query.getCourseName())){
             courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if(CollUtils.isEmpty(courseIds)){
                return PageDTO.empty(0L,0L);
            }
        }
        Integer status = query.getStatus();
        LocalDateTime beginTime = query.getBeginTime();
        LocalDateTime endTime = query.getEndTime();

        Page<InteractionQuestion> page = lambdaQuery().in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(beginTime != null, InteractionQuestion::getCreateTime, beginTime)
                .lt(endTime != null, InteractionQuestion::getCreateTime, endTime)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        Set<Long> cataIds = new HashSet<>();

        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }

        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long,UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)){
            userMap=users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(cIds);
        Map<Long,CourseSimpleInfoDTO> cinfoMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(cinfos)){
            cinfoMap=cinfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }

        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }

        List<QuestionAdminVO> questionAdminVOS = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            QuestionAdminVO questionAdminVO = BeanUtils.copyBean(r, QuestionAdminVO.class);
            questionAdminVOS.add(questionAdminVO);

            UserDTO userDTO = userMap.get(r.getUserId());
            if(userDTO != null){
                questionAdminVO.setUserName(userDTO.getName());
            }

            CourseSimpleInfoDTO courseSimpleInfoDTO = cinfoMap.get(r.getCourseId());
            if(courseSimpleInfoDTO != null){
                questionAdminVO.setCourseName(courseSimpleInfoDTO.getName());
                questionAdminVO.setCategoryName(categoryCache.getCategoryNames(courseSimpleInfoDTO.getCategoryIds()));
            }
            questionAdminVO.setChapterName(cataMap.getOrDefault(r.getChapterId(), ""));
            questionAdminVO.setSectionName(cataMap.getOrDefault(r.getSectionId(), ""));
        }

        return PageDTO.of(page, questionAdminVOS);
    }

    @Override
    public void updateQuestion(Long id, QuestionFormDTO questionFormDTO) {
        InteractionQuestion question = BeanUtils.copyBean(questionFormDTO, InteractionQuestion.class);
        question.setId(id);
        updateById(question);
    }

    @Override
    public void deleteQuestionById(Long id) {
        Long userId = UserContext.getUser();
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BizIllegalException("问题不存在");
        }
        if (!userId.equals(question.getUserId())) {
            throw new BizIllegalException("该问题提问者非当前用户，无法删除");
        }
        removeById(id);
        LambdaQueryWrapper<InteractionReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionReply::getQuestionId, question.getId());
        replyService.remove(wrapper);
    }

    @Override
    public void hiddenQuestion(Long id, Boolean hidden) {
        interactionQuestionMapper.hiddenQuestion(id,hidden);
    }

    @Override
    public QuestionAdminVO adminQueryQuestionById(Long id) {

        InteractionQuestion question = lambdaQuery().eq(InteractionQuestion::getId, id).one();
        QuestionAdminVO questionAdminVO = BeanUtils.copyBean(question, QuestionAdminVO.class);

        UserDTO userDTO = userClient.queryUserById(question.getUserId());
        questionAdminVO.setUserName(userDTO.getName());
        questionAdminVO.setUserIcon(userDTO.getIcon());


        CourseFullInfoDTO courseInfoById =
                courseClient.getCourseInfoById(question.getCourseId(),true,true);
        questionAdminVO.setCourseName(courseInfoById.getName());
        questionAdminVO.setCategoryName(categoryCache.getCategoryNames(courseInfoById.getCategoryIds()));

        List<Long> teacherIds = courseInfoById.getTeacherIds();
        List<UserDTO> teacherDto = userClient.queryUserByIds(teacherIds);
        questionAdminVO.setTeacherName(teacherDto.stream().map(UserDTO::getName)
                .collect(Collectors.joining(",")));

        Set<Long> chapterAndSectionIds = new HashSet<>(); //章和节id集合
        chapterAndSectionIds.add(question.getChapterId());
        chapterAndSectionIds.add(question.getSectionId());
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(chapterAndSectionIds);
        if (CollUtils.isEmpty(cataSimpleInfoDTOS)) {
            throw new BizIllegalException("章和节不存在！");
        }
        Map<Long, String> cataSimpleInfoDTOMap = cataSimpleInfoDTOS.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, c -> c.getName()));
        questionAdminVO.setChapterName(cataSimpleInfoDTOMap.get(question.getChapterId())); //章名称
        questionAdminVO.setSectionName(cataSimpleInfoDTOMap.get(question.getSectionId())); //节名称

        question.setStatus(QuestionStatus.CHECKED);
        this.updateById(question);
        return null;
    }
}




