package com.gary.trc.router;

import com.gary.trc.invoker.Invoker;
import com.gary.trc.util.Utils;
import lombok.Getter;
import org.springframework.util.AntPathMatcher;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-8 下午4:50
 */
public class Router {
    private String host;
    @Getter
    private Set<Map<String, Object>> routers;
    private AntPathMatcher antPathMatcher;
    public Router(String host, List<String> routers) {
        this.host = host;
        this.routers = Utils.urlParse(routers);
        this.antPathMatcher = new AntPathMatcher();
    }

    public void update(List<String> routers) {
        this.routers = Utils.urlParse(routers);
    }

    public Map<String, Invoker> match(Method method, Map<String, Invoker> providers) {
        if (this.routers != null && !this.routers.isEmpty()) {
            Map<String, Invoker> result = new HashMap<>();
            String methodName = method.getName();
            for (Map<String, Object> router : this.routers) {
                Collection<String> methods = Utils.getMapCollection(router, "method", Collections.<String>emptyList());
                Collection<String> consumeHosts = Utils.getMapCollection(router, "consumeHost", Collections.<String>emptyList());
                boolean matchMethod = matchRouter(methods, methodName);
                boolean matchConsumeHost = matchRouter(consumeHosts, this.host);
                if (matchMethod && matchConsumeHost) {
                    Collection<String> providerAddress = Utils.getMapCollection(router, "providerAddress", Collections.<String>emptyList());
                    matchProviders(result, providers, providerAddress);
                }
            }
        }
        return providers;
    }

    public boolean matchRouter(Collection<String> router, String str) {
        if (router == null || router.size() <= 0) return true;
        for (String s : router) {
            if (antPathMatcher.match(s, str)) return true;
        }
        return false;
    }

    public void matchProviders(Map<String, Invoker> result, Map<String, Invoker> providers, Collection<String> providerAddress) {
        if (providers == null || providers.isEmpty()) return;
        for (Map.Entry<String, Invoker> entry : providers.entrySet()) {
            Map<String, Object> p = Utils.queryToMap(entry.getKey());
            String host = Utils.getMapString(p, "host", "");
            int port = Utils.getMapInt(p, "port", 0);
            if (matchRouter(providerAddress, String.format("%s:%d", host, port))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
