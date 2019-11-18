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

    private int p = 66;
    private int i = 100;
    private int d = 66;

    public void setPID(int p, int i, int d) {
        this.p = checkRange(p);
        this.i = checkRange(i);
        this.d = checkRange(d);
    }

    private int checkRange(int value){
        if (value > 100){
            return 100;
        }else if (value < 0){
            return 0;
        }
        return value;
    }

    @Override
    public void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent) {
        RegulateStrategy.updateThreadCount(dynamicDisruptor, getNeedUpdateCount(sentinelEvent));
    }

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        //调用pid控制器
        int simpleCount = simpleStrategy.getNeedUpdateCount(sentinelEvent);
        int pCount = proportionStrategy.getNeedUpdateCount(sentinelEvent) * p / 100;
        int iCount = integralStrategy.getNeedUpdateCount(sentinelEvent) * i / 100;
        int dCount = derivativeStrategy.getNeedUpdateCount(sentinelEvent) * d / 100;
        System.out.println(" update p == " + pCount + " ,i == " + iCount + " ,d == " + dCount + " simpleCount == " + simpleCount);
        return simpleCount + pCount + iCount + dCount;
    }
}
