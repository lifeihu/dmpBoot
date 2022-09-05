package com.sw.bi.scheduler.util;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;

public class TestSub {
	
    private static URI getIP(URI uri) {
        URI effectiveURI = null;

        try {
            // URI(String scheme, String userInfo, String host, int port, String
            // path, String query,String fragment)
            effectiveURI = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
        } catch (Throwable var4) {
            effectiveURI = null;
        }

        return effectiveURI;
    }
	public static void main(String[] args) {
		String hdfspath ="hdfs://10.2.30.91:8020/group/user/tools/meta/hive-temp-table/tools.db/maliaoDMP/123.csv";
		//System.out.println(getIP(URI.create(hdfspath)));
		Long JobType =2030L;
		System.out.println(JobType.toString().substring(0, 2));
		System.out.println(JobType.toString().substring(2, 4));
		
		int m = StringUtils.ordinalIndexOf(hdfspath, "/",3);
		System.out.println(hdfspath.substring(0,m));
		System.out.println(hdfspath.substring(m));
		System.out.println(hdfspath.substring(hdfspath.lastIndexOf("/")+1));
		//----------------------
		String path = "C:\\abs\\afsaf\\sda.txt";
		System.out.println(path.substring(0,path.lastIndexOf("\\")+1));
		
		String file ="/group/user/tools/meta/hive-temp-table/tools.db/maliaoDMP/123.csv";
		System.out.println(file.substring(0,file.lastIndexOf("/")+1));
		
		String aa="db.collection";
		System.out.println(aa.substring(0,StringUtils.ordinalIndexOf(aa, ".",1)));
		System.out.println(aa.substring(StringUtils.ordinalIndexOf(aa, ".",1)+1));
		
		/**
		* 方法一：
		*/
		/*
		for(int i=1;i<=3;i++)
		String userNameUrl;
		int beginIndex = 0;
		int endIndex = 0;
		userNameUrl = "454512@hongri@4944115455d9591b274648a06303d910de";
		beginIndex = userNameUrl.indexOf("@")+1;
		endIndex = userNameUrl.lastIndexOf("@");
		System.out.println(userNameUrl.substring(beginIndex,userNameUrl.length()));*/


	}

}
