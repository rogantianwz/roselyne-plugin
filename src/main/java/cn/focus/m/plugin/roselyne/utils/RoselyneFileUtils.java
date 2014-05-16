package cn.focus.m.plugin.roselyne.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

public final class RoselyneFileUtils {
    
    private static final Pattern PUNIFYSEPERATOR = Pattern.compile("[\\\\/]");
    
    /**
     * 匹配路径表达式 如D:\workspace1\focus-wap\src\main\webapp\views\.\home.html中的.\ 或者./
     */
    private static final Pattern PREMOVEDOTSLASH = Pattern.compile("(\\.\\\\|\\./)");
    
    /**
     * 匹配路径表达式 如 D:\workspace1\focus-wap\src\main\webapp\views\..\static\appwap.html中的views\..\ 或者views/../
     */
    private static final Pattern PREMOVEDOTDOTSLASH = Pattern.compile("([^\\\\/:\\*\\?\"<>]+\\\\\\.\\.\\\\|[^\\\\/:\\*\\?\"<>]+/\\.\\./)");

    private RoselyneFileUtils() {

    }

    /**
     * 获取相对路径
     * @param path
     * @param basedir
     * @return
     */
    public static String makePathRelativeTo(String path, File basedir) {
        if (null == basedir) {
            return path;
        }

        if (null == path) {
            return null;
        }

        path = path.trim();

        String base = basedir.getAbsolutePath();
        if (path.startsWith(base)) {
            path = path.substring(base.length());
            if (path.length() > 0) {
                if ((path.startsWith("/")) || (path.startsWith("\\"))) {
                    path = path.substring(1);
                }
            }

            if (path.length() == 0) {
                path = ".";
            }
        }

        if (!new File(path).isAbsolute()) {
            path = path.replace('\\', '/');
        }

        return path;
    }
    
    /**
     * 获取相对于baseFile的相对路径relativePath的绝对路径
     * @param baseFile
     * @param relativePath
     * @return
     */
    public static String makeAbsolutePath(String baseFile, String relativePath) {
        if (StringUtils.isBlank(baseFile)) {
            return null;
        }
        
        if (StringUtils.isBlank(relativePath)) {
            return baseFile;
        }
        
        baseFile = unifySeperator(baseFile);
        relativePath = unifySeperator(relativePath);
        
        File base = new File(baseFile);
        String dir = baseFile;
        StringBuilder ret = new StringBuilder();
        if (!base.isDirectory()) {
            int li = baseFile.lastIndexOf(File.separator);
            dir = baseFile.substring(0,li+1);
        }
        
        ret.append(dir);
        
        if (!dir.endsWith(File.separator)) {
            ret.append(File.separator);
        }
        
        if (relativePath.startsWith(File.separator)) {
            ret.append(relativePath.substring(0, relativePath.length() - 1));
        } else {
            ret.append(relativePath);
        }
        
       
        System.out.println(ret);
        return removeDot(ret.toString());
    }
    
    /**
     * 统一seperator,将路径中的seperator换成本地文件系统使用的seperator
     * @param source
     * @return
     */
    public static String unifySeperator(String source) {
        String seperator = File.separator;
        if (StringUtils.equals("\\", seperator)) {
            return PUNIFYSEPERATOR.matcher(source).replaceAll("\\\\"); 
        } else {            
            return PUNIFYSEPERATOR.matcher(source).replaceAll("/");
        }
    }
    
    /**
     * 移除路径中类似./和../的表达式，可以参考org.codehaus.plexus.util.FileUtils.normalize(String str)方法
     * @param source
     * @return
     */
    public static String removeDot(String source) {
        String ret = PREMOVEDOTDOTSLASH.matcher(source).replaceAll("");
        return PREMOVEDOTSLASH.matcher(ret).replaceAll("");
    }
    
    /**
     * 参考org.codehaus.plexus.util.FileUtils.fileRead(...)方法
     * @param f
     * @return
     * @throws IOException
     */
    @Deprecated
    public static String readFile(File f) throws IOException {
        BufferedReader br = null;
        StringBuffer sb = new StringBuffer();
        
        try {
            FileReader fr = new FileReader(f);
            br = new BufferedReader(fr);
            String line = br.readLine();
            while (null != line) {
                sb.append(line);
                line = br.readLine();
            }
        } finally {
            if (null != br) {
                br.close();
            }
        }
       return sb.toString(); 
    }
    
    /**
     * 参考org.codehaus.plexus.util.FileUtils.fileWrite(...)方法
     * @param seq
     * @param fileAddr
     * @return
     * @throws IOException
     */
    @Deprecated
    public static File writeFile(CharSequence seq, String fileAddr) throws IOException{
        
        if (null == seq || StringUtils.isBlank(fileAddr)) {
            return null;
        }
        
        File outputFile = new File(fileAddr);
        
        if (!outputFile.isFile()) {
            throw new IllegalStateException("outputAddr is not a file");
        }
        
        if (outputFile.exists()) {
            outputFile.delete();
        }
        
        outputFile.createNewFile();
        
        FileWriter fw = null;
        try {
            fw = new FileWriter(outputFile);
            fw.write(seq.toString());
            
        } finally {
            if (null != fw) {
                fw.close();
            }
        }
        
        return outputFile;
    }
    
