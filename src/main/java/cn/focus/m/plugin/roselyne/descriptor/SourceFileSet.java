package cn.focus.m.plugin.roselyne.descriptor;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("fileSet")
public class SourceFileSet {

    @XStreamAlias("directory")
    private String directory;
    
    @XStreamAlias("excludes")
    private SourceFilesExcludes excludes;
    
    @XStreamAlias("filtered")
    private boolean filtered = false;
    
    @XStreamAlias("includes")
    private SourceFilesIncludes includes;
    
    @XStreamAlias("output")
    private String outpout;
    
    @XStreamAlias("release")
    private String release;
    
    @XStreamAlias("useDefaultExcludes")
    private boolean useDefaultExcludes = true;
    
    @XStreamAlias("md5")
    private boolean md5 = false;
    
    @XStreamAlias("md5len")
    private int md5len;
    
    @XStreamAlias("resolveImmeditely")
    private boolean resolveImmeditely = true;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public SourceFilesExcludes getExcludes() {
        return excludes;
    }

    public void setExcludes(SourceFilesExcludes excludes) {
        this.excludes = excludes;
    }

    public SourceFilesIncludes getIncludes() {
        return includes;
    }

    public void setIncludes(SourceFilesIncludes includes) {
        this.includes = includes;
    }

    public boolean isUseDefaultExcludes() {
        return useDefaultExcludes;
    }

    public void setUseDefaultExcludes(boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    public String getOutpout() {
        return outpout;
    }

    public void setOutpout(String outpout) {
        this.outpout = outpout;
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

    public boolean isResolveImmeditely() {
        return resolveImmeditely;
    }

    public void setResolveImmeditely(boolean resolveImmeditely) {
        this.resolveImmeditely = resolveImmeditely;
    }

    @Override
    public String toString() {
        return "FISSourceFileSet [directory=" + directory + ", excludes=" + excludes + ", filtered=" + filtered
                + ", includes=" + includes + ", outpout=" + outpout + ", release=" + release + ", useDefaultExcludes="
                + useDefaultExcludes + ", md5=" + md5 + ", md5len=" + md5len + ", resolveImmeditely="
                + resolveImmeditely + "]";
    }
    
    
}
