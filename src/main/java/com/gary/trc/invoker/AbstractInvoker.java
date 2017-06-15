package com.gary.trc.invoker;

import com.gary.trc.util.NetworkUtil;
import com.gary.trc.util.Utils;
import lombok.Getter;

import java.util.Map;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-8 下午3:12
 */
@Getter
public abstract class AbstractInvoker implements Invoker, Cloneable {
    private String address;
    private String host;
    private int port;
    private int weight;
    private long startTime;
    private int warmup;
    private Class<?> interfaceClass;
    private String interfaceName;
    private String attr;

    public AbstractInvoker(String address, Class<?> interfaceClass, String interfaceName) {
        this.address = address;
        this.interfaceClass = interfaceClass;
        this.interfaceName = interfaceName;
        Map<String, Object> params = Utils.queryToMap(address);
        this.override(params);
        this.startTime = Utils.getMapLong(params, "start", 0);
    }

    @Override
    public void override(Map<String, Object> override) {
        this.host = Utils.getMapString(override, "host", this.host);
        this.port = Utils.getMapInt(override, "port", this.port);
        this.weight = Utils.getMapInt(override, "weight", this.weight);
        this.warmup = Utils.getMapInt(override, "warmup", this.warmup);
        this.attr = Utils.getMapString(override, "attr", this.attr);
    }

    @Override
    public boolean validate() {
        return NetworkUtil.isValidHost(this.host) && NetworkUtil.isInvalidPort(this.port);
    }

    @Override
    public Invoker clone() throws CloneNotSupportedException {
        return (Invoker) super.clone();
    }
}
