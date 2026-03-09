package com.tianji.promotion.utils;

import com.tianji.common.exceptions.BizIllegalException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
@Aspect
@RequiredArgsConstructor
public class MyLocKAspect implements Ordered {

    private final MyLockFactory lockFactory;

    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint pjp,MyLock myLock) throws Throwable {

        RLock lock = lockFactory.getLock(myLock.lockType(), myLock.name());
        boolean isLock = myLock.lockStrategy().tryLock(lock,myLock);
        if(!isLock){
            return null;
        }
        try {
            return pjp.proceed();
        }  finally {
            lock.unlock();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
