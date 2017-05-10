package com.gary.trc.invoker.factory;

import com.gary.trc.invoker.Invoker;
import com.gary.trc.invoker.PoolInvoker;
import com.gary.trc.bean.PoolInvokerConfig;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-8 下午4:39
 */
public class PoolInvokerFactory implements InvokerFactory {
    private PoolInvokerConfig config;
    public PoolInvokerFactory(PoolInvokerConfig config) {
        this.config = config;
        if (config.getMaxActive() == 0) config.setMaxActive(100);
        if (config.getIdleTime() == 0) config.setIdleTime(180000);
    }

    @Override
    public Invoker newInvoker(String service, String address, Class<?> interfaceClass) throws Exception {
        return new PoolInvoker(address, interfaceClass, service, config);
    }
}
