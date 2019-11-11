package cn.rookiex.disruptor.sentinel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/8 14:47
 * @Describe : window类是窗口类,内容包含时间窗口内执行的消息和生产的消息,多个window组成检测用的大窗口
 * @version: 1.0
 */
public class Window {

    private long secondTime;

    private AtomicInteger produceCount = new AtomicInteger();

    private AtomicInteger consumeCount = new AtomicInteger();

    public void reSet(long secondTime){
        this.secondTime = secondTime;
        this.produceCount.set(0);
        this.consumeCount.set(0);
    }

    public void addProduceCount(){
        produceCount.incrementAndGet();
    }

    public void addConsumeCount(){
        consumeCount.incrementAndGet();
    }

    public int getProduceCount(){
        return produceCount.get();
    }

    public int getConsumeCount(){
        return consumeCount.get();
    }

    public long getSecondTime() {
        return secondTime;
    }
}
