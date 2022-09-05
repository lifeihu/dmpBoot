package com.sw.bi.scheduler.util;

import org.apache.commons.lang3.StringUtils;

public class TestContain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String sourceparameter ="\"fieldDelimiter\":\"\t\",\"encoding\":\"gbk\"";
		String[] sources = sourceparameter.split(",");
		for(String source:sources){
			//System.out.println(source.toString().split(":"));
			//String[] sou = source.toString().split(":");
			System.out.println(source.substring(0,StringUtils.ordinalIndexOf(source, ":",1)));

		}
		
	}

}
