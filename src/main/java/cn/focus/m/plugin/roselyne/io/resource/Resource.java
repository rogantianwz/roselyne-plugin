package cn.focus.m.plugin.roselyne.io.resource;

import java.io.File;

public class Resource {

    private String sourceAddr;

    private String releaseAddr;
    
    private String outputAddr;
    
    private File tempAddr;
    
    private boolean md5;
    
    private int md5len;
    
    private boolean temp;
    
    private boolean done;
    
    private String versionCode;

    public String getReleaseAddr() {
        return releaseAddr;
    }

    public void setReleaseAddr(String releaseAddr) {
        this.releaseAddr = releaseAddr;
    }

    public String getOutputAddr() {
        return outputAddr;
    }

    public void setOutputAddr(String outputAddr) {
        this.outputAddr = outputAddr;
    }

    public String getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(String sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    public File getTempAddr() {
        return tempAddr;
    }

    public void setTempAddr(File tempAddr) {
        this.tempAddr = tempAddr;
    }

    public boolean isTemp() {
        return temp;
    }

    public void setTemp(boolean temp) {
        this.temp = temp;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceAddr == null) ? 0 : sourceAddr.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Resource other = (Resource) obj;
        if (sourceAddr == null) {
            if (other.sourceAddr != null)
                return false;
        } else if (!sourceAddr.equals(other.sourceAddr))
            return false;
        return true;
    }

    public int getMd5len() {
        return md5len;
    }

    public void setMd5len(int md5len) {
        this.md5len = md5len;
    }

    public boolean isMd5() {
        return md5;
    }

    public void setMd5(boolean md5) {
        this.md5 = md5;
    }

    @Override
    public String toString() {
        return "Resource [sourceAddr=" + sourceAddr + ", releaseAddr=" + releaseAddr + ", outputAddr=" + outputAddr
                + ", tempAddr=" + tempAddr + ", md5=" + md5 + ", md5len=" + md5len + ", temp=" + temp + ", done="
                + done + "]";
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }
}
