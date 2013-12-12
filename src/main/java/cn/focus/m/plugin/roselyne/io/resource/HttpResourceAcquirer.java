package cn.focus.m.plugin.roselyne.io.resource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpResourceAcquirer implements ResourceAcquirer {

    public File acquireResource(String resourceURL, String tempDir) throws ResourceNotFoundException, ResourceInvalidException{
        URL url;
        try {
            url = new URL(resourceURL);
        } catch (MalformedURLException e1) {
            throw new ResourceInvalidException();
        }
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        File outputFile = null;
        try {
            int httpResult = 0;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            httpResult = conn.getResponseCode();
            if (httpResult != HttpURLConnection.HTTP_OK) {
                throw new ResourceNotFoundException(resourceURL);
            } else {
                input = new BufferedInputStream(conn.getInputStream());
                File outputDir = new File(tempDir);
                outputFile = new File(outputDir, String.valueOf(System.currentTimeMillis()));
                output = new BufferedOutputStream(new FileOutputStream(outputFile));
                byte[] buffer = new byte[1024];
                int num = -1;
                while (true) { 
                    num = input.read(buffer); 
                    if (num ==-1){ 
                        output.flush(); 
                        break;
                    }
                    output.flush(); 
                    output.write(buffer,0,num); 
                } 
            }
        } catch (IOException e) {
            throw new ResourceNotFoundException(resourceURL);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
            
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }
        return outputFile;
    }
    
    public static void main(String[] args) throws ResourceNotFoundException, ResourceInvalidException {
        HttpResourceAcquirer acquirer = new HttpResourceAcquirer();
        acquirer.acquireResource("http://a1.itc.cn/sceapp/focus_static/wap/js/jquery2.0.3.min.js", "D:\\workspace1\\test\\target\\fis\\temp");
    }

}
