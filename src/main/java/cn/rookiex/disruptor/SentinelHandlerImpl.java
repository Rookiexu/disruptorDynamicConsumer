package cn.rookiex.disruptor;

import cn.rookiex.disruptor.core.SentinelHandler;
import cn.rookiex.disruptor.core.HandlerEvent;
import cn.rookiex.disruptor.sentinel.SentinelClient;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/11 15:34
 * @Describe :
 * @version:
 */
public class SentinelHandlerImpl extends SentinelHandler {
    public SentinelHandlerImpl(String name, SentinelClient sentinelClient) {
        super(name, sentinelClient);
    }

    @Override
    public void deal(HandlerEvent event) throws Exception {
        int id = event.getId();
        String name = event.getName();
        Thread.sleep(20);
        if (id % 100 == 0)
            System.out.println(("connect ping == " + id + " name == " + name + "  thread ==> " + Thread.currentThread().getName()));
    }
}
