package com.gary.trc.invoker;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-4-28 下午4:16
 */
public interface Invoker extends Cloneable {
    String getAddress();

    String getHost();

    int getPort();

    int getWeight();

    long getStartTime();

    int getWarmup();

    Class<?> getInterfaceClass();

    String getInterfaceName();

    String getAttr();

    Object invoker(Method method, Object...args) throws Exception;

    boolean isAvailable();

    void destroy();

    void override(Map<String, Object> override);

    boolean validate();

    Invoker clone() throws CloneNotSupportedException;
}
