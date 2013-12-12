package cn.focus.m.plugin.roselyne;

import cn.focus.m.plugin.roselyne.MessageHolder.MessageType;

public class Message {

    private MessageType type;
    
    private String content;

    public Message() {
        super();
        // TODO Auto-generated constructor stub
    }

    public Message(MessageType type, String content) {
        super();
        this.type = type;
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
