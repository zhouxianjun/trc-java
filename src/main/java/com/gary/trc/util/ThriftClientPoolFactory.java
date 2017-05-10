package com.gary.trc.util;

import com.gary.trc.invoker.Invoker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.Constructor;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-8 下午4:22
 */
@Slf4j
public class ThriftClientPoolFactory extends BasePoolableObjectFactory<TServiceClient> {
    private final TServiceClientFactory<TServiceClient> clientFactory;
    private Class<TTransport> transportClass;
    private Class<TProtocol> protocolClass;
    private Invoker invoker;

    private String serviceName;

    public ThriftClientPoolFactory(TServiceClientFactory<TServiceClient> clientFactory,
                                   Invoker invoker,
                                   Class<TTransport> transportClass,
                                   Class<TProtocol> protocolClass) {
        this.clientFactory = clientFactory;
        this.transportClass = transportClass;
        this.protocolClass = protocolClass;
        this.serviceName = invoker.getInterfaceName();
        this.invoker = invoker;
    }

    @Override
    public TServiceClient makeObject() throws Exception {
        TSocket tsocket = new TSocket(invoker.getHost(), invoker.getPort());
        Constructor<TTransport> transportConstructor = Utils.getConstructorByParent(transportClass, TSocket.class);
        TTransport transport = transportConstructor.newInstance(tsocket);
        Constructor<TProtocol> protocolConstructor = Utils.getConstructorByParent(protocolClass, TTransport.class);
        TProtocol protocol = protocolConstructor.newInstance(transport);
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, serviceName);
        TServiceClient client = this.clientFactory.getClient(mp);
        transport.open();
        return client;
    }

    public void destroyObject(TServiceClient client) throws Exception {
        TTransport pin = client.getInputProtocol().getTransport();
        pin.close();
    }

    public boolean validateObject(TServiceClient client) {
        TTransport pin = client.getInputProtocol().getTransport();
        return pin.isOpen();
    }
}
