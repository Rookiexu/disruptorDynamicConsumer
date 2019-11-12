package cn.rookiex.disruptor.strategy;

import cn.rookiex.disruptor.DynamicDisruptor;
import cn.rookiex.disruptor.sentinel.SentinelEvent;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/12 13:34
 * @Describe :
 * @version:
 */
public class DerivativeStrategy implements RegulateStrategy {

    private int lastDifference = 0;

    @Override
    public void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent) {
        RegulateStrategy.updateThreadCount(dynamicDisruptor, getNeedUpdateCount(sentinelEvent));
    }

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int updateCount = 0;
        if (lastDifference != 0) {
            int runThreadCount = sentinelEvent.getRunThreadCount();
            int totalThreadCount = sentinelEvent.getTotalThreadCount();
            if (totalThreadCount == runThreadCount){
                int err = sentinelEvent.getTotalDifference() - lastDifference;
                int needUpdate = err * 100 / sentinelEvent.getRecentConsumeCount()  * runThreadCount / 100;
                updateCount += needUpdate;
            }
        }
        lastDifference = sentinelEvent.getTotalDifference();
        return updateCount;
    }
}
