package com.sw.bi.scheduler.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlagCreate {

	public static void NewFlag(List<String> list,String fileDir,Long ActionId,String jsonFile) throws IOException {

		File fileD = new File(fileDir);
		if (!fileD.exists()) {
			fileD.mkdirs();
		}
		
		
		//写入flag文件
		String tempPath = fileDir + ActionId + ".flag";
		File tempFile = new File(tempPath);
		if (!tempFile.exists()) {
			tempFile.createNewFile();
		}
		
		File outFile = new File(tempPath);
		try {
			FileWriter fileWriter = new FileWriter(outFile, false);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			for (String a : list) {
				bufferedWriter.write(a.substring(a.indexOf(":") + 1).trim()+"\n");
				System.out.println(a.substring(a.indexOf(":") + 1).trim());
			}
			bufferedWriter.write(jsonFile +"\n");
			bufferedWriter.close();
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ReadFile read = new ReadFile();
		//FlagCreate.NewFlag(read.readLastNLine(new File("e:/3887.log"), 8L));
	}

}
