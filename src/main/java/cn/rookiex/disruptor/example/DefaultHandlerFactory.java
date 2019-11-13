package cn.rookiex.disruptor.example;

import cn.rookiex.disruptor.core.HandlerFactory;
import cn.rookiex.disruptor.core.AbstractSentinelHandler;
import cn.rookiex.disruptor.sentinel.SentinelClient;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/11 17:28
 * @Describe :
 * @version:
 */
public class DefaultHandlerFactory implements HandlerFactory {

    private SentinelClient sentinelClient;

    @Override
    public AbstractSentinelHandler createHandler() {
        return new DefaultAbstractSentinelHandler(sentinelClient);
    }

    @Override
    public void setSentinelClient(SentinelClient sentinelClient) {
        this.sentinelClient = sentinelClient;
    }
}
