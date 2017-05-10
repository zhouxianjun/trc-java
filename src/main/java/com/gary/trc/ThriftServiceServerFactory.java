package com.gary.trc;

import com.gary.trc.annotation.ThriftService;
import com.gary.trc.util.NetworkUtil;
import com.gary.trc.util.Utils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Thrift 服务初始化工厂
 * @date 17-4-26 下午3:58
 */
@Slf4j
public class ThriftServiceServerFactory implements ApplicationContextAware, InitializingBean, Closeable {
    private ServerThread serverThread;
    @Setter
    private Class<TServer> serverClass;
    @Setter
    private Class<TServerTransport> transportClass;
    @Setter
    private TTransportFactory transportFactory; // 可选
    @Setter
    private TProtocolFactory protocolFactory; // 可选
    @Setter
    private ThriftServerRegister serverRegister;// 可选
    @Setter
    private ExecutorService executorService;// 可选
    @Setter
    private String host;
    @Setter
    private int port = 0; // 可选
    @Setter
    private long maxReadBufferBytes = 1024 * 1024L; // 可选
    @Setter
    private long warmup = 10 * 60 * 10000; // 可选

    private ApplicationContext applicationContext;

    public ThriftServiceServerFactory(Class<TServer> serverClass, Class<TServerTransport> transportClass) {
        this.serverClass = serverClass;
        this.transportClass = transportClass;
        this.host = NetworkUtil.getLocalHost();
        this.port = NetworkUtil.getAvailablePort();
    }

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
    @Override
    public void close() throws IOException {
        if (serverThread != null) serverThread.stopServer();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> services = applicationContext.getBeansWithAnnotation(ThriftService.class);
        if (services != null) {
            TMultiplexedProcessor multiplexedProcessor = new TMultiplexedProcessor();
            for (Map.Entry<String, Object> entry : services.entrySet()) {
                Class serviceClass = AopUtils.isAopProxy(entry.getValue()) ? AopUtils.getTargetClass(entry.getValue()) : entry.getValue().getClass();
                Class[] interfaces = serviceClass.getInterfaces();
                if (interfaces.length == 0)
                    throw new IllegalClassFormatException("service-class should implements Iface");

                Class<?> thriftInterface = getThriftInterfaceClass(interfaces);
                if (thriftInterface == null)
                    throw new IllegalClassFormatException("service-class should implements Iface");
                TProcessor processor = getProcessor(thriftInterface, entry.getValue());
                if (processor == null)
                    throw new IllegalClassFormatException("service-class should implements Iface");

                if (onRegisterProcessor(entry.getValue(), processor)) {
                    ThriftService service = AnnotationUtils.findAnnotation(serviceClass, ThriftService.class);
                    String serviceName = thriftInterface.getEnclosingClass().getSimpleName();
                    serviceName = StringUtils.isEmpty(service.name()) ? serviceName : service.name();
                    multiplexedProcessor.registerProcessor(serviceName, processor);
                    if (serverRegister != null) {
                        Map<String, Object> address = new HashMap<>();
                        address.put("host", host);
                        address.put("port", port);
                        address.put("weight", service.weight());
                        address.put("start", System.currentTimeMillis());
                        address.put("warmup", warmup);
                        address.put("methods", Utils.methodJoin(thriftInterface));
                        address.put("attr", service.attr());
                        address.put("pid", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
                        serverRegister.register(serviceName, service.version(), Utils.mapToQuery(address, null, null, false, null));
                    }
                    log.info("thrift service [{}-{}] register", serviceName, serviceClass);
                }
            }

            serverThread = new ServerThread(multiplexedProcessor);
            start();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 可做扩展
     * @param service
     * @param processor
     * @return
     */
    protected boolean onRegisterProcessor(Object service, TProcessor processor) {return true;}

    /**
     * 获取service 处理类
     * @param thriftInterface 接口
     * @param service
     * @return
     */
    protected TProcessor getProcessor(Class<?> thriftInterface, Object service) {
        String serviceName = thriftInterface.getEnclosingClass().getName();
        String pName = serviceName + "$Processor";
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> pClass = ClassUtils.forName(pName, classLoader);
            if (!TProcessor.class.isAssignableFrom(pClass)) {
                return null;
            }
            Constructor<?> constructor = Utils.getConstructorByParent(pClass, thriftInterface);
            return (TProcessor) constructor.newInstance(service);
        } catch (Exception e) {
            log.warn("service: {} new instance processor error", serviceName, e);
        }
        return null;
    }

    private Class<?> getThriftInterfaceClass(Class[] interfaces) {
        for (Class anInterface : interfaces) {
            if (anInterface.getSimpleName().equals("Iface")) {
                return anInterface;
            }
        }
        return null;
    }

    protected void start() {
        serverThread.start();
    }

    private class ServerThread extends Thread {
        private TServer server;
        ServerThread(TMultiplexedProcessor multiplexedProcessor) throws Exception {
            if (!TServer.class.isAssignableFrom(serverClass))
                throw new IllegalClassFormatException("serverClass should setter for TServer");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            TServerTransport transport = transportClass.getConstructor(int.class).newInstance(port);

            Class<?> argsClass = ClassUtils.forName(serverClass.getName() + ".Args", classLoader);
            TServer.AbstractServerArgs args = (TServer.AbstractServerArgs) Utils.getConstructorByParent(argsClass, TServerTransport.class).newInstance(transport);
            if (executorService != null && argsClass.isAssignableFrom(TThreadedSelectorServer.Args.class)) {
                ((TThreadedSelectorServer.Args)args).executorService(executorService);
                ((TThreadedSelectorServer.Args)args).maxReadBufferBytes = maxReadBufferBytes;
            }
            args.processor(multiplexedProcessor);
            if (transportFactory != null)
                args.transportFactory(transportFactory);
            if (protocolFactory != null)
                args.protocolFactory(protocolFactory);

            Constructor<TServer> serverConstructor = serverClass.getConstructor(args.getClass());
            server = serverConstructor.newInstance(args);
        }

        @Override
        public void run(){
            //启动服务
            server.serve();
        }

        void stopServer(){
            server.stop();
        }
    }
}
