package com.gary.trc.filter;

import com.gary.trc.bean.Context;
import com.gary.trc.invoker.Invoker;

import java.util.Map;
import java.util.Set;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-9 上午11:08
 */
public interface Filter {
    /**
     * 过滤器生产者
     * @param ctx 上下文
     * @param providerMap 生产者
     * @param routers 路由
     * @param configurators 配置
     * @param args 参数
     * @return
     */
    boolean selector(Context ctx, Map<String, Invoker> providerMap, Set<Map<String, Object>> routers, Set<String> configurators, Object[] args);

    /**
     * 执行前
     * @param ctx 上下文
     * @param invoker 远程调用
     * @param args 参数
     * @return 返回非boolean类型并且不为true 则直接返回该返回值
     */
    Object before(Context ctx, Invoker invoker, Object[] args);

    /**
     * 执行后
     * @param ctx 上下文
     * @param result 返回值
     * @param args 参数
     */
    void after(Context ctx, Object result, Object[] args);
}
