# disruptorDynamicConsumer
disruptor的动态消费者实现

## 动态消费者 ##

**为什么要实现动态消费者?**

在我的实际使用中,disruptor配合netty处理客户端发送来的消息,使用的是多生产者多消费者的模式,disruptor在多消费者的模式下,每个消费者会对应一个线程,正常情况下系统会运行良好,但是一个系统往往会依赖一些其他系统或者执行一些长io的操作,这种情况下线程就会被阻塞.只是一部分线程阻塞还好,但是如果请求很多,所有线程都阻塞了,系统就不能提供服务了.

解决这个问题有治标的方法也有治本的方法.

治本: 所有依赖其他系统的操作异步化,所有涉及io的操作异步化.对于有问题的系统执行降级限流熔断一系列的操作.

这个方法几乎可以解决所有的问题,但是,每个项目都有自己的历史包袱,并不是所有系统都能用这样的方式来解决,因为可能发生阻塞的地方非常多,将操作重构为异步又需要消耗很多时间,而你的项目或者公司或许并不能给到这么多的时间

治标:将disruptor的消费者数量改为动态的,在大部分消费者阻塞或者处理变慢的时候新增加消费者提供服务.

这个方法不能够彻底的解决问题,但是可以把系统的承载能力向上提示很大一部分.这样,可以一方面将阻塞方法一个一个改成异步,同时能短时间优化系统不能提供服务的问题.

### disruptor实现动态消费者 ###

