package cn.rookiex.disruptor.strategy;

import cn.rookiex.disruptor.DynamicDisruptor;
import cn.rookiex.disruptor.sentinel.SentinelEvent;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/12 13:39
 * @Describe :
 * @version: 1.0
 */
public class PIDStrategy implements RegulateStrategy {
    private ProportionStrategy proportionStrategy = new ProportionStrategy();
    private IntegralStrategy integralStrategy = new IntegralStrategy();
    private DerivativeStrategy derivativeStrategy = new DerivativeStrategy();
    private SimpleStrategy simpleStrategy = new SimpleStrategy();

    @Override
    public void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent) {
        RegulateStrategy.updateThreadCount(dynamicDisruptor, getNeedUpdateCount(sentinelEvent));
    }

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        //调用pid控制器
        int simpleCount = simpleStrategy.getNeedUpdateCount(sentinelEvent);
        int pCount = proportionStrategy.getNeedUpdateCount(sentinelEvent);
        int iCount = integralStrategy.getNeedUpdateCount(sentinelEvent);
        int dCount = derivativeStrategy.getNeedUpdateCount(sentinelEvent);
        System.out.println(" update p == " + pCount + " ,i == " + iCount + " ,d == " + dCount + " reduce == " + simpleCount);
        return simpleCount + pCount + iCount + dCount;
    }
}
