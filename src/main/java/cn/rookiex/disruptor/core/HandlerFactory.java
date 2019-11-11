package cn.rookiex.disruptor.core;

import cn.rookiex.disruptor.core.SentinelHandler;
import cn.rookiex.disruptor.sentinel.SentinelClient;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/11 15:41
 * @Describe :
 * @version: 1.0
 */
public interface HandlerFactory {
    SentinelHandler createHandler();

    void setSentinelClient(SentinelClient sentinelClient);
}
