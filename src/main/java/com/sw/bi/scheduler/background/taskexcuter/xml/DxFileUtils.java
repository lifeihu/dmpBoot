package com.sw.bi.scheduler.background.taskexcuter.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;

public class DxFileUtils {

	/**
	 * List all files(非递归方式),递归方式会较慢,且不需要
	 * 
	 * @param filepath 文件路径
	 * @return list 文件list
	 * @author qyx
	 */
	public static List<File> listAllFile(String filePath) {
		ArrayList<File> list = new ArrayList<>();
		File file = new File(filePath);
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			File eachFile = files[i];
			list.add(eachFile);
		}
		return list;
	}

	/**
	 * Load the *.properties file
	 * 
	 * @param configFileName
	 * @return
	 * @throws IOException
	 */
	public static Properties loadConfigureFile(String configFileName) throws IOException {
		Properties props = new Properties();
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(configFileName);
			props.load(inputStream);
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
		return props;
	}

	/**
	 * <pre>
	 * /temp/path/abc.txt.gz return /temp/path/
	 * /temp/path/abc.txt    return /temp/path/ 
	 * /temp/path/abc        return /temp/path/
	 * /temp/path/abc/       return /temp/path/abc/
	 * abc.txt               return ""
	 * ""                    return ""
	 * </pre>
	 * 
	 * @param filepath
	 * @return
	 */
	public static String extractFilePath(String filepath) {
		if (filepath == null)
			return null;
		if (filepath.contains("\\"))
			filepath = filepath.replaceAll("\\\\", "/");
		String fp = filepath.substring(0, filepath.lastIndexOf("/") + 1);
		return fp;
	}

	/**
	 * <pre>
	 * /temp/path/abc.txt.gz return abc.txt.gz
	 * /temp/path/abc.txt    return abc.txt 
	 * /temp/path/abc        return abc
	 * /temp/path/abc/       return ""
	 * abc.txt               return abc.txt
	 * ""                    return ""
	 * </pre>
	 * 
	 * @param filepath
	 * @return
	 */
	public static String extractFileName(String filepath) {
		if (filepath == null)
			return null;
		if (filepath.contains("\\"))
			filepath = filepath.replaceAll("\\\\", "/");
		String fileFullName = filepath.substring(filepath.lastIndexOf("/") + 1);
		return fileFullName;
	}

	/**
	 * <pre>
	 * /temp/path/abc.txt.gz return abc.txt
	 * /temp/path/abc.txt    return abc
	 * /temp/path/abc        return abc
	 * /temp/path/abc/       return ""
	 * abc.txt               return abc
	 * ""                    return ""
	 * </pre>
	 * 
	 * @param filepath
	 * @return
	 */
	public static String extractFileNameWithoutExtension(String filepath) {
		if (filepath == null)
			return null;
		if (filepath.contains("\\"))
			filepath = filepath.replaceAll("\\\\", "/");
		String fileFullName = filepath.substring(filepath.lastIndexOf("/") + 1);
		String fileName = "";
		if (fileFullName.lastIndexOf(".") != -1) {
			fileName = fileFullName.substring(0, fileFullName.lastIndexOf("."));
		} else
			fileName = fileFullName;
		return fileName;
	}

	/**
	 * <pre>
	 * /temp/path/abc.txt.gz return .gz
	 * /temp/path/abc.txt    return .txt
	 * /temp/path/abc        return ""
	 * /temp/path/abc/       return ""
	 * abc.txt               return .txt
	 * ""                    return ""
	 * </pre>
	 * 
	 * @param filepath
	 * @return
	 */
	public static String extractFileExtension(String filepath) {
		if (filepath == null)
			return null;
		if (filepath.contains("\\"))
			filepath = filepath.replaceAll("\\\\", "/");
		String fileFullName = filepath.substring(filepath.lastIndexOf("/") + 1);
		String fileExtension = "";
		if (fileFullName.lastIndexOf(".") != -1) {
			fileExtension = fileFullName.substring(fileFullName.lastIndexOf("."));
		}
		return fileExtension;
	}

	/**
	 * create dirs
	 * 
	 * @param path
	 * @return
	 */
	public static boolean createDirs(String path) {
		File file = new File(path);
		return file.mkdirs();
	}

	/**
	 * recursive delete
	 * 
	 * @param path
	 */
	public static void delete(File file) {
		if (file.isDirectory()) {
			File[] fs = file.listFiles();
			for (int i = 0; i < fs.length; i++) {
				delete(fs[i]);
			}
			file.delete();
		} else {
			file.delete();
		}
	}

	/**
	 * recursive delete delete dirs (or a single file)
	 * 
	 * @param path
	 */
	public static void delete(String path) {
		File file = new File(path);
		delete(file);
	}

	/**
	 * Create a zip file.
	 * 
	 * @param sourceFile
	 * @param destinationFile
	 * @throws IOException
	 */
	public static void zip(String sourceFile, String destinationFile) throws IOException {
		File inputFile = new File(sourceFile);
		ZipOutputStream out;
		out = new ZipOutputStream(new FileOutputStream(destinationFile));
		DxFileUtils.zip(out, inputFile, inputFile.getName());
		out.close();
	}

	/**
	 * Create a zip file.
	 * 
	 * @param out
	 * @param f
	 * @param base
	 * @throws IOException
	 */
	private static void zip(ZipOutputStream out, File f, String base) throws IOException {
		if (f.isDirectory()) {
			File[] fileArray = f.listFiles();
			base += "/";
			// Create directory in zipfile
			out.putNextEntry(new ZipEntry(base));
			for (int i = 0; i < fileArray.length; i++) {
				// recursive invoke this method for each file
				zip(out, fileArray[i], base + fileArray[i].getName());
			}
		} else {
			out.putNextEntry(new ZipEntry(base));
			InputStream is = new FileInputStream(f);
			byte[] byteBuffer = new byte[1048576]; // 1MB buffer size
			int length = 0;
			while ((length = is.read(byteBuffer)) != -1) {
				out.write(byteBuffer, 0, length);
			}
			is.close();
		}
	}

	/**
	 * Load a whole file into memory as a single string.
	 * 
	 * @param filePath
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static String file2String(String filePath, String encoding) throws IOException {
		File file = new File(filePath);
		if (file.isDirectory() || !file.exists())
			return null;

		BufferedReader reader = null;
		StringBuffer sb = new StringBuffer();
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
			String line = "";
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n"); // 为了配合XmlCreator.java中的/root/logs/2012-02-26/1099600.log 这里要特别加\n
			}
		} finally {
			reader.close();
		}
		return sb.toString();
	}

	/**
	 * save a String to a single text file. note: this may overwrite the corrent
	 * file <br>
	 * To create a new line, try to use: System.getProperty("line.separator"), not
	 * "\n"
	 * 
	 * @param srcStr
	 * @param filePath
	 * @param encoding
	 * @throws IOException
	 */
	public static void string2File(String srcStr, String filePath, String encoding) throws IOException {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new StringReader(srcStr));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), encoding));
			char buf[] = new char[4096];
			int len;
			while ((len = reader.read(buf)) != -1) {
				writer.write(buf, 0, len);
			}
		} finally {
			try {
				reader.close();
			} finally {
				writer.close();
			}
		}
	}

	/**
	 * 将属性文件中的key,value放入map中<br>
	 * Parse properties file to a map <br>
	 * 
	 * The properties file content should be like this: <br>
	 * 
	 * #{filepath}=/home/datax/${yyyyMMdd,-1d} <br>
	 * year=${yyyy} <br>
	 * dir=/home/data/dir/ <br>
	 * 
	 * @param filePath
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> file2Map(String filePath, String encoding) throws IOException {
		if (StringUtils.isBlank(filePath))
			return null;

		File file = new File(filePath);
		if (file.isDirectory() || !file.exists())
			return null;

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
		String line = "";
		Map<String, String> m = new HashMap<String, String>();
		while ((line = reader.readLine()) != null) {
			if (line.length() == 0 || !StringUtils.contains(line, "="))
				continue;
			int equalIndex = StringUtils.indexOf(line, "=");
			String key = StringUtils.substring(line, 0, equalIndex);
			String value = StringUtils.substring(line, equalIndex + 1);
			m.put(key, value);
		}
		return m;
	}

	public static void main(String args[]) {

		System.out.println(DxFileUtils.extractFileNameWithoutExtension("jobs/file2mysql.xml"));
	}
}
