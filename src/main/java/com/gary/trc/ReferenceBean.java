package com.gary.trc;

import com.gary.trc.bean.Context;
import com.gary.trc.filter.Filter;
import com.gary.trc.invoker.Invoker;
import com.gary.trc.invoker.factory.InvokerFactory;
import com.gary.trc.loadbalance.LoadBalance;
import com.gary.trc.router.Router;
import com.gary.trc.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: 客户端代理
 * @date 17-4-28 下午4:03
 */
@Slf4j
public class ReferenceBean implements FactoryBean, InitializingBean {
    @Getter
    private Object object;
    @Getter
    private Class<?> objectType;
    @Setter
    private Router router;
    private final List<Filter> filters = new ArrayList<>();
    private LoadBalance loadBalance;
    private Class<?> referenceClass;
    private InvokerFactory invokerFactory;
    private String service;
    private String version;
    private String host;

    private Map<String, Invoker> providerCache = new HashMap<>();

    private Set<String> configuratorCache = new HashSet<>();

    public ReferenceBean(Class<?> referenceClass, InvokerFactory invokerFactory, LoadBalance loadBalance, String service, String version, String host) {
        this.referenceClass = referenceClass;
        this.invokerFactory = invokerFactory;
        this.loadBalance = loadBalance;
        this.service = service;
        this.version = version;
        this.host = host;
    }

    public void updateProviders(List<String> providers) throws Exception {
        log.debug("{}-{} providers changed: \n{}", new Object[]{this.service, this.version, StringUtils.join(providers, "\n")});
        Set<String> urls = new HashSet<>(providers);

        // 删除不存在的服务地址
        Iterator<Map.Entry<String, Invoker>> iterator = providerCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Invoker> entry = iterator.next();
            String address = entry.getKey();
            if (!urls.contains(address)) {
                entry.getValue().destroy();
                iterator.remove();
                log.info("zookeeper unsubscribe {}-{} for {}", new Object[]{this.service, this.version, address});
            }
        }

        // 缓存新的服务地址
        for (String url : urls) {
            if (!providerCache.containsKey(url)) {
                try {
                    Invoker value = invokerFactory.newInvoker(this.service, url, this.referenceClass);
                    if (value != null && value.validate()) {
                        providerCache.put(url, value);
                        log.info("zookeeper subscribe {}-{} for {}", new Object[]{this.service, this.version, url});
                    }
                } catch (Exception e) {
                    log.warn("subscribe {}-{} for {} error", new Object[]{this.service, this.version, url, e});
                }
            }
        }
    }
    public void updateRouters(List<String> routers) {
        router.update(routers);
    }
    public void updateConfigurators(List<String> configurators) {
        configuratorCache.clear();
        configuratorCache = new HashSet<>(configurators);
    }

    public ReferenceBean addFilter(Filter filter) {
        synchronized (this.filters) {
            this.filters.add(filter);
        }
        return this;
    }

    public ReferenceBean removeFilter(Filter filter) {
        synchronized (this.filters) {
            this.filters.remove(filter);
        }
        return this;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 加载Iface接口
        final String name = referenceClass.getName();
        objectType = ClassUtils.forName(name, classLoader);
        object = Proxy.newProxyInstance(classLoader, new Class[] { objectType }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 检查生产者是否含有当前函数
                Map<String, Invoker> checkMethodResult = checkMethod(method);
                // 路由匹配
                Map<String, Invoker> routerResult = router.match(method, checkMethodResult);
                Context ctx = new Context();
                ctx.setMethod(method);
                ctx.setProxy(proxy);
                ctx.setService(service);
                ctx.setVersion(version);
                ctx.setHost(host);
                // 执行过滤器选择生产者
                if (!filterSelector(ctx, routerResult, args)) {
                    return null;
                }
                // 负载均衡选举
                Invoker invoker = loadBalance.selector(routerResult.values(), method);
                Assert.notNull(invoker, String.format("service:%s version:%s is not server online", service, version));
                // 调用过滤器before 返回 不等于 true则认为不往下执行 直接返回
                Object before = filterBefore(ctx, invoker, args);
                if (!(before instanceof Boolean) || !(boolean) before) {
                    filterAfter(ctx, before, args);
                    return before;
                }
                // 远程调用
                Object result = invoker.invoker(method, args);
                // 执行过滤器 after
                filterAfter(ctx, result, args);
                return result;
            }

            private boolean filterSelector(Context ctx, Map<String, Invoker> providerMap, Object[] args) {
                if (!filters.isEmpty()) {
                    for (Filter filter : filters) {
                        if (!filter.selector(ctx, providerMap, router.getRouters(), configuratorCache, args)) {
                            return false;
                        }
                    }
                }
                return true;
            }

            private Object filterBefore(Context ctx, Invoker invoker, Object[] args) {
                if (!filters.isEmpty()) {
                    for (Filter filter : filters) {
                        Object result = filter.before(ctx, invoker, args);
                        if (!(result instanceof Boolean) || !(boolean) result) {
                            return result;
                        }
                    }
                }
                return true;
            }

            private void filterAfter(Context ctx, Object result, Object[] args) {
                if (!filters.isEmpty()) {
                    for (Filter filter : filters) {
                        filter.after(ctx, result, args);
                    }
                }
            }

            private Map<String, Invoker> checkMethod(Method method) throws Exception {
                Map<String, Invoker> result = new HashMap<>();
                Iterator<Map.Entry<String, Invoker>> iterator = providerCache.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Invoker> entry = iterator.next();
                    String address = entry.getKey();
                    Map<String, Object> map = Utils.queryToMap(address);
                    String methods = Utils.getMapString(map, "methods", "");
                    if (Utils.findForArray(methods.split(","), method.getName()) != null) {
                        result.put(address, entry.getValue().clone());
                    }
                }

                if (providerCache.size() > 0 && result.size() <= 0) {
                    throw new NoSuchMethodException(String.format("service:%s version:%s method: %s not found", service, version, method.getName()));
                }
                return result;
            }
        });
    }

}
