package cn.focus.m.plugin.roselyne;

import cn.focus.m.plugin.roselyne.io.resource.Resource;

/**
 * 消息持有者，用于各个模块在主模块中打印日志等
 * @author rogantian
 *
 */
public interface MessageHolder {

    enum MessageType {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        SEPERATOR
    }
    
    void addMessage(MessageType type, String content);
    
    void addMessage(Resource resource, Message msg);
    
}
