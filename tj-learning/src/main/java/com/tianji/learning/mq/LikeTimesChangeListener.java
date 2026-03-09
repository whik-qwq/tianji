package com.tianji.learning.mq;

import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.InteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.Key.QA_LIKED_TIMES_KEY;

@Component
@RequiredArgsConstructor
public class LikeTimesChangeListener {

    private final InteractionReplyService interactionReplyService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.liked.times.queue",durable = "true"),
            exchange=@Exchange(name = "LIKE_RECORD_EXCHANGE",type = ExchangeTypes.TOPIC),
            key=QA_LIKED_TIMES_KEY
    ))

    public void listenReplyLikedTimesChange(List<LikeTimesDTO> likeTimesDTO){
        List<InteractionReply>list=new ArrayList<>(likeTimesDTO.size());
        for (LikeTimesDTO dto : likeTimesDTO) {
            InteractionReply r = new InteractionReply();
            r.setId(dto.getBizId());
            r.setLikedTimes(dto.getLikeTimes());
            list.add(r);
        }
        interactionReplyService.updateBatchById(list);
    }
}
