package com.gary.trc.filter;

import com.gary.trc.bean.Context;
import com.gary.trc.invoker.Invoker;
import com.gary.trc.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-9 下午4:08
 */
public class DisabledFilter implements Filter {
    /**
     * 过滤器生产者
     *
     * @param ctx           上下文
     * @param providerMap   生产者
     * @param routers       路由
     * @param configurators 配置
     * @param args          参数
     * @return
     */
    @Override
    public boolean selector(Context ctx, Map<String, Invoker> providerMap, Set<Map<String, Object>> routers, Set<String> configurators, Object[] args) {
        if (configurators == null || configurators.isEmpty()) return true;
        for (String configurator : configurators) {
            Map<String, Object> config = Utils.queryToMap(configurator);
            this.disableConsumer(ctx, config);
            if (!this.shieldedConsumer(ctx, config)) return false;
            this.configProvider(providerMap, config);
            this.selectorProvider(providerMap, config);
        }
        return true;
    }

    /**
     * 执行前
     *
     * @param ctx     上下文
     * @param invoker 远程调用
     * @param args    参数
     * @return 返回非boolean类型并且不为true 则直接返回该返回值
     */
    @Override
    public Object before(Context ctx, Invoker invoker, Object[] args) {
        return true;
    }

    /**
     * 执行后
     *
     * @param ctx    上下文
     * @param result 返回值
     * @param args   参数
     */
    @Override
    public void after(Context ctx, Object result, Object[] args) {

    }

    private void selectorProvider(Map<String, Invoker> providerMap, Map<String, Object> config) {
        String disable = Utils.getMapString(config, "disabled", "");
        if (StringUtils.isNotEmpty(disable)) {
            String[] disabled = disable.split(",");
            Iterator<Map.Entry<String, Invoker>> iterator = providerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Invoker> entry = iterator.next();
                Map<String, Object> p = Utils.queryToMap(entry.getKey());
                String value = String.format("%s:%d", Utils.getMapString(p, "host", ""), Utils.getMapInt(p, "port", 0));
                if (Utils.findForArray(disabled, value) != null) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void configProvider(Map<String, Invoker> providerMap, Map<String, Object> config) {
        String override = Utils.getMapString(config, "override", "");
        if (StringUtils.isNotEmpty(override)) {
            for (Map.Entry<String, Invoker> entry : providerMap.entrySet()) {
                Map<String, Object> p = Utils.queryToMap(entry.getKey());
                String value = String.format("%s:%d", Utils.getMapString(p, "host", ""), Utils.getMapInt(p, "port", 0));
                if (value.equals(override)) {
                    entry.getValue().override(config);
                }
            }
        }
    }

    private void disableConsumer(Context ctx, Map<String, Object> config) {
        String disable = Utils.getMapString(config, "consumer_disabled", "");
        if (StringUtils.isNotEmpty(disable)) {
            String[] disabled = disable.split(",");
            if (Utils.findForArray(disabled, ctx.getHost()) != null) {
                throw new RuntimeException("consumer " + ctx.getHost() + " is disabled");
            }
        }
    }

    private boolean shieldedConsumer(Context ctx, Map<String, Object> config) {
        String shield = Utils.getMapString(config, "shielded", "");
        if (StringUtils.isNotEmpty(shield)) {
            String[] shielded = shield.split(",");
            if (Utils.findForArray(shielded, ctx.getHost()) != null) {
                return false;
            }
        }
        return true;
    }
}
