package com.gary.trc.zookeeper;

import com.gary.trc.ThriftServerRegister;
import com.gary.trc.util.Utils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.InitializingBean;

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
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(zkClient.getState() == CuratorFrameworkState.LATENT){
            zkClient.start();
        }
    }
}
