package com.handwritten.spring.application;

import com.handwritten.spring.annotation.*;
import com.handwritten.spring.aware.BeanNameAware;
import com.handwritten.spring.aware.InitializingBean;
import com.handwritten.spring.postprocessor.BeanPostProcessor;
import com.handwritten.spring.vo.BeanDefinition;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MyAnnotationApplicationContext {

    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();
    //单例池
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>();
    //
    private List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<BeanPostProcessor>();

    private Class clazz;

    public MyAnnotationApplicationContext(Class clazz) {
        this.clazz = clazz;

        //扫描 得到class列表
        List<Class> classList = scan(clazz);

        //解析这些类 生成beanDefinition
        generalBeanDefinitionMap(classList);

        //基于beanDefinitionMap 创建单例bean
        instanceSingletonBean();

    }

    /**
     * 生成beanDefinition
     */
    private void generalBeanDefinitionMap(List<Class> classList) {
        for (Class aClass : classList) {
            // 如果有Component 注解
            if (aClass.isAnnotationPresent(Component.class)) {
                BeanDefinition beanDefinition = new BeanDefinition();
                beanDefinition.setBeanClass(aClass);

                Component component = (Component) aClass.getAnnotation(Component.class);
                String beanName = component.value();
                //是否是单例
                if (aClass.isAnnotationPresent(Scop.class)) {
                    Scop scop = (Scop) aClass.getAnnotation(Scop.class);
                    beanDefinition.setScop(scop.value());
                } else {
                    beanDefinition.setScop("singleton");
                }
                //是否是懒加载
                if (aClass.isAnnotationPresent(Lazy.class)) {
                    Lazy lazy = (Lazy) aClass.getAnnotation(Lazy.class);
                    beanDefinition.setLazy(true);
                } else {
                    beanDefinition.setLazy(false);
                }
                beanDefinitionMap.put(beanName, beanDefinition);

                //添加到 beanPostprocessor
                if (BeanPostProcessor.class.isAssignableFrom(aClass)) {
                    try {
                        BeanPostProcessor beanPostProcessor = (BeanPostProcessor) aClass.getDeclaredConstructor().newInstance();
                        beanPostProcessors.add(beanPostProcessor);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    /**
     * 实例单例bean
     */
    private void instanceSingletonBean() {
        Set<Map.Entry<String, BeanDefinition>> entries = beanDefinitionMap.entrySet();

        for (Map.Entry<String, BeanDefinition> entry : entries) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();

            if (beanDefinition.getScop().equals("singleton")) {

                //如果单例池中不存在 单实例 则创建
                if (!singletonObjects.containsKey(beanName)) {
                    Object object = doCreateBean(beanName, beanDefinition);
                    singletonObjects.put(beanName, object);
                }
            }

        }

    }

    /**
     * @param beanName       创建ena
     * @param beanDefinition
     */
    private Object doCreateBean(String beanName, BeanDefinition beanDefinition) {

        Class beanClass = beanDefinition.getBeanClass();

        try {
            //实例化
            Object object = beanClass.getDeclaredConstructor().newInstance();

            //属性填充
            Field[] declaredFields = beanClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {

                //判断是否有autoWired注解
                if (declaredField.isAnnotationPresent(Autowired.class)) {

                    //用name 来获取名称
                    String name = declaredField.getName();
                    //getBean
                    Object bean = getBean(name);
                    declaredField.setAccessible(true);
                    declaredField.set(object, bean);
                }

            }

            //aware方法
            if (object instanceof BeanNameAware) {
                ((BeanNameAware) object).setBeanName(beanName);
            }

            // 初始化bean之前  beanPostprocessor
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                object = beanPostProcessor.postProcessorBeforeInitization(object, beanName);
            }

            //初始化bena
            if (object instanceof InitializingBean) {
                ((InitializingBean) object).afterPropertiesSet();
            }

            // 初始化bean之后  beanPostprocessor
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                object = beanPostProcessor.postProcessorAfterInitization(object, beanName);
            }

            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    private List<Class> scan(Class clazz) {
        List<Class> calssList = new ArrayList<Class>();
        //扫描 得到class
        ComponentScan componentScan = (ComponentScan) clazz.getAnnotation(ComponentScan.class);
        //获取注解的值
        String path = componentScan.value(); //com.handwritten.demo
        path = path.replace(".", "/");   //com/handwritten/demo

        //获取到类加载器
        ClassLoader classLoader = this.clazz.getClassLoader();
        //加载到url
        URL url = classLoader.getResource(path);
        //获取文件夹
        File direct = new File(url.getFile());
        Set<File> list = new HashSet<File>();
        parseFileList(direct, list);

        for (File file : list) {

            String absolutePath = file.getAbsolutePath();
            String first = path.substring(0, path.indexOf("/"));
            String substring = absolutePath.substring(absolutePath.indexOf(first), absolutePath.lastIndexOf(".class")).replace("\\", ".");
            Class<?> aClass = null;
            try {
                aClass = classLoader.loadClass(substring);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            calssList.add(aClass);
        }
        return calssList;
    }


    public void parseFileList(File file, Set<File> list) {
        if (file.isFile()) {
            list.add(file);
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                parseFileList(f, list);
            }
        } else {
            return;
        }

    }

    public Object getBean(String beanName) {

        //单例bean
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        if (beanDefinition == null) {
            throw new RuntimeException(beanName + "未找到");
        }
        String scop = beanDefinition.getScop();
        if (scop.equals("singleton")) {
            return singletonObjects.get(beanName);
        } else if (scop.equals("prototype")) {
            //原型
            return doCreateBean(beanName, beanDefinition);
        }


        return null;
    }

}
