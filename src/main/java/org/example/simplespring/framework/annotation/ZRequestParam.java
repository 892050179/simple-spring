package org.example.simplespring.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description: 接收请求参数，暂时只支持String类型
 * @author: zhangxiaohu
 * @createDate: 2021/3/26
 * @version: 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZRequestParam {
    String value() default "";
}
