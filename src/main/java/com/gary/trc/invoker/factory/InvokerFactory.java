package com.gary.trc.invoker.factory;

import com.gary.trc.invoker.Invoker;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-8 下午4:37
 */
public interface InvokerFactory {
    Invoker newInvoker(String service, String address, Class<?> interfaceClass) throws Exception;
}
