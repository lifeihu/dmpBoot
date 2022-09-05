package com.sw.bi.scheduler.util;

import java.io.IOException;

public class LinuxBash {
	
	public static void main(String[] args){
		
		String tempPath = "/home/tools/temp/newdataxtemp/20190109/4035.sh";
		String[] cmd = { "/bin/sh","-c",tempPath};
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			System.out.println("process.exitValue()......" + process.exitValue());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
