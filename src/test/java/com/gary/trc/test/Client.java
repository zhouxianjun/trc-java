package com.gary.trc.test;

import com.gary.trc.test.client.Hello;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.LockSupport;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-4-27 下午4:57
 */
public class Client {
    public static void main(String[] args) {
        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring-client.xml");
        context.start();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    context.getBean(Hello.class).say("Alone");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100, 2000);

        LockSupport.park();
    }
}
