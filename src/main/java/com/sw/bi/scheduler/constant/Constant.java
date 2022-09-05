package com.sw.bi.scheduler.constant;

import java.io.File;

/**
 * datax目录常量 
 * @author panhong 2018-12-13
 *
 */
public class Constant {
	// datax安装目录
    public static final String DATAX_HOME = "/home/tools/datax/";
    // datax作业文件保存目录
    //public static final String DATAX_JSON_HOME = "E:/job/";

    public static final String DATAX_PLUGIN_HOME = DATAX_HOME + File.separator + "hjplugin";
    public static final String DATAX_READER_HOME = DATAX_PLUGIN_HOME + File.separator + "reader";
    public static final String DATAX_WRITER_HOME = DATAX_PLUGIN_HOME + File.separator + "writer";

}
