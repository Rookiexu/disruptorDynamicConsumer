package cn.rookiex.disruptor.sentinel;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/8 15:51
 * @Describe :
 * @version: 1.0
 */
public class SentinelEvent {
    private int totalProduceCount;

    private int totalConsumeCount;

    private int recentProduceCount;

    private int recentConsumeCount;
    private int millionCount;
    private long time;
    private int totalThreadCount;
    private int runThreadCount;

    public int getTotalProduceCount() {
        return totalProduceCount;
    }

    public void setTotalProduceCount(int totalProduceCount) {
        this.totalProduceCount = totalProduceCount;
    }

    public int getTotalConsumeCount() {
        return totalConsumeCount;
    }

    public void setTotalConsumeCount(int totalConsumeCount) {
        this.totalConsumeCount = totalConsumeCount;
    }

    public int getRecentProduceCount() {
        return recentProduceCount;
    }

    public void setRecentProduceCount(int recentProduceCount) {
        this.recentProduceCount = recentProduceCount;
    }

    public int getRecentConsumeCount() {
        return recentConsumeCount;
    }

    public void setRecentConsumeCount(int recentConsumeCount) {
        this.recentConsumeCount = recentConsumeCount;
    }

    public void setMillionCount(int millionCount) {
        this.millionCount = millionCount;
    }

    public int getMillionCount() {
        return millionCount;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public int getRecentDifference() {
        return recentProduceCount - recentConsumeCount;
    }

    public int getTotalDifference() {
        return totalProduceCount - totalConsumeCount;
    }

    public void setTotalThreadCount(int currentThreadCount) {
        this.totalThreadCount = currentThreadCount;
    }

    public int getTotalThreadCount() {
        return totalThreadCount;
    }

    public void setRunThreadCount(int runThreadCount) {
        this.runThreadCount = runThreadCount;
    }

    public int getRunThreadCount() {
        return runThreadCount;
    }
}
