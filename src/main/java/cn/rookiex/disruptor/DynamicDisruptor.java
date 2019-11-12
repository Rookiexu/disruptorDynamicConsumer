package cn.rookiex.disruptor;

import cn.rookiex.disruptor.core.HandlerFactory;
import cn.rookiex.disruptor.core.SentinelHandler;
import cn.rookiex.disruptor.core.DynamicConsumer;
import cn.rookiex.disruptor.core.HandlerEvent;
import cn.rookiex.disruptor.example.DefaultHandlerFactory;
import cn.rookiex.disruptor.sentinel.SentinelClient;
import cn.rookiex.disruptor.sentinel.SentinelEvent;
import cn.rookiex.disruptor.sentinel.SentinelListener;
import cn.rookiex.disruptor.strategy.PIDStrategy;
import cn.rookiex.disruptor.strategy.ProportionStrategy;
import cn.rookiex.disruptor.strategy.RegulateStrategy;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ExceptionHandlerWrapper;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/8 10:16
 * @Describe :
 * @version:
 */
public class DynamicDisruptor implements DynamicConsumer, SentinelListener {

    public static final int NU_USED = 0, USED = 1, STATE_CHANGE = 2;
    public static final int DEFAULT_INIT_SIZE = 32;
    public static final int DEFAULT_CORE_SIZE = 32;
    public static final int DEFAULT_MAX_SIZE = 256;

    private String name;

    private int initSize;

    private int coreSize;

    private int maxSize;

    private SentinelClient sentinelClient;


    public DynamicDisruptor(String name) {
        this.name = name;
        this.initSize = DEFAULT_INIT_SIZE;
        this.coreSize = DEFAULT_CORE_SIZE;
        this.maxSize = DEFAULT_MAX_SIZE;
    }

    public DynamicDisruptor(String name, int initSize, int coreSize, int maxSize) {
        this.name = name;
        this.initSize = initSize;
        this.coreSize = coreSize;
        this.maxSize = maxSize;
    }

