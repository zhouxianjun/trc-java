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
public abstract class AbstractInvoker implements Invoker {
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
        this.host = Utils.getMapString(params, "host", "");
        this.port = Utils.getMapInt(params, "port", 0);
        this.weight = Utils.getMapInt(params, "weight", 100);
        this.startTime = Utils.getMapLong(params, "start", 0);
        this.warmup = Utils.getMapInt(params, "warmup", 0);
        this.attr = Utils.getMapString(params, "attr", "");
    }

    @Override
    public boolean validate() {
        return NetworkUtil.isValidHost(this.host) && NetworkUtil.isInvalidPort(this.port);
    }
}
