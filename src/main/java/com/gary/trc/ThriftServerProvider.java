package com.gary.trc;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-4-28 下午4:19
 */
public interface ThriftServerProvider {
    void listenerProviders(ReferenceBean referenceBean, String service, String version, String address) throws Exception;
    void listenerRouters(ReferenceBean referenceBean, String service, String version, String address) throws Exception;
    void listenerConfigurators(ReferenceBean referenceBean, String service, String version, String address) throws Exception;

    void register(ReferenceBean referenceBean, String service, String version, String address) throws Exception;
}
