package com.gary.trc.invoker;

import com.gary.trc.bean.PoolInvokerConfig;
import com.gary.trc.util.ThriftClientPoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-8 下午4:03
 */
@Slf4j
public class PoolInvoker extends AbstractInvoker {
    private GenericObjectPool<TServiceClient> pool;

    public PoolInvoker(String address, Class<?> interfaceClass, String interfaceName, PoolInvokerConfig config) throws Exception {
        super(address, interfaceClass, interfaceName);

        // 加载Iface接口
        final String name = getInterfaceClass().getName();
        // 加载Client.Factory类
        Class<TServiceClientFactory<TServiceClient>> fi = (Class<TServiceClientFactory<TServiceClient>>) ClassUtils.forName(name.replace("$Iface", "") + "$Client$Factory", null);
        TServiceClientFactory<TServiceClient> clientFactory = fi.newInstance();
        ThriftClientPoolFactory poolFactory = new ThriftClientPoolFactory(clientFactory, this, config.getTransportClass(), config.getProtocolClass());
        GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        poolConfig.maxActive = config.getMaxActive();
        poolConfig.minIdle = 0;
        poolConfig.minEvictableIdleTimeMillis = config.getIdleTime();
        poolConfig.timeBetweenEvictionRunsMillis = config.getIdleTime() / 2L;

        pool = new GenericObjectPool<>(poolFactory, poolConfig);
    }

    @Override
    public Object invoker(Method method, Object... args) throws Exception {
        log.debug("{}:{} invoker:{} args: {}", new Object[]{getInterfaceName(), getAddress(), method.getName(), Arrays.toString(args)});
        log.debug("pool.max = {}", pool.getMaxActive());
        log.debug("pool.size = {}", pool.getNumActive());
        log.debug("pool.idle = {}", pool.getNumIdle());
        TServiceClient client = pool.borrowObject();
        try {
            return method.invoke(client, args);
        } finally {
            pool.returnObject(client);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
        try {
            pool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
