package com.sw.bi.scheduler.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 文件操作相关工具类
 * @author panhong 2018-12-13
 *
 */
public class FileUtil {

    public static void write(File file, String str) {
        try {
            FileWriter writer = new FileWriter(file);
            System.out.println("需要写回去的文件 "+file);
            System.out.println("需要写回去的字符串 "+str);
            writer.write(str);
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        //return Constant.success;
    }

    public static String read(File file) {
        if (!file.exists()) {
            System.out.println("文件不存在！" + file.getPath());
            return null;
        }
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (IOException e) {
        	System.out.println(e.getMessage());
        }
        try {
            return new String(filecontent, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        	System.out.println(e.getMessage());
            return null;
        }
    }
}