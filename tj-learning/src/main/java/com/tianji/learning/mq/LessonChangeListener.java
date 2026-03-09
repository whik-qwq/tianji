package com.tianji.learning.mq;

import cn.hutool.core.collection.CollUtil;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.service.LearningLessonService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LessonChangeListener {

    @Autowired
    private LearningLessonService learningLessonService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),
            exchange = @Exchange( name=MqConstants.Exchange.ORDER_EXCHANGE,type= ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void listenLessonPay(OrderBasicDTO order) {
        if(order==null||order.getUserId()==null|| CollUtil.isEmpty(order.getCourseIds())){
            log.error("Mq message is null");
            return;
        }
        learningLessonService.addUserLessons(order.getUserId(),order.getCourseIds());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.delete.queue",durable = "true"),
            exchange = @Exchange( name=MqConstants.Exchange.ORDER_EXCHANGE,type= ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void listenLessonDelete(OrderBasicDTO order) {
        if(order==null||order.getUserId()==null|| CollUtil.isEmpty(order.getCourseIds())){
            log.error("Mq message is null");
            return;
        }
        order.getCourseIds().forEach(courseId -> learningLessonService.deleteUserLessons(order.getUserId(),courseId));
    }
}