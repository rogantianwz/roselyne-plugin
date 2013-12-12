package cn.focus.m.plugin.roselyne.descriptor;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("fis")
public class Config {

    @XStreamAlias("id")
    private String id;
    
    @XStreamAlias("name")
    private String name;
    
    @XStreamAlias("files")
    private List<SourceFile> files;
    
    @XStreamAlias("fileSets")
    private List<SourceFileSet> fileSets;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SourceFile> getFiles() {
        return files;
    }

    public void setFiles(List<SourceFile> files) {
        this.files = files;
    }

    public List<SourceFileSet> getFileSets() {
        return fileSets;
    }

    public void setFileSets(List<SourceFileSet> fileSets) {
        this.fileSets = fileSets;
    }

    @Override
    public String toString() {
        return "FISConfig [id=" + id + ", name=" + name + ", files=" + files + ", fileSets=" + fileSets + "]";
    }
    
}
