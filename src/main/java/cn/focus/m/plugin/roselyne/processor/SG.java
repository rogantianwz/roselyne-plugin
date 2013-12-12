package cn.focus.m.plugin.roselyne.processor;

public class SG {
    
    private String content;
    
    private int start;
    
    private int end;

    public SG(String content, int start, int end) {
        super();
        this.content = content;
        this.start = start;
        this.end = end;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "SG [content=" + content + ", start=" + start + ", end=" + end + "]";
    }
    
    
}
