package cn.rookiex.disruptor.strategy;

import cn.rookiex.disruptor.core.DynamicDisruptor;
import cn.rookiex.disruptor.sentinel.SentinelEvent;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/10 19:28
 * @Describe :
 * @version: 1.0
 */
public interface RegulateStrategy {
    /**
     * 调节方法
     */
    void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent);

    /**
     * 获得调节的数量
     * */
    int getNeedUpdateCount(SentinelEvent sentinelEvent);
}
