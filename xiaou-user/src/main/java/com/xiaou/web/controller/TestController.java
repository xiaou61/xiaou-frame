package com.xiaou.web.controller;

import com.xiaou.redis.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Resource
    private RedisUtils redisUtils;

    @GetMapping
    public String test() {
        return "欢迎来到企业级开发框架";
    }

    @GetMapping("/test/redis")
    public void testRedis() {
        redisUtils.set("test", "test");
    }

}
