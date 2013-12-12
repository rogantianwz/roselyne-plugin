package cn.focus.m.plugin.roselyne.io.resource;

/**
 * 非法资源类型异常
 * @author rogantian
 *
 */
public class ResourceInvalidException extends Exception {

    private static final long serialVersionUID = -6141863853175277940L;

    public ResourceInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceInvalidException(String message) {
        super(message);
    }

    public ResourceInvalidException() {
        super();
    }

}
