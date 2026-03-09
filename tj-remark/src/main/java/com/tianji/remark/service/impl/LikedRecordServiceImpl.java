package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domin.dto.LikeRecordFormDTO;
import com.tianji.remark.domin.po.LikedRecord;
import com.tianji.remark.service.LikedRecordService;
import com.tianji.remark.mapper.LikedRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
* @author xuhe8
* @description 针对表【liked_record(点赞记录表)】的数据库操作Service实现
* @createDate 2026-02-12 21:38:34
*/
@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord>
    implements LikedRecordService{

    private  final RabbitMqHelper mqHelper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
        boolean success = recordDTO.getLiked()? like(recordDTO) : unlike(recordDTO);
        if(!success){
            return;
        }

        Long count = stringRedisTemplate.opsForSet().size(
                RedisConstants.LIKES_BIZ_KEY_PREFIX+recordDTO.getBizId()
        );
        if(count==null){
            return;
        }
        stringRedisTemplate.opsForZSet().add(
                RedisConstants.LIKES_TIMES_KEY_PREFIX+recordDTO.getBizType(),
                recordDTO.getBizId().toString(),
                count
        );
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long userId = UserContext.getUser();

        List<Object> objects = stringRedisTemplate.executePipelined(new RedisCallback<Object>() {

            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                StringRedisConnection src = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    String bizKey = RedisConstants.LIKES_BIZ_KEY_PREFIX + bizId;
                    src.sIsMember(bizKey, userId.toString());
                }
                return null;
            }
        });
        Set<Long> bizIdSet = new HashSet<>();
        for(int i=0;i<objects.size();i++){
            Boolean o =(Boolean)objects.get(i);
            if(o){
                bizIdSet.add(bizIds.get(i));
            }
        }

        return bizIdSet;
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        String key = RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().popMin(key, maxBizSize);
        if (CollUtils.isEmpty(tuples)) {
            return;
        }
        List<LikeTimesDTO> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String bizId = tuple.getValue();
            Double likedTimes = tuple.getScore();
            if(likedTimes==null||bizId==null){
                continue;
            }
            list.add(LikeTimesDTO.of(Long.valueOf(bizId),likedTimes.intValue()));
        }
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, bizType),
                list);
    }

    private boolean unlike(LikeRecordFormDTO recordDTO) {
        Long userId = UserContext.getUser();
        String key= RedisConstants.LIKES_BIZ_KEY_PREFIX+recordDTO.getBizId();
        Long result = stringRedisTemplate.opsForSet().remove(key, userId.toString());
        return result!=null&&result>0;
    }

    private boolean like(LikeRecordFormDTO recordDTO) {
        Long userId = UserContext.getUser();
        String key= RedisConstants.LIKES_BIZ_KEY_PREFIX+recordDTO.getBizId();
        Long result = stringRedisTemplate.opsForSet().add(key, userId.toString());
        return result!=null&&result>0;
    }
}




