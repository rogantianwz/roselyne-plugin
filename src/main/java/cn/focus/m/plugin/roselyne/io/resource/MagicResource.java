package cn.focus.m.plugin.roselyne.io.resource;

import java.util.List;
import java.util.regex.Pattern;

public class MagicResource extends Resource{

    private List<Pattern> includePatterns;
    
    private List<Pattern> excludePatterns;

    public List<Pattern> getIncludePatterns() {
        return includePatterns;
    }

    public void setIncludePatterns(List<Pattern> includePatterns) {
        this.includePatterns = includePatterns;
    }

    public List<Pattern> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<Pattern> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MagicResource[");
        sb.append("output:").append(super.getOutputAddr()).append(" ")
            .append("release:").append(super.getReleaseAddr()).append(" ")
            .append("temp:").append(super.getTempAddr()).append(" ");
        
        if (null != includePatterns) {
            sb.append("includes:");
            for (Pattern p : includePatterns) {
                sb.append(p).append("  ");
            }
        }
        
        if (null != excludePatterns) {
            sb.append("excludes:");
            for (Pattern p : excludePatterns) {
                sb.append(p).append("  ");
            }
        }
        
        sb.append("]");
        return sb.toString();
    } 
}
