package com.gary.trc.loadbalance;

import com.gary.trc.bean.AtomicPositiveInteger;
import com.gary.trc.invoker.Invoker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: 轮询调度
 * @date 17-5-9 下午3:57
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    private final ConcurrentMap<String, AtomicPositiveInteger> sequences = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, AtomicPositiveInteger> weightSequences = new ConcurrentHashMap<>();

    @Override
    protected Invoker doSelect(Collection<Invoker> invokers, Method method) {
        String key = invokers.iterator().next().getInterfaceClass().getName() + ":" + method.getName();
        int length = invokers.size(); // 总个数
        int maxWeight = 0; // 最大权重
        int minWeight = Integer.MAX_VALUE; // 最小权重
        List<Invoker> invokerList = new ArrayList<>(invokers);
        for (int i = 0; i < length; i++) {
            int weight = getWeight(invokerList.get(i));
            maxWeight = Math.max(maxWeight, weight); // 累计最大权重
            minWeight = Math.min(minWeight, weight); // 累计最小权重
        }
        if (maxWeight > 0 && minWeight < maxWeight) { // 权重不一样
            AtomicPositiveInteger weightSequence = weightSequences.get(key);
            if (weightSequence == null) {
                weightSequences.putIfAbsent(key, new AtomicPositiveInteger());
                weightSequence = weightSequences.get(key);
            }
            int currentWeight = weightSequence.getAndIncrement() % maxWeight;
            List<Invoker> weightInvokers = new ArrayList<>();
            for (Invoker invoker : invokers) { // 筛选权重大于当前权重基数的Invoker
                if (getWeight(invoker) > currentWeight) {
                    weightInvokers.add(invoker);
                }
            }
            int weightLength = weightInvokers.size();
            if (weightLength == 1) {
                return weightInvokers.get(0);
            } else if (weightLength > 1) {
                invokers = weightInvokers;
                length = invokers.size();
            }
        }
        AtomicPositiveInteger sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new AtomicPositiveInteger());
            sequence = sequences.get(key);
        }
        // 取模轮循
        return invokerList.get(sequence.getAndIncrement() % length);
    }
}
