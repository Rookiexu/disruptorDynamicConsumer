package cn.rookiex.disruptor.strategy;

import cn.rookiex.disruptor.DynamicDisruptor;
import cn.rookiex.disruptor.sentinel.SentinelEvent;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/12 14:23
 * @Describe :
 * @version: 1.0
 */
public class SimpleStrategy implements RegulateStrategy {
    @Override
    public void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent) {
        RegulateStrategy.updateThreadCount(dynamicDisruptor, getNeedUpdateCount(sentinelEvent));
    }

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int totalDifference = sentinelEvent.getTotalDifference();
        int runThreadCount = sentinelEvent.getRunThreadCount();
        int totalThreadCount = sentinelEvent.getTotalThreadCount();

        System.out.println("t diff == " + totalDifference
                + "  r diff == " + sentinelEvent.getRecentDifference()
                + "  r produce == " + sentinelEvent.getRecentProduceCount()
                + "  r consume == " + sentinelEvent.getRecentConsumeCount()
                + "  r thread == " + sentinelEvent.getRunThreadCount()
                + "  t thread == " + sentinelEvent.getTotalThreadCount()
        );
        int updateCount = 0;
        if (totalDifference < sentinelEvent.getRecentConsumeCount() && runThreadCount < totalThreadCount) {
            updateCount -= (totalThreadCount - runThreadCount) / 2 + (totalThreadCount - runThreadCount) % 2;
        }

        if (runThreadCount == totalThreadCount) {
            if (sentinelEvent.getRecentProduceCount() > sentinelEvent.getRecentConsumeCount()) {
                updateCount += 1;
            }
        }


        return updateCount;
    }
}
