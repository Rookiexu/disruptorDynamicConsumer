package cn.rookiex.disruptor.strategy;

import cn.rookiex.disruptor.DynamicDisruptor;
import cn.rookiex.disruptor.sentinel.SentinelEvent;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/10 19:27
 * @Describe :比例调节策略,如果WINDOWS_COUNT时间窗口内不能处理玩所有消息,并且线程全部在运行中,线程比例增加
 * 如果线程运行不满,并且总的任务剩余小于IDLE_THRESHOLD,移除空闲状态线程
 * @version: 1.0
 */
public class ProportionStrategy implements RegulateStrategy {

    private static int WINDOWS_COUNT = 3;
    private static int IDLE_THRESHOLD = 50;

    public static void setWindowsCount(int windowsCount) {
        WINDOWS_COUNT = windowsCount;
    }

    public static void setIdleThreshold(int idleThreshold) {
        IDLE_THRESHOLD = idleThreshold;
    }

    @Override
    public void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent) {
        //检测是否需要调节
        int needUpdateCount = getNeedUpdateCount(sentinelEvent);
        if (needUpdateCount > 0) {
            for (int i = 0; i < needUpdateCount; i++) {
                dynamicDisruptor.incrConsumer();
            }
        } else if (needUpdateCount < 0) {
            for (int i = 0; i < -needUpdateCount; i++) {
                dynamicDisruptor.decrConsumer();
            }
        }
    }

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int recentConsumeCount = sentinelEvent.getRecentConsumeCount();
        int recentProduceCount = sentinelEvent.getRecentProduceCount();
        int totalDifference = sentinelEvent.getTotalDifference();

        //thread info
        int totalThreadCount = sentinelEvent.getTotalThreadCount();
        int runThreadCount = sentinelEvent.getRunThreadCount();

        int updateCount = 0;

        //堆积的判定条件是,在指定窗口数量时间内能不能清理完剩下的消息
        boolean isFull = (totalDifference + WINDOWS_COUNT * recentProduceCount) > WINDOWS_COUNT * recentConsumeCount;

        boolean isThreadRunOut = runThreadCount == totalThreadCount;
        if (isFull && isThreadRunOut) {
            //如果有堆积,根据处理速度添加线程,每需要多一个窗口,就加一个线程
            int needConsumeCount = (totalDifference + WINDOWS_COUNT * recentProduceCount) * 100 / WINDOWS_COUNT;
            int needAddThread = (needConsumeCount / recentConsumeCount * runThreadCount)  / 100 - runThreadCount;
            updateCount += needAddThread;
        } else {
            //如果没有堆积并且没有线程空闲,并且剩余总量不到100
            if (!isThreadRunOut && totalDifference < IDLE_THRESHOLD) {
                updateCount -= totalThreadCount - runThreadCount;
            }
        }
        System.out.println("isFull=="+isFull + " ,runOut=="+isThreadRunOut + ",updateThread == " + updateCount);
        return updateCount;
    }
}
