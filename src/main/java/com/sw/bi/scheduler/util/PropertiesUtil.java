package com.sw.bi.scheduler.util;

import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * propertity读取辅助
 * Created by whl on 2015/6/18.
 */
public class PropertiesUtil {

    private static Properties prop = new Properties();

    static {
        try {
            prop.load(PropertiesUtil.class.getClassLoader().getResourceAsStream("database-scheduler.properties"));
            prop.load(PropertiesUtil.class.getClassLoader().getResourceAsStream("scheduler.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return (String) prop.get(key);
    }

    public static void main(String[] args) {
        System.out.println(PropertiesUtil.getProperty("sender.sms.signature"));
    }
}