    public static void fileWrite(File file, String encoding, String data) throws IOException{
        File dir = file.getParentFile();
        dir.mkdirs();
        
        if (!file.exists()) {
            file.createNewFile();
        }
        FileUtils.fileWrite(file, encoding, data);
    }
    
    public static String fileRead(File file, String encoding) throws IOException {
        return FileUtils.fileRead(file, encoding);
    }
    
    public static String fileMd5(File file, String encoding) throws IOException {
        return DigestUtils.md5Hex(fileRead(file, encoding));
    }
    
    /**
     * 给路径添加suffix，i.e http://www.baidu.com/abc.jpg  --》http://www.baidu.com/abc1121111.jpg
     * @param path
     * @param suffix
     * @return
     */
    public static String addSuffixBeforeExtension(String path, String suffix) {
        String oldPath = new String(path);
        StringBuilder ret = new StringBuilder();
        int idx = oldPath.lastIndexOf(".");
        if (idx != -1 && idx < oldPath.length()) {
            ret.append(oldPath.substring(0, idx)).append(suffix).append(oldPath.substring(idx));
        }
        return ret.toString();
    }
    
    /**
     * 如果targetPath是相对路径的话，返回其相对于currentPath的绝对路径，这里所说的路径仅指url路径
     * @param currentPath
     * @param relativePaht
     * @return
     */
    public static String normalizPathIfNeed(String currentPath, String targetPath) {
        if(targetPath.startsWith("http://") || targetPath.startsWith("/")) {
            return targetPath;
        }
        return tranRelativePathToAbsolutePath(currentPath, targetPath);
    }
    
    /**
     * 将相对路径转换为绝对路径，i.e http://www.baidu.com/a/b/1.jpg and ../../2.css  --》 http://www.baidu.com/2.css，这里所说的路径仅指url路径
     * @param currentPath 该路径必须是绝对路径，这里以"http://"开头作为判断
     * @param relativePath
     * @return
     */
    public static String tranRelativePathToAbsolutePath(String currentPath, String relativePath) {
        if (!currentPath.startsWith("http://")) {
            return null;
        }
        String path = null;
        int index = currentPath.lastIndexOf('/');
        if (index > 0) {
            path = currentPath.substring(0,index+1) + relativePath;
        } else {
            path = currentPath + relativePath;
        }
        
        while ( true )
        {
            int loc = path.indexOf( "/./" );
            if ( loc < 0 )
            {
                break;
            }
            path = path.substring( 0, loc ) + path.substring( loc + 2 );
        }

        // Resolve occurrences of "/../" in the normalized path
        while ( true )
        {
            int loc = path.indexOf( "/../" );
            if ( loc < 0 )
            {
                break;
            }
            if ( loc == 0 )
            {
                return null;  // Trying to go outside our context
            }
            int index2 = path.lastIndexOf( '/', loc - 1 );
            path = path.substring( 0, index2 ) + path.substring( loc + 3 );
        }
        
        return path;
    }
    
    /**
     * 测试makeAbsolutePath(...)方法
     * @param args
     */
    public static void main2(String[] args) {
        
        
        String baseFile = "D:\\workspace1\\focus-wap\\src/.\\main\\webapp\\views\\home.jsp";
        String relativePath = "../static\\appwap.html";
        String absolute = RoselyneFileUtils.makeAbsolutePath(baseFile, relativePath);
        System.out.println(absolute);
    }
    
    /**
     * 测试addSuffixBeforeExtension
     * @param args
     */
    public static void main3(String[] args) {
        String ret = RoselyneFileUtils.addSuffixBeforeExtension("http://www.baidu.com/abc.jpg", "121212121");
        System.out.println(ret);
    }
    
    /**
     * 测试normalizPathIfNeed方法
     * @param args
     */
    public static void main(String[] args) {
        /*String currentPath = "http://www.baidu.com/a/b/1.jpg";
        String relativePath = "../../2.css";*/
        String currentPath = "http://include.aifcdn.com/touchweb/2014_20_03_1/base/css/Base.css";
        //String relativePath = "../../touch/img/base_i_s.png";
        //String relativePath = "touch/img/base_i_s.png";
        //String relativePath = "/img/base_i_s.png";
        //String relativePath = "./img/base_i_s.png";
        String relativePath = "http://www.baidu.com/img/base_i_s.png";
        System.out.println(normalizPathIfNeed(currentPath, relativePath));
    }
    
}
