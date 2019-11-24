package com.gphw.spring.demo.service;

import com.gphw.spring.framework.annotation.GphwService;

@GphwService
public class GphwDemoService implements IGphwDemoService{
    @Override
    public String sayHello(String name) {
        return "Hello!"+name;
    }
}
