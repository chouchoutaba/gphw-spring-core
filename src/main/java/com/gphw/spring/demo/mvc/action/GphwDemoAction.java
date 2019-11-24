package com.gphw.spring.demo.mvc.action;

import com.gphw.spring.demo.service.IGphwDemoService;
import com.gphw.spring.framework.annotation.GphwAutowired;
import com.gphw.spring.framework.annotation.GphwController;
import com.gphw.spring.framework.annotation.GphwRequestMapping;
import com.gphw.spring.framework.annotation.GphwRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@GphwController
@GphwRequestMapping("/demo")
public class GphwDemoAction {

    @GphwAutowired
    private IGphwDemoService demoService;

    @GphwRequestMapping("/hello")
    public void sayHello(HttpServletRequest req, HttpServletResponse resp,
                           @GphwRequestParam("name") String name){
        String result = demoService.sayHello(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GphwRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                         @GphwRequestParam("name") String name){
        String result = demoService.sayHello(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
