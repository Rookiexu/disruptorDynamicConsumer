# disruptorDynamicConsumer
disruptor的动态消费者实现

## 版本补丁

20191231 如果需要使用这个项目需要对disruptor进行一些修改!!!

最近有朋友向我咨询了一个消费者扣除之后不能在处理消息的bug,这个问题实际上是因为删除消费者的时候,disruptor会丢失一个消息,在浏览器端感觉就是有消息被阻塞了.具体可以参考我给disruptor的pr=>[https://github.com/LMAX-Exchange/disruptor/pull/287](https://github.com/LMAX-Exchange/disruptor/pull/287 "pull request")

## 动态消费者 ##

**为什么要实现动态消费者?**

在我的实际使用中,disruptor配合netty处理客户端发送来的消息,使用的是多生产者多消费者的模式,disruptor在多消费者的模式下,每个消费者会对应一个线程,正常情况下系统会运行良好,但是一个系统往往会依赖一些其他系统或者执行一些长io的操作,这种情况下线程就会被阻塞.只是一部分线程阻塞还好,但是如果请求很多,所有线程都阻塞了,系统就不能提供服务了.

解决这个问题有治标的方法也有治本的方法.

治本: 所有依赖其他系统的操作异步化,所有涉及io的操作异步化.对于有问题的系统执行降级限流熔断一系列的操作.

这个方法几乎可以解决所有的问题,但是,每个项目都有自己的历史包袱,并不是所有系统都能用这样的方式来解决,因为可能发生阻塞的地方非常多,将操作重构为异步又需要消耗很多时间,而你的项目或者公司或许并不能给到这么多的时间

所以我给出了这个可以治标的方法:**将disruptor的消费者数量改为动态的**,在大部分消费者阻塞或者处理变慢的时候新增加消费者提供服务.

这个方法不能够彻底的解决问题,但是可以把系统的承载能力向上提示很大一部分.这样,可以一方面将阻塞方法一个一个改成异步,同时能短时间优化系统不能提供服务的问题.

## 项目实现

路径-> [https://github.com/Rookiexu/disruptorDynamicConsumer/blob/master/doc/HowToWork.md](https://github.com/Rookiexu/disruptorDynamicConsumer/blob/master/doc/HowToWork.md "how to work")

## 基本思路 ##

**disruptorDynamicConsumer**的基本思路是:

1. **DynamicDisruptor** : 负责启动**disruptor**,提供放入消息,增加消费者,减少消费者的方法(消费者数量修改方法是同步的),监听**SentinelClient**类对消息处理情况的信息推送,通过内置的**RegulateStrategy**类来调节消费者数量
2. **SentinelClient** : 收集消息处理情况,通过窗口算法,收集每个窗口收到的消息数量,处理的消息数量,收集当前运行handler数量等数据,每隔一定的窗口时间后,将监控的消息整理成一个**SentinelEvent**,发送给所有已注册的**SentinelListener**
3. **RegulateStrategy** : 调节数量的策略类,包含PID调节策略,和一个simple调节策略
	1.  **ProportionStrategy**  比例调节策略
	2.  **IntegralStrategy**  积分调节策略
	3.  **DerivativeStrategy** 微分调节策略
	4.  **SimpleStrategy** 简单调节策略
	5.  **PIDStrategy** PID混合调节策略 setPID(p,i,d)

## guide ##

在自定义的类中启动**DynamicDisruptor**

	//基础使用示例:

	int initSize = 2;//启动的时候使用的消费者数量
    int coreSize = 8;//消费者核心数量,类似于线程池核心线程池数量
	int maxSize = 64;//最大的消费者数量
    DynamicDisruptor server = new DynamicDisruptor(name, initSize, coreSize, maxSize);

	int windowsLength = 1000;//一个小窗口的长度,单位毫秒
	int windowsSzie = 10;//windowsSzie个窗口进行一次数量检测
    SentinelClient sentinelClient = new SentinelClient(windowsLength, windowsSzie);

	int bufferSize = 1024//ringBuffer长度 2的n次方
    server.init(bufferSize, sentinelClient, new ExampleHandlerFactory());
    server.start();

	//其他可扩展项:

	//消费者修改策略类
	PIDStrategy strategy = new PIDStrategy();//PID修改线程数量策略
	strategy.setPID(60,60,60);//PID控制比例,0到100的范围,可以根据需求调整
	
	server.setStrategy(strategy);

	//异常信息处理类
	ExceptionHandler<HandlerEvent> exceptionHandler = new ExceptionHandler<>() {
            @Override
            public void handleEventException(Throwable ex, long sequence, Object event) {
                //do sth
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                //do sth
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                //do sth
            }
        }
	server.setExceptionHandler(exceptionHandler);


    //处理类继承AbstractSentinelHandler
    
    @Override
    public void deal(HandlerEvent event) throws Exception {
        int id = event.getId();
        //do sth
    }

	
	




 
