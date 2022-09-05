package com.sw.bi.scheduler.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sw.bi.scheduler.background.taskexcuter.Parameters;
import com.sw.bi.scheduler.background.taskexcuter.Parameters.DBType;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.constant.Constant;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.util.Configure.SourceDataTypes;
import com.sw.bi.scheduler.util.Configure.TargetDataTypes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理job.json通用实体类
 * 
 * @author zhurongji 2018-12-13
 *
 */
@Service
public class JobJsonUtil {

	public void createByTask(JobDatasyncConfig config, Long ActionId) {
		pluginTemplate(config, ActionId);
		System.out.println("actionId......." + ActionId);

	}

	/**
	 * 封装替换的方式
	 * 
	 * @param jobDatasyncConfig
	 * @throws FileNotFoundException
	 */
	public void pluginTemplate(JobDatasyncConfig jobDatasyncConfig, Long Current_ActionId) {
		String readerName = "";
		String writerName = "";
		if (jobDatasyncConfig == null) {
			System.out.println("出错了 jobDatasyncConfig为空  请联系研发团队!!!");
			throw new RuntimeException("出错了 jobDatasyncConfig为空  请联系研发团队!!!");
		}
		// jobType拆分
		Long sourceDataTypes = Long.valueOf((jobDatasyncConfig.getJobType().toString()).substring(0, 2));
		System.out.println("jobType" + jobDatasyncConfig.getJobType().toString());
		Long targetDataTypes = Long.valueOf((jobDatasyncConfig.getJobType().toString()).substring(2, 4));
		System.out.println("sourceDataTypes :" + sourceDataTypes);
		System.out.println("targetDataTypes :" + targetDataTypes);

		if (sourceDataTypes == null || targetDataTypes == null) {
			throw new RuntimeException("出错了  来源data类型和目标data类型不能为空!!!");
		}

		readerName = SourceDataTypes.valueOf(sourceDataTypes.intValue()).getValue();
		writerName = TargetDataTypes.valueOf(targetDataTypes.intValue()).getValue();
		System.out.println("readerName :" + readerName);
		System.out.println("writerName :" + writerName);

		// 加载reader writer模板json
		File readerFile = new File(
				Constant.DATAX_READER_HOME + File.separator + readerName + File.separator + "plugin_job_template.json");
		if (!readerFile.exists()) {
			System.out.println("出错了 readerFile不存在");
			throw new RuntimeException("出错了 readerFile不存在");
			// return;
		}

		File writerFile = new File(
				Constant.DATAX_WRITER_HOME + File.separator + writerName + File.separator + "plugin_job_template.json");
		if (!writerFile.exists()) {
			System.out.println("出错了 writerFile不存在");
			throw new RuntimeException("出错了 writerFile不存在");
			// return;
		}
		System.out.println("动态map组装前");
		String resultJson = createByReaderWriter(jobDatasyncConfig, readerFile, writerFile);
		// mashifeng 2018.1.2 打印JSON文件的，调试时才用。正式使用时屏蔽这句，避免将JSON文件内容记录到日志中
		System.out.println("动态map组装后");
		if (resultJson.length() > 0) {
			String fileDir = Parameters.tempNewDataxPath + new SimpleDateFormat("yyyyMMdd").format(new Date());
			// String fileDir = Parameters.tempNewDataxPath + new
			// SimpleDateFormat("yyyyMMdd").format(new Date());
			System.out.println("替换后的Json作业模板路径" + fileDir);
			String tempPath = fileDir + "/" + Current_ActionId + ".json";
			System.out.println("替换后的Json作业模板路径+json模板" + tempPath);
			File fileD = new File(fileDir);
			if (!fileD.exists()) {
				fileD.mkdirs();
			}
			// 文件写回去
			File jobFile = new File(tempPath);
			FileUtil.write(jobFile, resultJson);
			// 赋值给UserXml
			/// home/tools/temp/newdataxtemp/20190225/6950.json
			System.out.println("替换后的Json作业模板路径+json模板 " + tempPath + " just do it");
			jobDatasyncConfig.setUserXml(tempPath);
		} else {
			try {
				throw new Exception("JsonError");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		// String jobName = StringUtils.substringBefore(readerName, "reader") +
		// "2" + StringUtils.substringBefore(writerName, "writer"); ;
		// File jobFile = new File(Constant.DATAX_JSON_HOME, jobName + ".json");
		// // 文件写回去
		// FileUtil.write(jobFile, resultJson);
		// jobDatasyncConfig.setUserXml(Constant.DATAX_JSON_HOME+jobName +
		// ".json");
		JobDatasyncConfigService jobDatasyncConfigService = BeanFactory.getService(JobDatasyncConfigService.class);
		jobDatasyncConfigService.saveOrUpdate(jobDatasyncConfig);
	}

	private String createByReaderWriter(JobDatasyncConfig jobDatasyncConfig, File readerFile, File writerFile) {

		JSONObject results;
		StringBuffer templateJson = new StringBuffer();
		// 封装通用Map 组装动态参数到一个map对象中,接下来用于动态参数的替换
		System.out.println("动态传入参数之前");
		Map<String, Map<String, String>> map = getConfigMap(jobDatasyncConfig);
		System.out.println("动态传入参数之后");
		System.out.println("组装动态参数map：" + map);
		if (map == null) {
			System.out.println("动态传入参数异常");
			throw new RuntimeException("动态传入参数异常 [组装动态参数map为null]");
		}
		// 获取reader writer模板
		
		try {
			String readerJson = getTemplate(new FileReader(readerFile));
			String writerJson = getTemplate(new FileReader(writerFile));
			System.out.println("====替换之前readerJson：====" + readerJson);
			// 模板参数替换
			readerJson = this.paramReplace(readerJson, map.get("source"), map.get("global"), "source");
			writerJson = this.paramReplace(writerJson, map.get("target"), map.get("global"), "target");
			System.out.println("====替换之后readerJson：====" + readerJson);

			templateJson.append("{\"job\":{");
			templateJson.append("\"setting\":{\"speed\":{\"channel\":${threadNumber}},"
					+ "\"errorLimit\":{\"record\":${errorLimitrecords},\"percentage\":${errorthreshold}}},");
			templateJson.append("\"content\":[{\"reader\":");
			// 拼接reader
			templateJson.append(readerJson);

			templateJson.append(",\"writer\":");
			// 拼接writer
			templateJson.append(writerJson);

			templateJson.append("}]}}");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String resultJson = templateJson.toString();
		resultJson = this.paramReplace(resultJson, map.get("global"), null, "");
		results = JSON.parseObject(resultJson);
		JSONArray content = ((JSONObject) results.get("job")).getJSONArray("content");
		for (int i = 0; i < content.size(); i++) {
			JSONObject readOrWrite = content.getJSONObject(i);
			// read
			if (jobDatasyncConfig.getSourceCustomerParameter() != null
					&& jobDatasyncConfig.getSourceCustomerParameter().length() != 0
					&& jobDatasyncConfig.getSourceCustomerParameter().replace(" ", "") != "{}") {
				if (isJSONValid(jobDatasyncConfig.getSourceCustomerParameter()) == true) {
					System.out.println(jobDatasyncConfig.getSourceCustomerParameter());
					JSONObject readObject = (JSONObject) readOrWrite.get("reader");
					JSONObject readParameter = (JSONObject) readObject.get("parameter");
					readParameter.putAll(JSON.parseObject(jobDatasyncConfig.getSourceCustomerParameter()));
				} else {
					throw new Warning("该参数不符合json格式!");
				}
			}
			// writer
			if (jobDatasyncConfig.getTargetCustomerParameter() != null
					&& jobDatasyncConfig.getTargetCustomerParameter().length() != 0
					&& jobDatasyncConfig.getTargetCustomerParameter().replace(" ", "") != "{}") {
				if (isJSONValid(jobDatasyncConfig.getTargetCustomerParameter()) == true) {
					System.out.println(jobDatasyncConfig.getSourceCustomerParameter());
					JSONObject writeObject = (JSONObject) readOrWrite.get("writer");
					JSONObject writeParameter = (JSONObject) writeObject.get("parameter");
					writeParameter.putAll(JSON.parseObject(jobDatasyncConfig.getTargetCustomerParameter()));
				} else {
					throw new Warning("该参数不符合json格式!");
				}
			}
		}
		return results.toString();
	}

	/**
	 * 根据数据库配置值获取每个部分的配置属性
	 * 
	 * @param config
	 * @return
	 */
	public Map<String, Map<String, String>> getConfigMap(JobDatasyncConfig config) {
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> globalMap = new HashMap<String, String>();
		if (config == null) {
			throw new RuntimeException("出错了 jobDatasyncConfig为空  请联系研发团队!!!");
		}
		Long sourceDataTypes = Long.valueOf((config.getJobType().toString()).substring(0, 2));
		Long targetDataTypes = Long.valueOf((config.getJobType().toString()).substring(2, 4));
		if (sourceDataTypes == null || targetDataTypes == null) {
			throw new RuntimeException("出错了  来源data类型和目标data类型不能为空!!!");
		}
		String readerName = SourceDataTypes.valueOf(sourceDataTypes.intValue()).getValue();
		String writerName = TargetDataTypes.valueOf(targetDataTypes.intValue()).getValue();
		Field[] fields = JobDatasyncConfig.class.getDeclaredFields();
		Field.setAccessible(fields, true);
		for (Field field : fields) {
			if (field.getName().endsWith("DatasourceId")) {
				try {
					Object value = field.get(config);
					if (value != null) {
						Datasource datasource = (Datasource) value;
						Map<String, String> dataSourceMap = this.getDataSourceMap(datasource.getDatasourceId());
						String key = field.getName().replace("DatasourceId", "").replace("datasourceBy", "");
						key = key.substring(0, 1).toLowerCase() + key.substring(1);
						map.put(key, dataSourceMap);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				// 拆分HDFS文件目录(读)
			} else if (field.getName().equals("sourceDatapath") && readerName == "hdfsreader") {
				try {
					Object value = field.get(config);
					if (value != null) {
						// 例:hdfs://10.2.30.91:8020/group/user/tools/meta/hive-temp-table/tools.db/maliaoDMP/123.csv;
						// HDFS defaultFS
						String defaut = value.toString().substring(0,
								StringUtils.ordinalIndexOf(value.toString(), "/", 3));
						// HDFS path
						String path = value.toString().substring(StringUtils.ordinalIndexOf(value.toString(), "/", 3));
						globalMap.put("${" + field.getName() + ".defaultFS}", defaut);
						globalMap.put("${" + field.getName() + ".hdfsPath}", path);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				// 拆分HDFS文件目录(写)
			} else if (field.getName().equals("targetDatapath") && writerName == "hdfswriter") {
				try {
					Object value = field.get(config);
					if (value != null) {
						// 例:hdfs://10.2.30.91:8020/group/user/tools/meta/hive-temp-table/tools.db/maliaoDMP/123.csv;
						// HDFS defaultFS
						String defaut = value.toString().substring(0,
								StringUtils.ordinalIndexOf(value.toString(), "/", 3));
						// HDFS path
						String path = value.toString().substring(StringUtils.ordinalIndexOf(value.toString(), "/", 3));
						String finallypath = path.substring(0, path.lastIndexOf("/") + 1);
						// HDFS fileName
						String fileName = value.toString().substring(value.toString().lastIndexOf("/") + 1);
						globalMap.put("${" + field.getName() + ".defaultFS}", defaut);
						globalMap.put("${" + field.getName() + ".hdfsPath}", finallypath);
						globalMap.put("${" + field.getName() + ".hdfsFileName}", fileName);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				// 拆分FTP文件目录(写)
				// 拆分FILE文件所在目录(写)
				// 拆分CSV文件所在目录(写)
			} else if ((field.getName().equals("targetDatapath") && writerName == "ftpwriter")
					|| (field.getName().equals("targetDatapath") && writerName == "filewriter")
					|| (field.getName().equals("targetDatapath") && writerName == "csvwriter")) {
				try {
					Object value = field.get(config);
					if (value != null) {
						// 例:/group/user/tools/meta/hive-temp-table/tools.db/maliaoDMP/123.csv;
						// FilePath
						String filePath = value.toString().substring(0, value.toString().lastIndexOf("/") + 1);
						// FileName
						String fileName = value.toString().substring(value.toString().lastIndexOf("/") + 1);
						globalMap.put("${" + field.getName() + ".filePath}", filePath);
						globalMap.put("${" + field.getName() + ".fileName}", fileName);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}

				// 拆分DB和DB集合(读)
			} else if (field.getName().equals("sourceTableName") && readerName == "mongodbreader") {
				try {
					Object value = field.get(config);
					if (value != null) {
						// 例:db.collection;
						// db
						String db = value.toString().substring(0, StringUtils.ordinalIndexOf(value.toString(), ".", 1));
						// collection
						String collection = value.toString()
								.substring(StringUtils.ordinalIndexOf(value.toString(), ".", 1) + 1);
						globalMap.put("${" + field.getName() + ".db}", db);
						globalMap.put("${" + field.getName() + ".collection}", collection);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				// 拆分DB和DB集合(写)
			} else if (field.getName().equals("targetTableName") && writerName == "mongodbwriter") {
				try {
					Object value = field.get(config);
					if (value != null) {
						// 例:db.collection;
						// db
						String db = value.toString().substring(0, StringUtils.ordinalIndexOf(value.toString(), ".", 1));
						// collection
						String collection = value.toString()
								.substring(StringUtils.ordinalIndexOf(value.toString(), ".", 1) + 1);
						globalMap.put("${" + field.getName() + ".db}", db);
						globalMap.put("${" + field.getName() + ".collection}", collection);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}

			} else {
				// System.out.println("global map的属性: "+field.getName());
				try {
					Object value = field.get(config);
					if (value != null) {
						globalMap.put("${" + field.getName() + "}", value.toString());
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}

		}
		System.out.println("globalMap 的所有内容： " + globalMap.toString());
		map.put("global", globalMap);
		System.out.println("map 的所有内容： " + map.toString());
		return map;
	}

	/**
	 * 获得一个数据源的配置参数字典
	 * 
	 * @param 数据源对象
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	private Map<String, String> getDataSourceMap(long datasourceid)
			throws IllegalArgumentException, IllegalAccessException {
		// 参数改为传入datasourceid,然后从数据库重新查询一次. 如果直接传入ds的话,后面的 value =
		// field.get(ds); 取不到值.
		DatasourceService datasourceService = BeanFactory.getService(DatasourceService.class);
		Datasource ds = datasourceService.get(datasourceid);
		System.out.println("ds:" + ds);

		// 增加密码的加密
		try {
			String password = DxDESCipher.DecryptDES(ds.getPassword(), ds.getUsername());
			// ds.setPassword(DxDESCipher.EncryptDES(password));
			ds.setPassword(password);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		Map<String, String> map = new HashMap<String, String>();
		if (ds != null) {
			Field[] fields = Datasource.class.getDeclaredFields();
			Field.setAccessible(fields, true);
			for (Field field : fields) {

				Object value = null;

				// System.out.println(field.getName()+"======"+field.get(ds));

				try {
					value = field.get(ds);
					if (value != null && value.toString().length() > 0) {
						map.put("${" + field.getName() + "}", value.toString());
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			map.put("${driver}", DBType.valueOf(ds.getType().intValue()).getDriver());
		}
		return map;
	}

	/**
	 * 参数替换
	 * 
	 * @param s
	 * @param map
	 * @param gmap
	 * @param part
	 * @return
	 */
	private String paramReplace(String s, Map<String, String> map, Map<String, String> gmap, String part) {
		if (map != null) {
			for (String key : map.keySet()) {
				s = s.replace(key, map.get(key));
			}
		}
		if (gmap != null) {
			for (String key : gmap.keySet()) {
				if (key.startsWith("${" + part)) {
					String inKey = key.replace(part, "");
					inKey = "${" + inKey.substring(2, 3).toLowerCase() + inKey.substring(3);
					s = s.replace(inKey, gmap.get(key));
				}
			}
		}
		return s;
	}

	/**
	 * 获取模板
	 * 
	 * @param reader
	 * @return
	 */
	private String getTemplate(FileReader reader) {
		String template = null;
		if (template == null) {
			StringBuffer stb = new StringBuffer();
			try {
				readToBuffer(stb, reader);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			template = stb.toString();
			System.out.println(template);
		}

		return template;
	}

	private void readToBuffer(StringBuffer buffer, FileReader reader) throws IOException {
		BufferedReader breader = new BufferedReader(reader);
		String line;
		line = breader.readLine();
		while (line != null) {
			buffer.append(line);
			buffer.append("\n");
			line = breader.readLine();
		}
		reader.close();
	}

	/**
	 * json string 转换为 map 对象
	 * 
	 * @param jsonObj
	 * @return
	 */
	public static Map<String, Object> jsonToMap(String jsonObj) {
		// JSONObject jsonObject = JSONObject.fromObject(jsonObj);
		Map map = JSON.parseObject(jsonObj);
		return map;
	}

	/**
	 * 暴力解析:Alibaba fastjson
	 * 
	 * @param test
	 * @return
	 */
	public final static boolean isJSONValid(String test) {
		JSONObject.parseObject(test);
		return true;
	}

}
