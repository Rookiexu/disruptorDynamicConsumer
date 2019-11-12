package cn.rookiex.disruptor;

import cn.rookiex.disruptor.sentinel.SentinelClient;

import java.util.concurrent.CountDownLatch;

public class DynamicDisruptorTest {

    @org.junit.Test
    public void getDisruptor() {

        int produceSzie = 10;
        CountDownLatch countDownLatch = new CountDownLatch(produceSzie * 2);
        final DynamicDisruptor server = new DynamicDisruptor("server", 16, 16, 512);
        SentinelClient sentinelClient = new SentinelClient(1000,5);
        server.init(1024 * 1024,sentinelClient);
        server.start();

        for (int i = 0; i < produceSzie; i++) {
            startPublishEvent(server, countDownLatch, 1000000, "produce=" + i);
        }

        try {
            for (int i = 0; i < produceSzie; i++) {
                Thread.sleep(5000);
                System.out.println("添加生产者========================>"+i);
                startPublishEvent(server, countDownLatch, 100000, "produce=" + i);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            countDownLatch.await();
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void sleepAndRemoveConsumer(DynamicDisruptor server, int count) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 6; i++) {
            server.decrConsumer();
        }
    }

    private void sleepAndAddConsumer(DynamicDisruptor server, int count) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 6; i++) {
            server.incrConsumer();
        }
    }

    private void startPublishEvent(DynamicDisruptor server, CountDownLatch countDownLatch, int size, String name) {
        new Thread(() -> {
            for (int i = 0; i < size; i++) {
                final int finalI = i;
                server.publishEvent((event, sequence, buffer) -> {
                    event.setName(name);
                    event.setId(finalI);
                });
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            countDownLatch.countDown();
        }).start();
    }

    @org.junit.Test
    public void getExceptionHandler() {
    }

    @org.junit.Test
    public void init() {
    }

    @org.junit.Test
    public void start() {
    }
}