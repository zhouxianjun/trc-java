package com.gary.trc.zookeeper;

import com.gary.trc.ReferenceBean;
import com.gary.trc.ThriftServerProvider;
import com.gary.trc.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-4-28 下午4:22
 */
@Slf4j
public class ZookeeperThriftServerProvider implements ThriftServerProvider, InitializingBean {
    private CuratorFramework zkClient;
    public ZookeeperThriftServerProvider(CuratorFramework zkClient) {
        this.zkClient = zkClient;
    }

    @Override
    public void listenerProviders(final ReferenceBean referenceBean, String service, String version, String address) throws Exception {
        final String path = String.format("/%s/%s/providers", service, version);
        Utils.createPath(zkClient, path, CreateMode.PERSISTENT);
        PathChildrenCache watcher = new PathChildrenCache(zkClient, path, true);
        watcher.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                try {
                    referenceBean.updateProviders(zkClient.getChildren().forPath(path));
                } catch (Exception e) {
                    log.warn("listener providers error", e);
                }
            }
        });
        watcher.start(PathChildrenCache.StartMode.NORMAL);
    }

    @Override
    public void listenerRouters(final ReferenceBean referenceBean, String service, String version, String address) throws Exception {
        final String path = String.format("/%s/%s/routers", service, version);
        Utils.createPath(zkClient, path, CreateMode.PERSISTENT);
        PathChildrenCache watcher = new PathChildrenCache(zkClient, path, true);
        watcher.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                try {
                    referenceBean.updateRouters(zkClient.getChildren().forPath(path));
                } catch (Exception e) {
                    log.warn("listener routers error", e);
                }
            }
        });
        watcher.start();
    }

    @Override
    public void listenerConfigurators(final ReferenceBean referenceBean, String service, String version, String address) throws Exception {
        final String path = String.format("/%s/%s/configurators", service, version);
        Utils.createPath(zkClient, path, CreateMode.PERSISTENT);
        PathChildrenCache watcher = new PathChildrenCache(zkClient, path, true);
        watcher.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                try {
                    referenceBean.updateConfigurators(zkClient.getChildren().forPath(path));
                } catch (Exception e) {
                    log.warn("listener configurators error", e);
                }
            }
        });
        watcher.start();
    }

    @Override
    public void register(ReferenceBean referenceBean, String service, String version, String address) throws Exception {
        String path = String.format("/%s/%s/consumers/%s", service, version, address);
        Utils.createPath(zkClient, path, CreateMode.EPHEMERAL);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 如果zk尚未启动,则启动
        if (zkClient.getState() == CuratorFrameworkState.LATENT) {
            zkClient.start();
        }
    }
}
