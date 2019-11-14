package cn.rookiex.disruptor;

import cn.rookiex.disruptor.example.ExampleDisruptorServer;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class DynamicDisruptorTest {

    private ExampleDisruptorServer exampleDisruptorServer;

    @Before
    public void initServer() {
        exampleDisruptorServer = new ExampleDisruptorServer();
        exampleDisruptorServer.startServer("server", 1024 * 1024, 16, 512, 1000, 5);
    }

    @Test
    public void getDisruptor() {

        int produceSize = 10;
        CountDownLatch countDownLatch = new CountDownLatch(produceSize * 2);

        for (int i = 0; i < produceSize; i++) {
            startPublishEvent( countDownLatch, 1000000, "produce=" + i);
        }

        try {
            for (int i = 0; i < produceSize; i++) {
                Thread.sleep(5000);
                System.out.println("添加生产者========================>" + i);
                startPublishEvent(countDownLatch, 100000, "produce=" + i);
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

    private void startPublishEvent(CountDownLatch countDownLatch, int size, String name) {
        new Thread(() -> {
            for (int i = 0; i < size; i++) {
                exampleDisruptorServer.sendMsg(i, name);
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