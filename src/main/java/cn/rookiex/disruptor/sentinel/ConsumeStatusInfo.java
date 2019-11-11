package cn.rookiex.disruptor.sentinel;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/11 15:32
 * @Describe :
 * @version: 1.0
 */
public interface ConsumeStatusInfo {
    void addConsumeCount();

    void addProduceCount();
}
