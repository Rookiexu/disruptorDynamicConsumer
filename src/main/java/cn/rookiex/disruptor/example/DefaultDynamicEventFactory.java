package cn.rookiex.disruptor.example;

import cn.rookiex.disruptor.core.HandlerEvent;
import com.lmax.disruptor.EventFactory;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/13 11:11
 * @Describe :
 * @version:
 */
public class DefaultDynamicEventFactory implements EventFactory<HandlerEvent> {
    @Override
    public HandlerEvent newInstance() {
        return new HandlerEvent();
    }
}
