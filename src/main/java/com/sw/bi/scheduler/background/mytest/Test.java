package com.sw.bi.scheduler.background.mytest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

//  /home/tools/scheduler/scheduler jar   /home/tools/t_test.jar com.sw.bi.scheduler.background.mytest.Test
public class Test {

	 

	public static void main(String[] args) throws Exception {
		Test t =  new Test();
		//t.programeRun("expect -c 'spawn su - tools-xinhaoyi;expect \\"Password: \\";send \\"tools-xinhaoyi\n\\";interact'");
		t.programeRun("expect -c 'spawn su - tools-xinhaoyi;expect \"Password: \";send \"tools-xinhaoyi\n\";interact'");
	}
	
//	expect -c 'spawn su - tools-xinhaoyi;expect "Password: ";send "tools-xinhaoyi\n";interact'
	
	
	private Process programeRun(String cmd) throws Exception {
		String[] commands = new String[] { "/bin/bash", "-c", cmd};
		Process process = Runtime.getRuntime().exec(commands);
		return process;
	}
}
