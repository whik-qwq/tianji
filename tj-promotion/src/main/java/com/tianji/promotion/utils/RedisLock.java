package com.tianji.promotion.utils;

import com.tianji.common.utils.BooleanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisLock {

    private final String key;
    private final StringRedisTemplate redisTemplate;
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        String value = Thread.currentThread().getName();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, unit);
        return BooleanUtils.isTrue(success);
    }
    public void unlock(){
        redisTemplate.delete(key);
    }
}
