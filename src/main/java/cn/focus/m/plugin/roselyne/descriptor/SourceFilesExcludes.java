package cn.focus.m.plugin.roselyne.descriptor;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("excludes")
public class SourceFilesExcludes {

    @XStreamImplicit(itemFieldName="exclude")
    private List<String> exclude;

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }

    @Override
    public String toString() {
        return "FISSourceFilesExcludes [exclude=" + exclude + "]";
    }
}
