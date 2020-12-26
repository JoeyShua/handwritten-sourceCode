package com.handwritten.demo.service.impl;

import com.handwritten.demo.entity.User;
import com.handwritten.demo.service.UserService;
import com.handwritten.spring.annotation.Autowired;
import com.handwritten.spring.annotation.Component;
import com.handwritten.spring.annotation.Scop;
import com.handwritten.spring.aware.BeanNameAware;

@Component("userService")
@Scop("prototype")
public class UserServiceImpl implements UserService, BeanNameAware {

    @Autowired
    private User user;

    private String beanName;

    public void test() {
        System.out.println(user);
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
