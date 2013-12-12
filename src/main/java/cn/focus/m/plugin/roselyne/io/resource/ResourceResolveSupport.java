package cn.focus.m.plugin.roselyne.io.resource;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.MessageHolder;
import cn.focus.m.plugin.roselyne.compress.CompressConfig;

public interface ResourceResolveSupport extends MessageHolder{

    /**
     * 获取临时文件存放目录
     * @return
     */
    String getTempDir();
    
    /**
     * 获取编码格式
     * @return
     */
    String getEncoding();
    
    /**
     * 打印解析过程中的日志
     * @param msg
     */
    void printResolveLog(Message msg);
    
    /**
     * 获取资源压缩使用的参数,参考 <link>https://github.com/yui/yuicompressor</link>
     * 
     * @return
     */
    CompressConfig getCompressConfig();
}
