package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.InteractionQuestionService;
import com.tianji.learning.service.InteractionReplyService;
import com.tianji.learning.mapper.InteractionReplyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
* @author xuhe8
* @description 针对表【interaction_reply(互动问题的回答或评论)】的数据库操作Service实现
* @createDate 2026-02-08 19:37:52
*/
@Service
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply>
    implements InteractionReplyService{

    @Autowired
    private InteractionQuestionMapper interactionQuestionMapper;
    @Autowired
    private UserClient userClient;


    @Override
    public void addReply(ReplyDTO replyDTO) {
        InteractionReply reply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        reply.setUserId(UserContext.getUser());
        save(reply);
        InteractionQuestion question = interactionQuestionMapper.selectById(reply.getQuestionId());
        if(reply.getAnswerId() == null){

            question.setLatestAnswerId(reply.getAnswerId());
            question.setAnswerTimes(question.getAnswerTimes()+1);
            interactionQuestionMapper.updateById(question);
        }else{
            InteractionReply answer = getById(reply.getAnswerId());
            answer.setReplyTimes(answer.getReplyTimes()+1);
            updateById(answer);
        }

        Boolean isStudent = replyDTO.getIsStudent();
        if (isStudent) {
            // dto.isStudent为true 则代表学生提交 如果是则将问题表中该问题的status字段改为未查看
            question.setStatus(QuestionStatus.UN_CHECK);
        }
        interactionQuestionMapper.updateById(question);
    }

    @Override
    public PageDTO<ReplyVO> queryRepliesPage(ReplyPageQuery query) {
        if (query.getQuestionId() == null && query.getAnswerId() == null) {
            throw new BadRequestException("问题id和回答id不能都为空");
        }
        Page<InteractionReply> page = this.lambdaQuery()
                .eq(query.getQuestionId() != null, InteractionReply::getQuestionId, query.getQuestionId())
                .eq(InteractionReply::getAnswerId, query.getAnswerId() == null ? 0L : query.getAnswerId())
                .eq(InteractionReply::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());

        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(0L, 0L);
        }
        Set<Long> uids = new HashSet<>();
        Set<Long> targetReplyIds = new HashSet<>();
        for (InteractionReply record : records) {
            if (!record.getAnonymity()) {
                uids.add(record.getUserId());
                uids.add(record.getTargetUserId());
            }
            if (record.getTargetReplyId() != null && record.getTargetReplyId() > 0) {
                targetReplyIds.add(record.getTargetReplyId());
            }
        }


        if (targetReplyIds.size() > 0) {
            List<InteractionReply> targetReplies = listByIds(targetReplyIds);
            Set<Long> targetUserIds = targetReplies.stream()
                    .filter(Predicate.not(InteractionReply::getAnonymity))
                    .map(InteractionReply::getUserId)
                    .collect(Collectors.toSet());
            uids.addAll(targetUserIds);
        }

        List<UserDTO> userDTOList = userClient.queryUserByIds(uids);
        Map<Long, UserDTO> userDTOMap = new HashMap<>();
        if (userDTOList != null) {
            userDTOMap = userDTOList.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        }
        List<ReplyVO> voList = new ArrayList<>();
        for (InteractionReply record : records) {
            ReplyVO vo = BeanUtils.copyBean(record, ReplyVO.class);
            if (!record.getAnonymity()) {
                UserDTO userDTO = userDTOMap.get(record.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                    vo.setUserType(userDTO.getType());
                }
            }
            UserDTO targetUserDTO = userDTOMap.get(record.getTargetReplyId());
            if (targetUserDTO != null) {
                vo.setTargetUserName(targetUserDTO.getName());
            }
            voList.add(vo);
        }
        return PageDTO.of(page, voList);
    }

    @Override
    public PageDTO<ReplyVO> queryRepliesAdminPage(ReplyPageQuery replyPageQuery) {
        return queryRepliesPage(replyPageQuery);
    }

    @Override
    public void hiddenReplyAdmin(Long id, Boolean hidden) {
        InteractionReply reply = this.lambdaQuery()
                .eq(InteractionReply::getId, id).one();
        if (reply == null || hidden == null) {
            throw new BadRequestException("该回答或评论不存在");
        }
        reply.setHidden(hidden);
        this.updateById(reply);

        int num = hidden ? -1 : 1;

        if (reply.getAnswerId() == 0) {
            InteractionReply answer = this.lambdaQuery().eq(InteractionReply::getAnswerId, id).one();
            answer.setReplyTimes(answer.getReplyTimes() + num);
            return;
        }

        InteractionQuestion question = interactionQuestionMapper.selectById(reply.getQuestionId());
        question.setAnswerTimes(question.getAnswerTimes() + num);
        interactionQuestionMapper.updateById(question);
    }
}




