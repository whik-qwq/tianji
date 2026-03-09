package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.InteractionReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/replies")
public class InteractionReplyController {

    @Autowired
    InteractionReplyService interactionReplyService;

    @PostMapping()
    public void addReply(@RequestBody ReplyDTO replyDTO) {
        interactionReplyService.addReply(replyDTO);
    }

    @GetMapping("page")
    public PageDTO<ReplyVO> queryRepliesPage(@RequestBody ReplyPageQuery replyPageQuery) {
        return interactionReplyService.queryRepliesPage(replyPageQuery);
    }
}
