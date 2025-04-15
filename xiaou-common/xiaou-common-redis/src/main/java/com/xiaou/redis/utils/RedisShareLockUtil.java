package com.xiaou.redis.utils;

import cn.hutool.core.util.StrUtil;
import com.xiaou.redis.exception.ShareLockException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisShareLockUtil {
    @Resource
    private RedisUtils redisUtils;

    private final Long TIME_OUT = 10000L;


    /**
     * 加锁
     *
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */
    public boolean lock(String lockKey, String requestId, Long expireTime) {
        //1.参数的校验
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(requestId) || expireTime < 0) {
            throw new ShareLockException("加锁参数异常");
        }
        long currentTime = System.currentTimeMillis();
        long outTime = currentTime + TIME_OUT;
        Boolean result = false;
        //2.加锁可以自旋
        while (currentTime < outTime) {
            //3.借助redis的setnx命令
            result = redisUtils.setNx(lockKey, requestId, expireTime);
            if (result) {
                return result;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            currentTime = System.currentTimeMillis();
        }
        return result;
    }


    public boolean unlock(String key, String requestId) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(requestId)) {
            throw new ShareLockException("解锁参数异常");
        }
        try {
            String value = redisUtils.get(key);
            if (requestId.equals(value)) {
                redisUtils.del(key);
                return true;
            }
        } catch (Exception e) {
            throw new ShareLockException("解锁异常");
        }
        return false;
    }

    public boolean tryLock(String lockKey, String requestId, Long expireTime) {
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(requestId) || expireTime < 0) {
            throw new ShareLockException("加锁参数异常");
        }
        return redisUtils.setNx(lockKey, requestId, expireTime);
    }
}
