package cn.rookiex.disruptor.core;

import cn.rookiex.disruptor.sentinel.SentinelClient;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/11 15:41
 * @Describe :
 * @version: 1.0
 */
public interface HandlerFactory {
    /**
     * @return 创建handler
     */
    AbstractSentinelHandler createHandler();

    /**
     * @param sentinelClient s
     * 设置
     */
    void setSentinelClient(SentinelClient sentinelClient);
}
