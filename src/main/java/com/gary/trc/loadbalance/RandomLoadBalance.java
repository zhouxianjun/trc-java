package com.gary.trc.loadbalance;

import com.gary.trc.invoker.Invoker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-5-9 下午3:50
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    private final Random random = new Random();

    @Override
    protected Invoker doSelect(Collection<Invoker> invokers, Method method) {
        int length = invokers.size(); // 总个数
        int totalWeight = 0; // 总权重
        boolean sameWeight = true; // 权重是否都一样
        List<Invoker> invokerList = new ArrayList<>(invokers);
        for (int i = 0; i < length; i++) {
            int weight = getWeight(invokerList.get(i));
            totalWeight += weight; // 累计总权重
            if (sameWeight && i > 0 && weight != getWeight(invokerList.get(i - 1))) {
                sameWeight = false; // 计算所有权重是否一样
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            // 如果权重不相同且权重大于0则按总权重数随机
            int offset = random.nextInt(totalWeight);
            // 并确定随机值落在哪个片断上
            for (int i = 0; i < length; i++) {
                offset -= getWeight(invokerList.get(i));
                if (offset < 0) {
                    return invokerList.get(i);
                }
            }
        }
        // 如果权重相同或权重为0则均等随机
        return invokerList.get(random.nextInt(length));
    }
}
