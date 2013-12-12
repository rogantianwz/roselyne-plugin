package cn.focus.m.plugin.roselyne.compress;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.MessageHolder;
import cn.focus.m.plugin.roselyne.MessageHolder.MessageType;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupportAdaptor;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * 资源压缩器，使用YUIcompressor实现</br>
 * 如果文件以.min.js或者.min.css结尾则不压缩
 * @author rogantian
 * @date 2013-11-26
 * @email rogantianwz@gmail.com
 */
public class ResourceCompressor {
    
    private CompressConfig config;
    
    public ResourceCompressor() {
        
    }
    
    public ResourceCompressor(ResourceResolveSupport support) {
        config = support.getCompressConfig();
        if (null == config) {
            config = new CompressConfig();
        }
        if (config.getLineBreak() <= 0) {
            config.setLineBreak(-1);
        }
        
        if (StringUtils.isBlank(config.getCharSet())) {
            config.setCharSet(support.getEncoding());
        }
    }

    
    public void compressResource(final Resource resource, final MessageHolder msgHolder) throws ResourceCompressException {
        String source = resource.getOutputAddr();
        String output = resource.getOutputAddr();
        Reader in = null;
        Writer out = null;
        String sourceType = null;
        
        try {
            in = new InputStreamReader(new FileInputStream(source), config.getCharSet());

            int idx = source.lastIndexOf(".");
            if (idx >= 0 && idx < source.length() - 1) {
                sourceType = source.substring(idx + 1);
            }
            
            if (null == sourceType || !sourceType.equalsIgnoreCase("js") && !sourceType.equalsIgnoreCase("css")) {
                StringBuilder msg = new StringBuilder("does not need compress beacause of it'name doesn't like *.js or *.css or not implict use 'type' config ");
                if (null != msgHolder) {
                    msgHolder.addMessage(resource, new Message(MessageType.INFO, msg.toString()));
                } else {
                    System.out.println("\n[INFO] " + msg.toString());
                }
                return;
            }
            
            if ("js".equals(sourceType)) {
                try {
                    if (source.endsWith(".min.js")) {
                        StringBuilder msg = new StringBuilder("does not need compress beacause of it'name like *.min.js");
                        if (null != msgHolder) {
                            msgHolder.addMessage(resource, new Message(MessageType.INFO, msg.toString()));
                        } else {
                            System.out.println("\n[INFO] " + msg.toString());
                        }
                        return;
                    }
                    JavaScriptCompressor compressor = new JavaScriptCompressor(in, new ErrorReporter() {

                        public void warning(String message, String sourceName,
                                int line, String lineSource, int lineOffset) {
                            if (line < 0) {
                                if (null == msgHolder) {
                                    System.err.println("\n[WARNING] " + message);
                                } else {
                                    msgHolder.addMessage(resource, new Message(MessageType.WARN, message));
                                }
                                
                            } else {
                                if (null == msgHolder) {
                                    System.err.println("\n[WARNING] " + line + ':' + lineOffset + ':' + message);
                                } else {
                                    msgHolder.addMessage(resource, new Message(MessageType.WARN, line + ':' + lineOffset + ':' + message));
                                }
                            }
                        }

                        public void error(String message, String sourceName,
                                int line, String lineSource, int lineOffset) {
                            if (line < 0) {
                                if (null == msgHolder) {
                                    System.err.println("\n[ERROR] " + message);
                                } else {
                                    msgHolder.addMessage(resource, new Message(MessageType.ERROR, message));
                                }
                            } else {
                                if (null == msgHolder) {
                                    System.err.println("\n[ERROR] " + line + ':' + lineOffset + ':' + message);
                                } else {
                                    msgHolder.addMessage(resource, new Message(MessageType.ERROR, line + ':' + lineOffset + ':' + message));
                                }
                            }
                        }

                        public EvaluatorException runtimeError(String message, String sourceName,
                                int line, String lineSource, int lineOffset) {
                            error(message, sourceName, line, lineSource, lineOffset);
                            return new EvaluatorException(message);
                        }
                    });
                    
                    // Close the input stream first, and then open the output stream,
                    // in case the output file should override the input file.
                    in.close(); in = null;
                    
                    out = new OutputStreamWriter(new FileOutputStream(output), config.getCharSet());
                    compressor.compress(out, config.getLineBreak(), config.isNoMunge(), config.isVerbose(), config.isPreserveSemi(), config.isDisableOptimaztion());
                    
                } catch (EvaluatorException e) {
                    StringBuilder err = new StringBuilder("compress error because of:").append(e.getMessage());
                    if (null != msgHolder) {
                        msgHolder.addMessage(resource, new Message(MessageType.ERROR,""));
                    } else {
                        System.out.println("\n[ERROR] " + err);
                    }
                    throw new ResourceCompressException();
                }
            } else {
                if (source.endsWith(".min.css")) {
                    StringBuilder msg = new StringBuilder("] does not need compress beacause of it'name like *.min.css");
                    if (null != msgHolder) {
                        msgHolder.addMessage(resource, new Message(MessageType.INFO, msg.toString()));
                    } else {
                        System.out.println("\n[INFO] " + msg.toString());
                    }
                    return;
                }
                CssCompressor compressor = new CssCompressor(in);

                // Close the input stream first, and then open the output stream,
                // in case the output file should override the input file.
                in.close(); in = null;
                out = new OutputStreamWriter(new FileOutputStream(output), config.getCharSet());
                compressor.compress(out, config.getLineBreak());
            }
        } catch (IOException e) {
            StringBuilder err = new StringBuilder("compress error because of:").append(e.getMessage());
            if (null != msgHolder) {
                msgHolder.addMessage(resource, new Message(MessageType.ERROR,""));
            } else {
                System.out.println("\n[ERROR] " + err);
            }
            throw new ResourceCompressException();
        } finally {
            IOUtil.close(in);
            IOUtil.close(out);
        }
    }
    
    public static void main(String[] args) throws ResourceCompressException {
        Resource r = new Resource();
        r.setSourceAddr("D:\\yui\\common.js");
        r.setOutputAddr("D:\\yui\\common.js");
        new ResourceCompressor(new ResourceResolveSupportAdaptor(){

            @Override
            public String getEncoding() {
                return "UTF-8";
            }

            @Override
            public CompressConfig getCompressConfig() {
                return new CompressConfig();
            }
            
        }).compressResource(r, null);
        
    }
}
