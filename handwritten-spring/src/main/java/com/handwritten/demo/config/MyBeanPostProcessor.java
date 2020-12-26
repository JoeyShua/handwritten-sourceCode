package com.handwritten.demo.config;

import com.handwritten.spring.annotation.Component;
import com.handwritten.spring.postprocessor.BeanPostProcessor;

@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    public Object postProcessorBeforeInitization(Object bean, String beanName) {
        //System.out.println("bean实例化之前");
        return bean;
    }

    public Object postProcessorAfterInitization(Object bean, String beanName) {
        //System.out.println("bean实例化之后");
        return bean;
    }
}
