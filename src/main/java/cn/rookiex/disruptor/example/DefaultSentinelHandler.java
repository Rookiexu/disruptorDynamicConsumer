package cn.rookiex.disruptor.example;

import cn.rookiex.disruptor.core.SentinelHandler;
import cn.rookiex.disruptor.core.HandlerEvent;
import cn.rookiex.disruptor.sentinel.SentinelClient;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/11 15:34
 * @Describe :
 * @version:
 */
public class DefaultSentinelHandler extends SentinelHandler {

    public DefaultSentinelHandler(SentinelClient sentinelClient) {
        super(sentinelClient);
    }

    @Override
    public void deal(HandlerEvent event) throws Exception {
        int id = event.getId();
        String name = event.getName();
        Thread.sleep(20);
        if (id % 1000 == 0)
            System.out.println(("connect ping == " + id + " name == " + name + "  thread ==> " + Thread.currentThread().getName()));
    }
}
