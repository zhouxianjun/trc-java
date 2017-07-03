package com.gary.trc.zookeeper;

import com.gary.trc.ThriftServerRegister;
import com.gary.trc.bean.ServerRegCache;
import com.gary.trc.util.Utils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Zookeeper 注册中心
 * @date 17-4-24 下午4:48
 */
@Slf4j
public class ZookeeperThriftServerRegister implements ThriftServerRegister, InitializingBean {
    @Setter
    private CuratorFramework zkClient;
    private Map<String, ServerRegCache> caches;
    private AtomicBoolean run = new AtomicBoolean(false);

    /**
     * 发布服务接口
     *
     * @param service 服务接口名称，一个产品中不能重复
     * @param version 服务接口的版本号，默认1.0.0
     * @param address 服务发布的地址和端口
     */
    @Override
    public void register(String service, String version, String address) throws Exception {
        String path = String.format("/%s/%s/providers/%s", service, version, address);
        Utils.createPath(zkClient, path, CreateMode.EPHEMERAL);
        if (!caches.containsKey(path)) caches.put(path, new ServerRegCache(service, version, address));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(zkClient.getState() == CuratorFrameworkState.LATENT){
            zkClient.start();
        }
        caches = new HashMap<>();

        zkClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.LOST && !caches.isEmpty() && run.compareAndSet(false, true)) {
                    while (true) {
                        try {
                            if (curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
                                retry();
                                run.set(false);
                                break;
                            }
                        } catch (Exception e) {
                            log.warn("zookeeper check connect error", e);
                        }
                    }
                }
            }
        });
    }

    private void retry() {
        for (Map.Entry<String, ServerRegCache> entry : caches.entrySet()) {
            try {
                ServerRegCache cache = entry.getValue();
                this.register(cache.getService(), cache.getVersion(), cache.getAddress());
            } catch (Throwable e) {
                log.warn("register for {} error", entry.getKey(), e);
            }
        }
    }
}
