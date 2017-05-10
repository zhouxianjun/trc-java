package com.gary.trc.annotation;

import java.lang.annotation.*;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-4-27 上午11:34
 */
@Inherited
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThriftReference {
    String version() default "1.0.0";

    String name() default "";

    String attr() default "";
}