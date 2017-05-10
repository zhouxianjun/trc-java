package com.gary.trc.loadbalance;

import com.gary.trc.invoker.Invoker;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: 负载均衡方案
 * @date 2016/4/27 9:46
 */
public interface LoadBalance {
    Invoker selector(Collection<Invoker> addressList, Method method);
}
