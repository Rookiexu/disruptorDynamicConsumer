package cn.rookiex.disruptor.example;

import cn.rookiex.disruptor.DynamicDisruptor;
import cn.rookiex.disruptor.sentinel.SentinelClient;

import java.util.concurrent.CountDownLatch;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/14 17:51
 * @Describe :
 * @version: 1.0
 */
public class ExampleDisruptorServer {

    private DynamicDisruptor server;

    public void startServer(String name, int bufferSize, int consumeSize, int maxConsumeSize, int windowsLength, int windowsSzie) {
        server = new DynamicDisruptor(name, consumeSize, consumeSize, maxConsumeSize);
        SentinelClient sentinelClient = new SentinelClient(windowsLength, windowsSzie);
        server.init(bufferSize, sentinelClient, new ExampleHandlerFactory());
        server.start();
    }

    public void sendMsg(int id, String msg) {
        server.publishEvent((a, b, c) -> {
            a.setId(id);
            a.setName(msg);
        });
    }
}
