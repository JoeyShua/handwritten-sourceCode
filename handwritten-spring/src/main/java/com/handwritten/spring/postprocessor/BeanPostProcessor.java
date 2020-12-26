package com.handwritten.spring.postprocessor;

public interface BeanPostProcessor {

    public Object postProcessorBeforeInitization(Object bean ,String beanName);


    public Object postProcessorAfterInitization(Object bean ,String beanName);

}
