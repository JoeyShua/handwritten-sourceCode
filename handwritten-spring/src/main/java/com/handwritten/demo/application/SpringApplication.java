package com.handwritten.demo.application;

import com.handwritten.demo.service.UserService;
import com.handwritten.spring.application.MyAnnotationApplicationContext;
import com.handwritten.spring.config.AppConfig;

public class SpringApplication {

    public static void main(String[] args) {

        //启动spring  加载非懒加载单例bean
        MyAnnotationApplicationContext myAnnotationApplicationContext = new MyAnnotationApplicationContext(AppConfig.class);

        //加载 懒加载  原型 bean

        UserService userService = (UserService) myAnnotationApplicationContext.getBean("userService");
        userService.test();
    }
}
