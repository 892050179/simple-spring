package org.example.simplespring.framework.servlet;

import org.example.simplespring.framework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @description: 前端控制器
 * @author: zhangxiaohu
 * @createDate: 2021/3/26
 * @version: 1.0
 */
public class ZDispatcherServlet extends HttpServlet {

    private final Properties properties = new Properties();
    /**
     * 用以保存所有扫描包的类名
     */
    private final List<String> classNames = new ArrayList<>();
    /**
     * IoC容器,key名默认为首字母小写
     */
    private final Map<String, Object> ioc = new HashMap<>();

    private final Map<String,Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doPut(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //根据相应的URL找到一个相应的方法，并通过resp返回
        try {
            doDisptach(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception.Detail : " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDisptach(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        //若web.xml中servlet-mapping为/*使用
        //若web.xml中servlet-mapping为/使用req.getServletPath()
        String url = req.getPathInfo();

        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //用于存放实参
        Object[] paramValues = new Object[method.getParameterCount()];
        //形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if(parameterType == HttpServletRequest.class){
                paramValues[i] = req;
            } else if(parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
            } else if (parameterType == String.class) {
                //获取参数前的注解
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                //注入参数按照@ZRequestParam的value值赋值
                for (int j = 0; j < parameterAnnotations[i].length; j++) {
                    Annotation paramAnnotation = parameterAnnotations[i][j];
                    if(paramAnnotation instanceof ZRequestParam){
                        String value = ((ZRequestParam) paramAnnotation).value().trim().replaceAll("/+","");
                        paramValues[i] = req.getParameter(value);
                    }
                }
            }
        }

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(this.ioc.get(beanName),paramValues);
    }

    @Override
    public void init(ServletConfig config) {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类
        doScanner(properties.getProperty("scanPackage"));

        //====================IoC====================
        //3.初始化IoC容器，实例化相关的类并保存到IoC容器中
        doInstance();

        //====================AOP====================
        //TODO

        //====================DI====================
        //4.依赖注入
        doAutowired();

        //====================MVC====================
        //5.初始化HandlerMapping
        doInitHandlerMapping();
    }

    private void doInitHandlerMapping() {
        if(ioc.isEmpty()) {return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(ZController.class)) {continue;}

            String baseUrl = "";
            if(clazz.isAnnotationPresent(ZRequestMapping.class)){
                baseUrl = clazz.getAnnotation(ZRequestMapping.class).value();
            }

            for (Method method : clazz.getMethods()){
                if(!method.isAnnotationPresent(ZRequestMapping.class)) {continue; }
                ZRequestMapping requestMapping = method.getAnnotation(ZRequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
            }
        }

    }

    private void doAutowired() {
        if (ioc.isEmpty()) {return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(ZAutowired.class)) {continue;}
                String beanName = toLowerFirstCase(field.getType().getSimpleName());
                String autowiredValue = field.getAnnotation(ZAutowired.class).value().trim();
                if(!"".equals(autowiredValue)) {
                    beanName = autowiredValue;
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                //判断class需不需要加入IoC容器
                if (clazz.isAnnotationPresent(ZController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(ZService.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //1.多个包下有相同类名，需在@ZService注解添加value
                    if (!"".equals(clazz.getAnnotation(ZService.class).value().trim())) {
                        beanName = clazz.getAnnotation(ZService.class).value();
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    //2.针对接口,若有一个实现类注入唯一实现类，若有多个实现类抛异常
                    for (Class<?> i : clazz.getInterfaces()) {
                        beanName = toLowerFirstCase(i.getSimpleName());
                        if (ioc.containsKey(beanName)) {
                            throw new Exception("This " + i.getName() + "already exists");
                        }
                        ioc.put(beanName, instance);
                    }

                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        //默认为首字母为大写，若首字母为小写有错误
        chars[0] += 32;
        return new String(chars);
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = file.getName().replaceAll(".class", "");
                classNames.add(scanPackage + "." + className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
