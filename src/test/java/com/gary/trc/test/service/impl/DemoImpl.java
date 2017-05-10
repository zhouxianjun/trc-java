package com.gary.trc.test.service.impl;

import com.gary.trc.annotation.ThriftService;
import com.gary.trc.test.service.Demo;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-4-27 下午4:34
 */
@ThriftService(name = "DemoService")
@Service
public class DemoImpl implements Demo.Iface {
    @Override
    public String say(String name) throws TException {
        System.out.println(name);
        return "hello " + name;
    }
}
