package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BooleanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;

    @Override
    public SignResultVO addSignRecords() {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        String key= RedisConstants.SIGN_RECORD_KEY_PREFIX+userId
                +now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        int offset = now.getDayOfMonth()-1;
        Boolean exists = redisTemplate.opsForValue().setBit(key, offset, true);
        if(BooleanUtils.isTrue(exists)){
            throw new BizIllegalException("不允许重复签到");
        }
        int signDays = countSignDays(key, now.getDayOfMonth());
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints+1));

        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    @Override
    public Byte[] querySignRecords() {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        String key= RedisConstants.SIGN_RECORD_KEY_PREFIX+userId
                +now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return new Byte[0];
        }
        int num = result.get(0).intValue();
        int offset = now.getDayOfMonth() - 1;
        // 利用& 封装结构
        Byte[] arr = new Byte[dayOfMonth];
        while (offset >= 0) {
            arr[offset] = (byte) (num & 1);// 计算最后一天是否签到 赋值结果
            offset--;
            num = num >>> 1;
        }
        return arr;
    }

    private int countSignDays(String key, int len) {
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(len)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return 0;
        }
        int num = result.get(0).intValue();
        int count = 0;
        while ((num & 1) == 1) {
            count++;
            num >>>= 1;
        }
        return count;
    }
}
