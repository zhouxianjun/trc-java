package com.gary.trc.bean;

import lombok.Data;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-8 下午4:06
 */
@Data
public class PoolInvokerConfig {
    private long timeOut;
    private int maxActive;
    private long idleTime;
    private Class<TTransport> transportClass;
    private Class<TProtocol> protocolClass;
}
