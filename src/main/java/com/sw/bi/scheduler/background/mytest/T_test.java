package com.sw.bi.scheduler.background.mytest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.sw.bi.scheduler.util.DateUtil;

//  /home/tools/scheduler/scheduler jar   /root/t_test.jar com.sw.bi.scheduler.background.mytest.T_test
public class T_test {

	 

	public static void main(String[] args) throws Exception {
		T_test t =  new T_test();
		 System.out.println("1111111111111111111111111111");
		//t.programeRun("expect -c 'spawn su  - tools-xinhaoyi;expect \"Password: \";send \"tools-xinhaoyi\n\";expect \"*$*\";send \"mkdir /home/tools-xinhaoyi/test-expect/\";expect eof'");
		//t.programeRun2("/home/tools/a.sh");
		//Process process = Runtime.getRuntime().exec("expect -c 'spawn su  - tools-xinhaoyi;expect \"Password: \";send \"tools-xinhaoyi\n\";expect \"*$*\";send \"mkdir /home/tools-xinhaoyi/test-expect/\";expect eof'");
		Process process = Runtime.getRuntime().exec("/bin/su  -c '/bin/mkdir /home/tools/gh_test88' - tools");
		BufferedReader ireader = null;
		BufferedReader ereader = null;
		String line = null;
		ireader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		ereader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		while ((line = ireader.readLine()) != null) {
			System.out.println(line);
		}

		while ((line = ereader.readLine()) != null) {
			System.out.println("ERROR: " + line);
		}
		System.out.println("22222222222222222222222222222222222");
	}
	
//	expect -c 'spawn su    - tools-xinhaoyi;expect "Password: ";send "tools-xinhaoyi\n";expect "*$*";send "mkdir /home/tools-xinhaoyi/test-expect/\n";interact'
	
	
	
	
	private Process programeRun2(String path) throws IOException {
		// Map<String, String> paramsMapping = Parameters.getRunTimeParamter(currentTask);

		//java调用shell脚本并且传入了两个参数. $1表示yyyyMMdd  $2表示yyyyMMddHH  $3表示yyyyMM  $4表示settingTime(yyyy-MM-dd HH:mm:ss)  $5表示作业ID
		String[] commands = new String[] { "/bin/bash", path};
		Process process = Runtime.getRuntime().exec(commands);

		return process;
	}
	
	private Process programeRun(String cmd) throws Exception {
		String[] commands = new String[] { "/bin/bash", "-c", cmd};
		Process process = Runtime.getRuntime().exec(commands);
		BufferedReader ireader = null;
		BufferedReader ereader = null;
		String line = null;
		ireader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		ereader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		while ((line = ireader.readLine()) != null) {
			System.out.println(line);
		}

		while ((line = ereader.readLine()) != null) {
			System.out.println("ERROR: " + line);
		}
		
		
		return process;
	}
}