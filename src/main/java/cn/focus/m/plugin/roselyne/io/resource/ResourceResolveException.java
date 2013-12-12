package cn.focus.m.plugin.roselyne.io.resource;

public class ResourceResolveException extends Exception {


    private static final long serialVersionUID = 6266856469631041840L;

    public ResourceResolveException() {
        super();
    }
    
    public ResourceResolveException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ResourceResolveException(String message) {
        super(message);
    }

    public ResourceResolveException(Throwable cause) {
        super(cause);
    }
}
