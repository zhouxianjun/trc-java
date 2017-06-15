package com.gary.trc.bean;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-9 上午11:12
 */
@Data
public class Context {
    private Object proxy;
    private Method method;
    private String service;
    private String version;
    private String host;
}