在我的项目**disruptorDynamicConsumer**具体的实现思路和核心代码如下:

	初始化三个数组,分别保存处理器和handler,最后一个是同步数组,三个数组位置一一对应,通过同步数组获得需要新增或者删除的处理器handler的位置

	private WorkProcessor[] processors;

    private SentinelHandler[] handlers;

    private AtomicIntegerArray availableArray;

	public void init(int bufferSize, SentinelClient sentinelClient) {
		//初始化三个数组
        this.processors = new WorkProcessor[maxSize];
        this.handlers = new SentinelHandler[maxSize];
        this.availableArray = new AtomicIntegerArray(maxSize);

		//初始化其他参数

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
		//启动disruptor
        disruptor.start();
    }

	@Override
    public void incrConsumer() {
		//获取下一个没有被使用的位置
        int nextUnUsed = getNextUnUsed();
        if (nextUnUsed == -1) {
            //没有空位和大部分都在状态切换的时候
            System.out.println("no available index exits ==> ");
        } else {
            RingBuffer<HandlerEvent> ringBuffer = disruptor.getRingBuffer();
			//获得一个新的handler
            SentinelHandler disruptorHandler = createHandler();
			//获得一个新的处理器
            WorkProcessor<HandlerEvent> processor = createProcessor(disruptorHandler);

			//将处理器和handler放在对应的位置上
            processors[nextUnUsed] = processor;
            handlers[nextUnUsed] = disruptorHandler;
			
			//添加消费者
            ringBuffer.addGatingSequences(processor.getSequence());
            executor.execute(processor);
            updateUseState(nextUnUsed, USED);
        }
    }

	@Override
    public void decrConsumer() {
		//获得一个已经被使用的位置
        int nextUnUsed = getNextUsed();
        if (nextUnUsed == -1) {
            //已经小于等于核心数量的时候
            System.out.println("used thread less than core size");
        } else {
            RingBuffer<HandlerEvent> ringBuffer = disruptor.getRingBuffer();
			//通过获得的位置获取对应的处理器和handler
            WorkProcessor processor = processors[nextUnUsed];
            SentinelHandler handler = handlers[nextUnUsed];
            if (processor == null || handler == null) {
                System.out.println("remove disruptor thread ,handler == " + handler + " ,processor == " + processor);
            }

			//结束消费者
            if (processor != null && handler != null) {
                processor.halt();
                try {
                    handler.awaitShutdown();
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                ringBuffer.removeGatingSequence(processor.getSequence());
            }

			//清除数组对象
            processors[nextUnUsed] = null;
            handlers[nextUnUsed] = null;
            updateUseState(nextUnUsed, NU_USED);
        }
    }

	//因为消费者数量的调整很快会跟上,所以cas操作并不需要放在循环中,等待下一次cas操作就可以了
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
	

disruptor官方也提供了一个动态添加消费者的示例,[https://github.com/LMAX-Exchange/disruptor/blob/master/src/test/java/com/lmax/disruptor/example/DynamiclyAddHandler.java](https://github.com/LMAX-Exchange/disruptor/blob/master/src/test/java/com/lmax/disruptor/example/DynamiclyAddHandler.java "https://github.com/LMAX-Exchange/disruptor/blob/master/src/test/java/com/lmax/disruptor/example/DynamiclyAddHandler.java")


### 检测消费情况 ###
基于上面的代码,disruptor的动态消费者已经实现了,但是仅仅实现动态增减的方法还是不够,因为还没有随时检测并且调整消费者数量的功能.所以还需要一个采样算法的实现.

在**disruptorDynamicConsumer**中,同时支持滑动窗口和固定窗口算法来对disruptor的处理消息的情况采样.其实窗口算法更多的是应用在限流算法中,dubbo的默认负载均衡算法random就是通过滑动窗口设计的.直接通过限流算法来解释固定窗口和滑动窗口算法：  

固定窗口算法相对暴力,直接计算窗口内的请求的数量,如果超过某个值就拒绝请求.但是固定窗口有一个问题,以一秒为一个窗口为例,假设恶意用户在上一秒的后半部分发和下一秒的前半部分都发送了需要限流的数据,但是因为不在同一个窗口,限流算法会失效.

滑动窗口则可以通过更细粒度对数据进行统计.
假设我们将1s划分为4个窗口，则每个窗口对应250ms。假设恶意用户还是在上一秒的最后一刻和下一秒的第一刻冲击服务，按照滑动窗口的原理，此时统计上一秒的最后750毫秒和下一秒的前250毫秒，这种方式能够判断出用户的访问依旧超过了1s的访问数量，因此依然会阻拦用户的访问。



在**disruptorDynamicConsumer**中,窗口的获取和通知方法如下


	 public Window getCurrentWindow(long time) {
		//根据时间获得对应的小窗口index,并且获得旧的窗口对象
        long timeId = time / windowsLength;
        int idx = (int) (timeId % windowsSize);
        time = time - time % windowsLength;
        Window old = windows[idx];
        SentinelEvent noticeEvent = null;
        while (true) {
			//如果当前时间和窗口时间相等,跳出循环
            if (time == old.getSecondTime()) {
                break;
            } else if (time > old.getSecondTime()) {
				//如果窗口时间小于当前时间,说明旧的窗口已经过期,需要更换
                if (updateLock.tryLock()) {
                    try {
						//双重判定保证同步
                        if (time > old.getSecondTime()) {
							//如果index是检测index,生成一个检测事件,事件内保存了需要的检测数据                          
							if (idx % (checkInterval - 1) == 0 && idx != 0)
                                noticeEvent = getNoticeEvent(time);
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

		//如果检测数据不为空,通知订阅的listener
        if (noticeEvent != null) {
            SentinelEvent finalNoticeEvent = noticeEvent;
            listenerList.forEach(listenerList -> {
                CompletableFuture.runAsync(() -> listenerList.notice(finalNoticeEvent));
            });
        }
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

	//注意点在于,当checkInterval==窗口大小的时候,实际上就是固定窗口算法,当checkInterval<窗口大小的时候,则是滑动窗口算法	  

	//DynamicDisruptor就是其中的一个监听者,根据采样的event,触发DynamicDisruptor的添加消费者和减少消费者的算法

	

### PID调节控制 ###

通过窗口算法,DynamicDisruptor在每个采样窗口获得了disruptor的运行数据,但是怎么根据这个数据调整消费者数量,仍然没有解决.在**disruptorDynamicConsumer**中,选择了自动控制中经典的PID算法来控制消费者数量.

PID算法由Proportion(比例控制),Integral(积分控制),Derivative(微分控制)三部分组成.

以生产者消费者模式为例:

1. 假设生产者消费者速率一致,同时我们期望在队列中的消息一直控制在100以内,生产者数量可能随时发生变化,我们通过调整消费者数量来实现消息队列中消息不堆积,一直保持在100以内的量级.
2. 如果生产者突然增多了一倍,为了保持生成消费的平衡,消费者也需要成比例的增加,这就是p比例控制
3. 比例控制可以保证生成消费的平衡,但是不能保证已经堆积在队列中的消息被消耗掉,所以如果已经堆积了一部分消息,还需要再增加一部分线程来处理已经堆积的消息.这部分就是积分控制
4. 
	5. 比例和积分保证了队列数量可以在调整之后达到设计的预期,但是不管是比例还是积分,其实都是滞后于生成者变化信息的.
	6. 如果生产者在某个时间内急剧增多,当比例和积分开始调整的时候,生产者可能已经再次翻倍了,这样的情况下,处理可能很不及时,为此还需要引出微分
	7. 为了能响应及时,比较之前的窗口数据和当前窗口数据的变化率,如果变化率很大,则提前增加更多的消费者,如果变化率倾向缩小甚至为负,就提前减少一部分消费者,这就是微分控制


总结起来,简单的解释就是,P是控制现在，I是纠正曾经，D是管控未来.

核心代码如下:

	 public static void updateThreadCount(DynamicDisruptor dynamicDisruptor, int needUpdateCount) {
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

	//PID策略

	private ProportionStrategy proportionStrategy = new ProportionStrategy();
    private IntegralStrategy integralStrategy = new IntegralStrategy();
    private DerivativeStrategy derivativeStrategy = new DerivativeStrategy();

    //pid控制的比例系数
    private int p = 66;
    private int i = 66;
    private int d = 66;

    @Override
    public void regulate(DynamicDisruptor dynamicDisruptor, SentinelEvent sentinelEvent) {
        RegulateStrategy.updateThreadCount(dynamicDisruptor, getNeedUpdateCount(sentinelEvent));
    }

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        //调用pid控制器
        int pCount = proportionStrategy.getNeedUpdateCount(sentinelEvent) * p / 100;
        int iCount = integralStrategy.getNeedUpdateCount(sentinelEvent) * i / 100;
        int dCount = derivativeStrategy.getNeedUpdateCount(sentinelEvent) * d / 100;
		int simpleCount = simpleStrategy.getNeedUpdateCount(sentinelEvent);
        return pCount + iCount + dCount + simpleCount;
    }

	//比例控制策略
	@Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int recentConsumeCount = sentinelEvent.getRecentConsumeCount();
        int recentProduceCount = sentinelEvent.getRecentProduceCount();

        //thread info
        int totalThreadCount = sentinelEvent.getTotalThreadCount();
        int runThreadCount = sentinelEvent.getRunThreadCount();

        int updateCount = 0;

        boolean isThreadRunOut = runThreadCount == totalThreadCount;
        if (isThreadRunOut) {
            if (recentProduceCount > recentConsumeCount){
                //保留两位小数
                int needAddThread = (recentProduceCount * 100 / recentConsumeCount * runThreadCount)  / 100 - runThreadCount;
                updateCount += needAddThread;
            }
        }

        return updateCount;
    }

	//积分控制策略
	@Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int updateCount = 0;
        int totalDifference = sentinelEvent.getTotalDifference();
        int recentConsumeCount = sentinelEvent.getRecentConsumeCount();
        int runThreadCount = sentinelEvent.getRunThreadCount();
        int totalThreadCount = sentinelEvent.getTotalThreadCount();
        if (totalThreadCount == runThreadCount){
            if (totalDifference > recentConsumeCount){
                //保留两位小数
                int needAddThread = (totalDifference * 100 / recentConsumeCount * runThreadCount)  / 100 - runThreadCount;
                updateCount += needAddThread;
            }
        }
        return updateCount;
    }

	//微分控制策略
	private int lastDifference = 0;

    @Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int updateCount = 0;
        if (lastDifference != 0) {
            int runThreadCount = sentinelEvent.getRunThreadCount();
            int totalThreadCount = sentinelEvent.getTotalThreadCount();
            if (totalThreadCount == runThreadCount){
                int err = sentinelEvent.getTotalDifference() - lastDifference;
                int needUpdate = err * 100 / sentinelEvent.getRecentConsumeCount()  * runThreadCount / 100;
                updateCount += needUpdate;
            }
        }
        lastDifference = sentinelEvent.getTotalDifference();
        return updateCount;
    }

	//simple控制策略,这个策略用在快速减少浪费的消费者数量和pid算法在消费者数量较多时,因为算法精度不足是弥补消费者数量上
	@Override
    public int getNeedUpdateCount(SentinelEvent sentinelEvent) {
        int totalDifference = sentinelEvent.getTotalDifference();
        int runThreadCount = sentinelEvent.getRunThreadCount();
        int totalThreadCount = sentinelEvent.getTotalThreadCount();

        int updateCount = 0;
        if (totalDifference < sentinelEvent.getRecentConsumeCount() && runThreadCount < totalThreadCount) {
            updateCount -= (totalThreadCount - runThreadCount) / 2 + (totalThreadCount - runThreadCount) % 2;
        }

        if (runThreadCount == totalThreadCount) {
            if (sentinelEvent.getRecentProduceCount() > sentinelEvent.getRecentConsumeCount()) {
                updateCount += 1;
            }
        }

        return updateCount;
    }


## 总结 ##
**disruptorDynamicConsumer**中除了动态的消费者,还实现了简单的滑动窗口算法和pid控制算法,三部分都是可以应用在很多地方的,希望可以给到读者一些帮助,如果有问题可以给我留言,如果觉得还不错请给个star吧,也欢迎fork我的项目,谢谢 [https://github.com/Rookiexu/disruptorDynamicConsumer](https://github.com/Rookiexu/disruptorDynamicConsumer "https://github.com/Rookiexu/disruptorDynamicConsumer")  
