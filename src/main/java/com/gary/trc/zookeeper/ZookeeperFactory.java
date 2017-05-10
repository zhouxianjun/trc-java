package com.gary.trc.zookeeper;

import lombok.Setter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: zk 连接工厂
 * @date 17-4-24 下午3:54
 */
public class ZookeeperFactory implements FactoryBean<CuratorFramework>, Closeable {
    @Setter
    private String hosts;
    // session超时
    @Setter
    private int sessionTimeout = 30000;
    @Setter
    private int connectionTimeout = 30000;

    // 共享一个zk链接
    @Setter
    private boolean singleton = true;

    // 全局path前缀,常用来区分不同的应用
    @Setter
    private String namespace = "thrift";

    private final static String ROOT = "rpc";

    private CuratorFramework zkClient;
    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     * <p>
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        if (zkClient != null) {
            zkClient.close();
        }
    }

    public CuratorFramework getObject() throws Exception {
        if (singleton) {
            if (zkClient == null) {
                zkClient = create();
                zkClient.start();
            }
            return zkClient;
        }
        return create();
    }

    private CuratorFramework create() throws Exception {
        if (StringUtils.isEmpty(namespace)) {
            namespace = ROOT;
        } else {
            namespace = ROOT +"/"+ namespace;
        }
        return create(hosts, sessionTimeout, connectionTimeout, namespace);
    }

    private static CuratorFramework create(String connectString, int sessionTimeout, int connectionTimeout, String namespace) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        return builder.connectString(connectString).sessionTimeoutMs(sessionTimeout).connectionTimeoutMs(connectionTimeout)
                .canBeReadOnly(true).namespace(namespace).retryPolicy(new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
                .defaultData(null).build();
    }

    public Class<?> getObjectType() {
        return CuratorFramework.class;
    }

    public boolean isSingleton() {
        return singleton;
    }
}
