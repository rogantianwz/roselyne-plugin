package cn.focus.m.plugin.roselyne.descriptor;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("file")
public class SourceFile {

    @XStreamAlias("source")
    private String source;
    
    @XStreamAlias("output")
    private String output;
    
    @XStreamAlias("release")
    private String release;
    
    @XStreamAlias("md5")
    private boolean md5 = false;
    
    @XStreamAlias("md5len")
    private int md5len;
    
    @XStreamAlias("filtered")
    private boolean filtered = false;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public boolean isMd5() {
        return md5;
    }

    public void setMd5(boolean md5) {
        this.md5 = md5;
    }

    public int getMd5len() {
        return md5len;
    }

    public void setMd5len(int md5len) {
        this.md5len = md5len;
    }

    @Override
    public String toString() {
        return "FISSourceFile [source=" + source + ", output=" + output + ", release=" + release + ", md5=" + md5
                + ", md5len=" + md5len + ", filtered=" + filtered + "]";
    }

        
    
}
