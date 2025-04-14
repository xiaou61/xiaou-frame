package com.xiaou.redis.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InitCache implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println("初始化缓存");
        //要知道那些缓存需要进行一个预热
        //调用init方法
    }
}
