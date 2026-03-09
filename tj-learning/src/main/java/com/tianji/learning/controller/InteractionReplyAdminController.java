package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.InteractionReplyService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/replies")
public class InteractionReplyAdminController {
    @Autowired
    private InteractionReplyService interactionReplyService;

    @GetMapping("page")
    public PageDTO<ReplyVO> queryRepliesPage(@RequestBody ReplyPageQuery replyPageQuery) {
        return interactionReplyService.queryRepliesAdminPage(replyPageQuery);
    }

    @ApiOperation("隐藏显示回答或评论")
    @PutMapping("{id}/hidden/{hidden}")
    public void hiddenReplyAdmin(@PathVariable("id") Long id, @PathVariable("hidden") Boolean hidden){
        interactionReplyService.hiddenReplyAdmin(id, hidden);
    }

}
