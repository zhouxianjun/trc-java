package com.gary.trc;

import com.gary.trc.annotation.ThriftFilter;
import com.gary.trc.annotation.ThriftReference;
import com.gary.trc.filter.DisabledFilter;
import com.gary.trc.filter.Filter;
import com.gary.trc.invoker.factory.InvokerFactory;
import com.gary.trc.loadbalance.LoadBalance;
import com.gary.trc.loadbalance.RoundRobinLoadBalance;
import com.gary.trc.router.Router;
import com.gary.trc.util.NetworkUtil;
import com.gary.trc.util.Utils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Thrift 客户端初始化工厂
 * @date 17-4-26 下午3:58
 */
@Slf4j
public class ThriftServiceClientFactory extends InstantiationAwareBeanPostProcessorAdapter implements ApplicationContextAware {
    private ThriftServerProvider thriftServerProvider;
    private InvokerFactory invokerFactory;
    @Setter
    private String host;
    @Setter
    private LoadBalance loadBalance = new RoundRobinLoadBalance();
    @Setter
    private ApplicationContext applicationContext;
    private ConcurrentHashMap<String, ReferenceBean> referenceBeans = new ConcurrentHashMap<>();

    public ThriftServiceClientFactory(ThriftServerProvider thriftServerProvider, InvokerFactory invokerFactory) {
        this.thriftServerProvider = thriftServerProvider;
        this.invokerFactory = invokerFactory;
        this.host = NetworkUtil.getLocalHost();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && ! Modifier.isStatic(method.getModifiers())) {
                try {
                    ThriftReference reference = method.getAnnotation(ThriftReference.class);
                    if (reference != null) {
                        Object value = refer(reference, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean);
                        }
                    }
                } catch (Throwable e) {
                    log.error("Failed to init remote service reference at method {} in class {}, cause: {}", new Object[]{name, bean.getClass().getName(), e.getMessage(), e});
                }
            }
        }

        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (! field.isAccessible()) {
                    field.setAccessible(true);
                }
                ThriftReference reference = field.getAnnotation(ThriftReference.class);
                if (reference != null) {
                    Object value = refer(reference, field.getType());
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Throwable e) {
                log.error("Failed to init remote service reference at filed {} in class {}, cause: {}", new Object[]{e.getMessage(), field.getName(), bean.getClass().getName(), e});
            }
        }
        return bean;
    }

    private Object refer(ThriftReference reference, Class<?> referenceClass) throws Exception {
        String key = referenceClass.getName() + ":" + reference.version();
        ReferenceBean referenceBean = referenceBeans.get(key);
        if (referenceBean != null)
            return referenceBean.getObject();
        String serviceName = referenceClass.getEnclosingClass().getSimpleName();
        serviceName = StringUtils.isEmpty(reference.name()) ? serviceName : reference.name();
        referenceBean = new ReferenceBean(referenceClass, invokerFactory, loadBalance, serviceName, reference.version());
        referenceBean.setRouter(new Router(host, Collections.<String>emptyList()));
        referenceBean.addFilter(new DisabledFilter());
        initFilters(referenceBean);
        referenceBean.afterPropertiesSet();
        referenceBeans.putIfAbsent(key, referenceBean);
        referenceBean = referenceBeans.get(key);
        Map<String, Object> address = new HashMap<>();
        address.put("host", host);
        address.put("start", System.currentTimeMillis());
        address.put("methods", Utils.methodJoin(referenceClass));
        address.put("attr", reference.attr());
        address.put("pid", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        String addressStr = Utils.mapToQuery(address, null, null, false, null);
        thriftServerProvider.register(referenceBean, serviceName, reference.version(), addressStr);
        thriftServerProvider.listenerProviders(referenceBean, serviceName, reference.version(), addressStr);
        thriftServerProvider.listenerRouters(referenceBean, serviceName, reference.version(), addressStr);
        thriftServerProvider.listenerConfigurators(referenceBean, serviceName, reference.version(), addressStr);
        return referenceBean.getObject();
    }

    protected void initFilters(ReferenceBean referenceBean) {
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(ThriftFilter.class);
        if (map != null) {
            for (Object object : map.values()) {
                if (object instanceof Filter) {
                    referenceBean.addFilter((Filter) object);
                }
            }
        }
    }
}
