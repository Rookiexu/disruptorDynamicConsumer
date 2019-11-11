package cn.rookiex.disruptor.core;

/**
 * @Author : Rookiex
 * @Date : Created in 2019/11/8 10:31
 * @Describe :
 * @version:
 */
public class HandlerEvent {
    private int id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
