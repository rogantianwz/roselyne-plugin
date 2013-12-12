package cn.focus.m.plugin.roselyne;

public class InvalidConfigurationException extends Exception {
    
    private static final long serialVersionUID = -4259123693104263549L;
    
    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConfigurationException(String message) {
        super(message);
    }

}
