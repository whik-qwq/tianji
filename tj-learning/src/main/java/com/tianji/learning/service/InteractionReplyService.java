package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;

/**
* @author xuhe8
* @description 针对表【interaction_reply(互动问题的回答或评论)】的数据库操作Service
* @createDate 2026-02-08 19:37:52
*/
public interface InteractionReplyService extends IService<InteractionReply> {

    void addReply(ReplyDTO replyDTO);

    PageDTO<ReplyVO> queryRepliesPage(ReplyPageQuery replyPageQuery);

    PageDTO<ReplyVO> queryRepliesAdminPage(ReplyPageQuery replyPageQuery);

    void hiddenReplyAdmin(Long id, Boolean hidden);
}
