package com.gary.trc;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: 注册中心 接口
 * @date 17-4-24 下午3:39
 */
public interface ThriftServerRegister {
    /**
     * 发布服务接口
     * @param service 服务接口名称，一个产品中不能重复
     * @param version 服务接口的版本号，默认1.0.0
     * @param address 服务发布的地址和端口
     */
    void register(String service, String version, String address) throws Exception;
}
