package com.gary.trc.test.client;

import com.gary.trc.annotation.ThriftReference;
import com.gary.trc.test.service.Demo;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-5 下午4:56
 */
@Component
public class Hello {
    @ThriftReference(name = "DemoService")
    private Demo.Iface demo;

    public void say(String name) {
        try {
            demo.say(name);
        } catch (TException e) {
            e.printStackTrace();
        }
    }
}
