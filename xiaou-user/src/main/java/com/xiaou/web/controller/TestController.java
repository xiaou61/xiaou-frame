package com.xiaou.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping
    public String test(){
      return "欢迎来到企业级开发框架";
    }
}
