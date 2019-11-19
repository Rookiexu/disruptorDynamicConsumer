package cn.rookiex.disruptor.sentinel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/8 14:46
 * @Describe :
 * @version: 1.0
 */
public class SentinelClient implements ThreadStatusInfo, ConsumeStatusInfo {

    /**
     * 默认一个小窗口大小5秒
     */
    public static final int WINDOWS_LENGTH = 3 * 1000;

    /**
     * 默认窗口数量 10个  3*10 == 30秒 //默认的检测窗口也是12个开始检测
     */
    public static final int WINDOWS_SIZE = 10;

    private static final int MILLION = 1000000;

    private AtomicInteger totalProduceCount = new AtomicInteger();

    private AtomicInteger totalConsumeCount = new AtomicInteger();

    private AtomicInteger millionCount = new AtomicInteger();

    private int windowsLength;
    private int windowsSize;
    private int checkInterval;

    private Window[] windows;

    private List<SentinelListener> listenerList = new ArrayList<>();

    /**
     * 运行中的线程
     */
    private AtomicInteger runThreadCount = new AtomicInteger();

    /**
     * 已经在运行的线程数量
     */
    private AtomicInteger totalThreadCount = new AtomicInteger();

    /**
     * 延时线程
     * */
    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2, (r) -> {
        Thread thread = new Thread(r);
        thread.setName("SentinelThread");
        return thread;
    });

    //暂时不用,因为滑动窗口算法,在策略算法的反馈上会有延迟误差,导致频繁增减线程
    @Deprecated
    public SentinelClient(int windowsLength, int windowsSize, int checkInterval) {
        this.windowsLength = windowsLength;
        this.windowsSize = windowsSize;
        this.checkInterval = checkInterval;
        init();
    }

    //windowsSize == checkInterval 其实就是固定窗口算法,固定窗口是滑动窗口的一个特例
    public SentinelClient(int windowsLength, int windowsSize) {
        this.windowsLength = windowsLength;
        this.windowsSize = windowsSize;
        this.checkInterval = windowsSize;
        init();
    }

    public SentinelClient() {
        this.windowsLength = WINDOWS_LENGTH;
        this.windowsSize = WINDOWS_SIZE;
        this.checkInterval = WINDOWS_SIZE;
        init();
    }

    private void init() {
        windows = new Window[windowsSize];
        for (int i = 0; i < windowsSize; i++) {
            windows[i] = new Window();
        }
    }

    public void addListener(SentinelListener sentinelListener) {
        this.listenerList.add(sentinelListener);
    }

    @Override
    public void addConsumeCount() {
        totalConsumeCount.incrementAndGet();
        checkTotalCount();
        long millis = System.currentTimeMillis();
        Window currentWindow = getCurrentWindow(millis);
        currentWindow.addConsumeCount();
    }

    private Lock updateLock = new ReentrantLock();

    public Window getCurrentWindow(long time) {
        long timeId = time / windowsLength;
        int idx = (int) (timeId % windowsSize);
        time = time - time % windowsLength;
        Window old = windows[idx];
        while (true) {
            if (time == old.getSecondTime()) {
                break;
            } else if (time > old.getSecondTime()) {
                if (updateLock.tryLock()) {
                    try {
                        if (time > old.getSecondTime()) {
                            old.reSet(time);
                            break;
                        }
                    } finally {
                        updateLock.unlock();
                    }
                } else {
                    Thread.yield();
                }
            }
        }

//        if (idx % (checkInterval - 1) == 0 && idx != 0) {
//            check();
//        }
        return old;
    }

    private SentinelEvent getNoticeEvent(long time) {
        int a = 0;
        int b = 0;
        for (Window w : windows) {
            a += w.getProduceCount();
            b += w.getConsumeCount();
        }
        SentinelEvent sentinelEvent = new SentinelEvent();
        sentinelEvent.setRecentProduceCount(a);
        sentinelEvent.setRecentConsumeCount(b);
        sentinelEvent.setTotalProduceCount(totalProduceCount.get());
        sentinelEvent.setTotalConsumeCount(totalConsumeCount.get());
        sentinelEvent.setMillionCount(millionCount.get());
        sentinelEvent.setTime(time);
        sentinelEvent.setRunThreadCount(runThreadCount.get());
        sentinelEvent.setTotalThreadCount(totalThreadCount.get());
        return sentinelEvent;
    }

    @Override
    public void addProduceCount() {
        totalProduceCount.incrementAndGet();
        checkTotalCount();
        long millis = System.currentTimeMillis();
        Window currentWindow = getCurrentWindow(millis);
        currentWindow.addProduceCount();
    }

    private Lock lock = new ReentrantLock();

    /**
     * 缩小记录值,保证不会溢出
     */
    private void checkTotalCount() {
        int a = totalConsumeCount.get();
        int b = totalProduceCount.get();
        if (a > MILLION && b > MILLION) {
            if (lock.tryLock()) {
                try {
                    a = totalConsumeCount.get();
                    b = totalProduceCount.get();
                    if (a > MILLION && b > MILLION) {
                        totalProduceCount.addAndGet(-MILLION);
                        totalConsumeCount.addAndGet(-MILLION);
                        millionCount.incrementAndGet();
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                Thread.yield();
            }
        }
    }

    @Override
    public void threadRun() {
        runThreadCount.incrementAndGet();
    }

    @Override
    public void threadWait() {
        runThreadCount.decrementAndGet();
    }

    @Override
    public void threadReady() {
        totalThreadCount.incrementAndGet();
    }

    @Override
    public void threadShutDown() {
        totalThreadCount.decrementAndGet();
    }

    private void check() {
        long time = System.currentTimeMillis();
        time = time - time % windowsLength;
        SentinelEvent noticeEven = getNoticeEvent(time);
        listenerList.forEach(listenerList -> CompletableFuture.runAsync(() -> listenerList.notice(noticeEven)));
    }

    public void start() {
        scheduledThreadPoolExecutor.scheduleAtFixedRate(this::check, windowsLength * checkInterval, windowsLength * checkInterval, TimeUnit.MILLISECONDS);
    }
}
