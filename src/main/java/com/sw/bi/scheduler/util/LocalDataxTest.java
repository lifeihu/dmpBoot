package com.sw.bi.scheduler.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LocalDataxTest {
	public static void main(String[] args) {
		try {
			System.out.println("start");
			String[] arguments = new String[]  {"python", "D:/DataX-master/bin/datax.py",  "E:/job/mysql2mysql.json"};
			System.out.println(arguments);
			Process pr = Runtime.getRuntime().exec(arguments);
			BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println("这里的日志..."+line);
			}
			in.close();
			pr.waitFor();
			System.out.println("end");
		} catch (Exception e) {
			e.printStackTrace();
		}
		String abc="username,email";
		System.out.println(abc.replace(",", "\",\""));
		System.out.println("abc............"+abc);
		
		
	}

}
