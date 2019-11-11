package cn.rookiex.disruptor.core;

import cn.rookiex.disruptor.sentinel.SentinelClient;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.WorkHandler;

import java.util.concurrent.CountDownLatch;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/8 11:20
 * @Describe :
 * @version:
 */
public class DynamicHandler implements WorkHandler<HandlerEvent>, LifecycleAware {
    private SentinelClient sentinelClient;

    private String name;

    public DynamicHandler(String name, SentinelClient sentinelClient) {
        this.name = name;
        this.sentinelClient = sentinelClient;
    }

    /**
     * Callback to indicate a unit of work needs to be processed.
     *
     * @param event published to the {@link RingBuffer}
     * @throws Exception if the {@link WorkHandler} would like the exception handled further up the chain.
     */
    @Override
    public void onEvent(HandlerEvent event) throws Exception {
        try {
            sentinelClient.threadRun();
            int id = event.getId();
            String name = event.getName();
            Thread.sleep(20);
            if (id % 100 == 0)
                System.out.println(("connect ping == " + id + " name == " + name + "  thread ==> " + Thread.currentThread().getName()));
        } catch (Exception e) {
            System.out.println("deal transmit err ");
        } finally {
            sentinelClient.addConsumeCount();
            sentinelClient.threadWait();
        }
    }

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @Override
    public void onStart() {
        sentinelClient.threadReady();
    }

    @Override
    public void onShutdown() {
        shutdownLatch.countDown();
        sentinelClient.threadShutDown();
    }

    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    public void setName(String name) {
        this.name = name;
    }
}