    /**
     * 工作的序列
     */
    private final Sequence workSequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);

    /**
     * 异常handler
     */
    private ExceptionHandler<HandlerEvent> exceptionHandler = new ExceptionHandlerWrapper<>();

    /**
     * 工作的processor数组
     */
    private WorkProcessor[] processors;

    /**
     * 工作的handler数组,和processor数组一一对应
     */
    private SentinelHandler[] handlers;

    /**
     * 和processor,handler一一对应,标识位置上是否空闲,用Atomic封装了cas操作
     */
    private AtomicIntegerArray availableArray;

    /**
     * 工作线程池
     */
    private ExecutorService executor;

    /**
     * Disruptor
     */
    private Disruptor<HandlerEvent> disruptor;

    /**
     * 线程取名字用的同步int
     */
    private AtomicInteger threadName = new AtomicInteger();

    /**
     * handler 工厂
     */
    private HandlerFactory handlerFactory = new DefaultHandlerFactory();

    /**
     * 默认的处理策略是比例控制
     */
    private RegulateStrategy strategy = new PIDStrategy();

    public Disruptor<HandlerEvent> getDisruptor() {
        return disruptor;
    }

    public ExceptionHandler<HandlerEvent> getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandler<HandlerEvent> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void init(int bufferSize, SentinelClient sentinelClient, HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
        init(bufferSize, sentinelClient);
    }

    public void init(int bufferSize, SentinelClient sentinelClient) {
        this.processors = new WorkProcessor[maxSize];
        this.handlers = new SentinelHandler[maxSize];
        this.availableArray = new AtomicIntegerArray(maxSize);
        this.sentinelClient = sentinelClient;
        this.handlerFactory.setSentinelClient(sentinelClient);
        this.sentinelClient.addListener(this);

        this.executor = new ThreadPoolExecutor(coreSize, maxSize,
                120L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("disruptorHandler" + "_" + name + "_" + threadName.incrementAndGet());
                    return t;
                });

        this.disruptor = new Disruptor<>(HandlerEvent::new, bufferSize, r -> {
            Thread t = new Thread(r);
            t.setName("disruptorCore" + "_" + name);
            return t;
        });

        for (int i = 0; i < initSize; i++) {
            SentinelHandler handlerEvent = createHandler();
            handlers[i] = handlerEvent;
            processors[i] = createProcessor(handlerEvent);
            updateUseState(i, USED);
        }

        disruptor.start();
    }

    public void start() {
        RingBuffer<HandlerEvent> ringBuffer = disruptor.getRingBuffer();
        for (WorkProcessor processor : processors) {
            if (processor != null) {
                ringBuffer.addGatingSequences(processor.getSequence());
                executor.execute(processor);
            }
        }
    }

    private SentinelHandler createHandler() {
        return handlerFactory.createHandler();
    }

    private WorkProcessor<HandlerEvent> createProcessor(SentinelHandler disruptorHandler) {
        RingBuffer<HandlerEvent> ringBuffer = disruptor.getRingBuffer();
        return new WorkProcessor<>(ringBuffer, ringBuffer.newBarrier(), disruptorHandler, exceptionHandler, workSequence);
    }

    @Override
    public void incrConsumer() {
        int nextUnUsed = getNextUnUsed();
        if (nextUnUsed == -1) {
            //没有空位和大部分都在状态切换的时候
            System.out.println("no available index exits ==> ");
        } else {
            RingBuffer<HandlerEvent> ringBuffer = disruptor.getRingBuffer();
            SentinelHandler disruptorHandler = createHandler();
            WorkProcessor<HandlerEvent> processor = createProcessor(disruptorHandler);

            WorkProcessor processor1 = processors[nextUnUsed];
            SentinelHandler handler = handlers[nextUnUsed];
            //容错,但实际上不应该出现,出现就是严重bug
            if (processor1 != null || handler != null) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("create disruptor thread ,but thread is exits");
                if (processor1 == null || handler == null) {
                    System.out.println("create disruptor thread ,handler == " + handler + " ,processor == " + processor);
                } else {
                    processor1.halt();
                    try {
                        handler.awaitShutdown();
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                    ringBuffer.removeGatingSequence(processor.getSequence());
                }
            }

            processors[nextUnUsed] = processor;
            handlers[nextUnUsed] = disruptorHandler;

            ringBuffer.addGatingSequences(processor.getSequence());
            executor.execute(processor);
            updateUseState(nextUnUsed, USED);
        }
    }

    @Override
    public void decrConsumer() {
        int nextUnUsed = getNextUsed();
        if (nextUnUsed == -1) {
            //已经小于等于核心数量的时候
            System.out.println("used thread less than core size");
        } else {
            RingBuffer<HandlerEvent> ringBuffer = disruptor.getRingBuffer();
            WorkProcessor processor = processors[nextUnUsed];
            SentinelHandler handler = handlers[nextUnUsed];
            if (processor == null || handler == null) {
                System.out.println("remove disruptor thread ,handler == " + handler + " ,processor == " + processor);
            }
            if (processor != null && handler != null) {
                processor.halt();
                try {
                    handler.awaitShutdown();
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                ringBuffer.removeGatingSequence(processor.getSequence());
            }

            processors[nextUnUsed] = null;
            handlers[nextUnUsed] = null;
            updateUseState(nextUnUsed, NU_USED);
        }
    }

    public void publishEvent(EventTranslatorVararg<HandlerEvent> translator, Object... args) {
        this.disruptor.getRingBuffer().publishEvent(translator, args);
        sentinelClient.addProduceCount();
    }

    private int getNextUsed() {
        int count = 0;
        for (int i = 0; i < maxSize; i++) {
            if (availableArray.get(i) == USED) {
                count++;
            }
            if (count > coreSize) {
                if (availableArray.compareAndSet(i, USED, STATE_CHANGE)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getNextUnUsed() {
        for (int i = 0; i < maxSize; i++) {
            if (availableArray.compareAndSet(i, NU_USED, STATE_CHANGE)) {
                return i;
            }
        }
        return -1;
    }

    private void updateUseState(int index, int state) {
        availableArray.set(index, state);
    }

    @Override
    public void notice(SentinelEvent sentinelEvent) {
        strategy.regulate(this, sentinelEvent);
    }

    public void setStrategy(RegulateStrategy strategy) {
        this.strategy = strategy;
    }

    public void setHandlerFactory(HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
}
