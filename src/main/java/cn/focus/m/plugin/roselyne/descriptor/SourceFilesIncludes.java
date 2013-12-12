package cn.focus.m.plugin.roselyne.descriptor;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

public class SourceFilesIncludes {

    @XStreamImplicit(itemFieldName="include")
    private List<String> include;

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    @Override
    public String toString() {
        return "FISSourceFilesIncludes [include=" + include + "]";
    }
}
