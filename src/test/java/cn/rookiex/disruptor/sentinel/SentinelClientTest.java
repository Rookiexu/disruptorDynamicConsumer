package cn.rookiex.disruptor.sentinel;

import org.junit.Test;

public class SentinelClientTest {

    @Test
    public void getCurrentWindow() {

        SentinelClient sentinelClient = new SentinelClient();
        Window currentWindow = sentinelClient.getCurrentWindow(System.currentTimeMillis());
         currentWindow = sentinelClient.getCurrentWindow(System.currentTimeMillis());
         currentWindow = sentinelClient.getCurrentWindow(System.currentTimeMillis());
         currentWindow = sentinelClient.getCurrentWindow(System.currentTimeMillis());
    }
}