package cn.rookiex.disruptor.core;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/8 10:14
 * @Describe :
 * @version: 1.0
 */
public interface DynamicConsumer {
    /**
     * 添加消费者
     */
    void incrConsumer();

    /**
     * 减少消费者
     */
    void decrConsumer();
}
