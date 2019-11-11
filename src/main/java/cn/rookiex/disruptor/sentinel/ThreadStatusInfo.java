package cn.rookiex.disruptor.sentinel;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/11 15:17
 * @Describe :
 * @version: 1.0
 */
public interface ThreadStatusInfo {
    void threadRun();

    void threadWait();

    void threadReady();

    void threadShutDown();
}
