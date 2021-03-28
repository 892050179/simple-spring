package org.example.simplespring.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description: 自动注入注解
 * @author: zhangxiaohu
 * @createDate: 2021/3/26
 * @version: 1.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ZAutowired {
    String value() default "";
}
