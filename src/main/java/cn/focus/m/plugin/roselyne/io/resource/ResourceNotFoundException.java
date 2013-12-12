package cn.focus.m.plugin.roselyne.io.resource;

/**
 * 在不能成功获取资源时抛出该异常
 * @author rogantian
 *
 */
public class ResourceNotFoundException extends Exception {
    
    private static final long serialVersionUID = 7731188256503894539L;

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    

}
