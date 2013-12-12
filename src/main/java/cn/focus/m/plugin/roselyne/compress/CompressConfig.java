package cn.focus.m.plugin.roselyne.compress;

public class CompressConfig {

    private String charSet;
    
    private int lineBreak;
    
    private boolean noMunge;
    
    private boolean preserveSemi;
    
    private boolean disableOptimaztion;
    
    private boolean verbose;

    public String getCharSet() {
        return charSet;
    }

    public void setCharSet(String charSet) {
        this.charSet = charSet;
    }

    public boolean isNoMunge() {
        return noMunge;
    }

    public void setNoMunge(boolean noMunge) {
        this.noMunge = noMunge;
    }

    public boolean isPreserveSemi() {
        return preserveSemi;
    }

    public void setPreserveSemi(boolean preserveSemi) {
        this.preserveSemi = preserveSemi;
    }

    public boolean isDisableOptimaztion() {
        return disableOptimaztion;
    }

    public void setDisableOptimaztion(boolean disableOptimaztion) {
        this.disableOptimaztion = disableOptimaztion;
    }

    public int getLineBreak() {
        return lineBreak;
    }

    public void setLineBreak(int lineBreak) {
        this.lineBreak = lineBreak;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
