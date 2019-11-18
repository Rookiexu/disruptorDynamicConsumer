package cn.rookiex.disruptor.strategy;

import cn.rookiex.disruptor.DynamicDisruptor;
import cn.rookiex.disruptor.sentinel.SentinelEvent;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/12 13:33
 * @Describe :
 * @version: 1.0
 */
public class IntegralStrategy implements RegulateStrategy {
    @Override
    public void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent) {
        RegulateStrategy.updateThreadCount(dynamicDisruptor, getNeedUpdateCount(sentinelEvent));
    }

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int updateCount = 0;
        int totalDifference = sentinelEvent.getTotalDifference();
        int recentConsumeCount = sentinelEvent.getRecentConsumeCount();
        int runThreadCount = sentinelEvent.getRunThreadCount();
        int totalThreadCount = sentinelEvent.getTotalThreadCount();
        if (totalThreadCount == runThreadCount) {
            if (totalDifference > recentConsumeCount) {
                //保留两位小数
                int needAddThread = (totalDifference * 100 / recentConsumeCount * runThreadCount) / 100 - runThreadCount;
                updateCount += needAddThread;
            } else if (totalDifference > 0) {
                updateCount += 1;
            }
        }
        return updateCount;
    }
}
