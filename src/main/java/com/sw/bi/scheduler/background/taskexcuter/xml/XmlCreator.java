package com.sw.bi.scheduler.background.taskexcuter.xml;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.sw.bi.scheduler.background.taskexcuter.Parameters;
import com.sw.bi.scheduler.background.taskexcuter.Parameters.DBType;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.Configure.JobType;

public class XmlCreator {

	/**
	 * 数据库操作的协作者类
	 */
	private String coordinatorClass;

	public String createByTask(Task task) {
		Job job = BeanFactory.getService(JobService.class).get(task.getJobId());
		long job_id = job.getJobId();
		JobDatasyncConfigService jobDatasyncConfigService = BeanFactory.getService(JobDatasyncConfigService.class);
		JobDatasyncConfig syncConfig = jobDatasyncConfigService.getJobDatasyncConfigByJob(job_id);

		String XML = this.createXmlByConfig(syncConfig, task);
		if (this.errorThreshold == null || this.errorThreshold.length() < 1) {
			return "";
		}
		return XML;
	}

	public String createXmlByConfig(JobDatasyncConfig syncConfig, Task task) {
		this.initRunTimeParams(task);
		if (syncConfig.getUserXml() != null && syncConfig.getUserXml().length() > 0) {
			//自定义完整XML模版
			String user_xml = syncConfig.getUserXml();
			System.out.println("自定义XML模板的地址是: " + user_xml);
			try {
				return createByUserXML(user_xml, syncConfig);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "";
		} else {
			//根据源和目标组成的标准类型模版
			SourceTargetPair<DBType> stp;
			stp = this.getDataxSourceTarget(syncConfig.getJobType());
			if (stp == null) {
				return "";
			}

			this.initCoordinatorClass(stp.target);

			return this.createBySourceTarget(syncConfig, stp);
		}
	}

	/**
	 * 初始化协作者类(因为目前的协作者类都只针对写接口的所以这里对target进行判断)
	 * 
	 * @param targetType
	 */
	private void initCoordinatorClass(DBType targetType) {
		switch (targetType) {
			case Greenplum:
				coordinatorClass = "com.shunwang.datasync.plugins.GreenplumPluginCoordinator";
				break;
			case Mysql:
			case Sqlserver:
			case Oracle:
				coordinatorClass = "com.shunwang.datasync.plugins.DBPluginCoordinator";
				break;
		}
	}

	/**
	 * 手动配置XML需要替换的字符 UserXML替换规则： 将${abc}替换为config中的config.abc属性
	 * 将个数据源模块的前缀加上属性，替换为config中的对应属性 如：${initDatapath}
	 * 替换为config中的initDatasource对象的 datapath属性
	 * 
	 * @param xmlPath
	 * @param syncConfig
	 * @return
	 * @throws IOException
	 */
	private String createByUserXML(String xmlPath, JobDatasyncConfig syncConfig) throws IOException {
		String XML = DxFileUtils.file2String(xmlPath, "UTF-8");
		//System.out.println("=========user-XML1: =========\n"+XML);
		Map<String, String> map = this.getConfigFullMap(syncConfig);
		for (String s : map.keySet()) {
			XML = XML.replace(s, map.get(s));
		}
		XML = runTimeParamReplace(XML);
		XML = this.filterUnused(XML);
		//System.out.println("=========user-XML2: =========\n"+XML);
		this.setErrorThreadFromXML(XML);
		return XML;
	}

	//  在自定义XML中,这部分要规范书写. 如errorThreshold="100" 是正确的.   errorThreshold = "100" 是错误的
	private void setErrorThreadFromXML(String XML) {
		String size = "errorThreshold=\"";
		int index = 0;
		if ((index = XML.indexOf(size)) > -1) {
			String s = XML.substring(index + size.length());
			if ((index = s.indexOf("\"")) > -1) {
				s = s.substring(0, index);
				this.errorThreshold = s;
			}
		}
	}

	/**
	 * 容错率属性为必需字段，如果没有该字段XML将整体返回为空字符串
	 */
	private String errorThreshold;

	public String getErrorThreshold() {
		return this.errorThreshold;
	}

	private Map<String, String> rumTimeParams;
	protected Task currentTask;
	private void initRunTimeParams(Task task) {
		currentTask=task;
		this.rumTimeParams = Parameters.getRunTimeParamter(task);
	}

	/**
	 * 根据数据源和目标得到标准的xml模版 规则 ${abc} 直接替换为 config.abc
	 * 如果${abc}在某个组成部分中(init,final,source,target) init中的 ${abc}将替换为
	 * config.initAbc 的值 config属性规则为数据库列名，如果列名中存在"_"则视为多单词组成 如，列名 column_abc 将变成
	 * config 对象的config.columnAbc属性
	 * 
	 * @param syncConfig
	 * @param stp
	 * @return
	 */
	private String createBySourceTarget(JobDatasyncConfig syncConfig, SourceTargetPair<DBType> stp) {

		if (stp == null) {
			return "";
		}
		this.errorThreshold = syncConfig.getErrorthreshold();
		//System.out.println("begin: 根据JobDatasyncConfig组装一个map对象");
		Map<String, Map<String, String>> map = this.getConfigMap(syncConfig); //组装动态参数到一个map对象中,接下来用于动态参数的替换
		//System.out.println("end: 根据JobDatasyncConfig组装一个map对象");

		String initializer = this.createInitializer();
		initializer = this.paramReplace(initializer, map.get("init"), map.get("global"), "init");//动态参数替换

		String finalizer = this.createFinalizer();
		finalizer = this.paramReplace(finalizer, map.get("finaly"), map.get("global"), "finaly");//动态参数替换

		// modify by zhuzhongji 2015年9月11日09:48:41
		/*String source = this.createSourcePart(stp.source);
		source = this.paramReplace(source, map.get("source"), map.get("global"), "source");//动态参数替换

		String target = this.createTargetPart(stp.target);
		target = this.paramReplace(target, map.get("target"), map.get("global"), "target");//动态参数替换*/
		String source = this.createSourcePart(stp.source);
		if(DBType.HBase.equals(stp.source)){
			source = this.paramReplace_new(source, map.get("source"), map.get("global"), "source");//动态参数替换
		}else {
			source = this.paramReplace(source, map.get("source"), map.get("global"), "source");//动态参数替换
		}

		String target = this.createTargetPart(stp.target);
		if(DBType.HBase.equals(stp.target)){
			target = this.paramReplace_new(target, map.get("target"), map.get("global"), "target");//动态参数替换
		} else {
			target = this.paramReplace(target, map.get("target"), map.get("global"), "target");//动态参数替换
		}
		
		
		StringBuilder template = new StringBuilder();
		template.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		template.append("<job readConcurrency=\"${threadNumber}\" writeConcurrency=\"${threadNumber}\" errorThreshold=\"${errorthreshold}\">\n");
		template.append(initializer).append("\n");
		template.append(source).append("\n");
		template.append(target).append("\n");
		template.append(finalizer).append("\n");
		if (!StringUtils.isBlank(coordinatorClass)) {
			template.append("<property key=\"plugin.coordinator\" value=\"").append(coordinatorClass).append("\" />\n");
		}
		template.append("</job>");

		/*String templateXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "\n" +
				"<job readConcurrency=\"${threadNumber}\" writeConcurrency=\"${threadNumber}\" errorThreshold=\"${errorthreshold}\">" + "\n" + initializer + "\n" + source + "\n" + target + "\n" +
				finalizer + "\n" + "<property key=\"plugin.coordinator\" value=\"com.shunwang.datasync.plugins.DBPluginCoordinator\" />" + "\n" + "</job>";*/

		String xml = template.toString();
		xml = this.paramReplace(xml, map.get("global"), null, "");
		//System.out.println("===XML1=== \n"+XML);
		xml = this.filterUnused(xml);
		//System.out.println("===XML2=== \n"+XML);
		return xml;
	}

	/**
	 * 过滤没有被替换的或者无用的<property>行 处理逻辑: 先将XML用\n分隔以后. 然后对每一行进行处理. 如果该行没有${, 则该行要的.
	 * 如果该行有${, 则进行参数的替换. 如果替换后,还存在${,则舍弃该行.
	 * 
	 * @param XML
	 * @return
	 */
	private String filterUnused(String XML) {
		XML = XML.replace("errorThreshold=\"${errorthreshold}\"", "");
		String[] arr = XML.split("\n");
		XML = "";
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].indexOf("${") == -1) {
				XML += arr[i] + "\n";
			} else {
				String line = this.runTimeParamReplace(arr[i]);
				if (line.indexOf("${") == -1) {
					XML += line + "\n";
				}
			}
		}
		return XML;
	}

	/**
	 * 替换需要程序运行时才能得到的参数
	 * modify by chenpp 20160614 添加 任意小时 天 的时间替换
	 * @param s
	 * @return
	 */

    private String runTimeParamReplace(String s) {
        for (String key : rumTimeParams.keySet()) {
            if (s.indexOf(key) > -1) {
                s = s.replace(key, rumTimeParams.get(key));
            }
        }
		Calendar calendar = DateUtil.cloneCalendar();
		calendar.setTime(this.currentTask != null ? this.currentTask.getSettingTime() : new Date());
        if(s.contains("month_now")){
            String regHour = "\\$\\{month_now[^\\$]*\\}";    
            Pattern p = Pattern.compile(regHour);
            Matcher m = p.matcher(s);
            while(m.find()){
                calendar.add(Calendar.MONTH, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
                s= s.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMM"));
            }
        }      
        if(s.contains("date_now")){
            String reg = "\\$\\{date_now[^\\$]*\\}";    
            Pattern p = Pattern.compile(reg);
            Matcher m = p.matcher(s);
            while(m.find()){
            	String[] config = m.group().split(",");
                config[config.length - 1] = config[config.length - 1].substring(0, config[config.length - 1].length() - 1);
            	calendar.add(Calendar.DATE, 0-Integer.parseInt(config[1]));
                if (config.length == 3) {
                	s= s.replace(m.group(), DateUtil.format(calendar.getTime(), config[2]));
                } else {
                	s= s.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMMdd"));
                }
            }
        }
        if(s.contains("hour_now")){
            String regHour = "\\$\\{hour_now[^\\$]*\\}";    
            Pattern p = Pattern.compile(regHour);
            Matcher m = p.matcher(s);
            while(m.find()){
                calendar.add(Calendar.HOUR, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
                s= s.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
            }
        }
        return s;
    }
    
	/**
	 * 模块替换
	 * 
	 * @param 需要替换的字符串
	 * @param 模块内用于替换的map
	 * @param 公共的用于替换的map
	 * @param 模块名
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
	 * 模块替换(用户HBase类型的替换)（其实这个方法可以和上面那个方法合并成一个方法）
	 * @author：zhuzhongji
	 * @date： 2015-9-11 上午09:49:55
	 * @param 需要替换的字符串
	 * @param 模块内用于替换的map
	 * @param 公共的用于替换的map
	 * @param 模块名
	 * @return String
	 */
	private String paramReplace_new(String s, Map<String, String> map, Map<String, String> gmap, String part) {
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
					if(inKey.contains("datapath")){
						//TODO 解析datapath
						s = addDataPathLabel(s, gmap.get(key));
					} else {
						s = s.replace(inKey, gmap.get(key));
					}
					
				}
			}
		}
		return s;
	}
	
	/**
	 * 动态生成<datapath>标签
	 * @author：zhuzhongji
	 * @date：2015-9-11 上午09:50:36
	 * @param xmlStr
	 * @param datapath
	 * @return String
	 */
	private String addDataPathLabel(String xmlStr, String datapath) {
		String[] datapaths = datapath.split(";");
		StringBuilder sb = new StringBuilder();
		for(String path : datapaths){
			sb.append("<dataPath path=\""+ path +"\"/>").append("\n");
		}
		//xmlStr.replace(sb.toString(), "datapath");  //用replace和replaceAll这两个方法都不能替换
		xmlStr = Pattern.compile("datapath").matcher(xmlStr).replaceFirst(sb.toString());
		
		return xmlStr;
	}

	public Map<String, String> getConfigFullMap(JobDatasyncConfig config) {
		Map<String, String> map = new HashMap<String, String>();
		Field[] fields = JobDatasyncConfig.class.getDeclaredFields();
		Field.setAccessible(fields, true);
		for (Field field : fields) {
			if (field.getName().endsWith("DatasourceId")) {
				try {
					Object value = field.get(config);

					if (value != null) {
						Datasource ds = (Datasource) value;
						Map<String, String> dataSourceMap = this.getDataSourceMap(ds.getDatasourceId());
						String key = field.getName().replace("DatasourceId", "").replace("datasourceBy", "");
						key = key.substring(0, 1).toLowerCase() + key.substring(1);
						for (String s : dataSourceMap.keySet()) {
							String subkey = "${" + key + s.substring(2, 3).toUpperCase() + s.substring(3);
							map.put(subkey, dataSourceMap.get(s));
						}

					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				try {
					Object value = field.get(config);
					if (value != null) {
						map.put("${" + field.getName() + "}", value.toString());
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

		return map;
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

						//System.out.println("key:"+key);  // key:   target   value:  与target对应的dataSourceMap
						//System.out.println("key对应的dataSourceMap组装完毕");
						map.put(key, dataSourceMap);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				//System.out.println("global map的属性: "+field.getName());
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
		map.put("global", globalMap);
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
	private Map<String, String> getDataSourceMap(long datasourceid) throws IllegalArgumentException, IllegalAccessException {
		// 参数改为传入datasourceid,然后从数据库重新查询一次.  如果直接传入ds的话,后面的 value = field.get(ds); 取不到值.
		DatasourceService datasourceService = BeanFactory.getService(DatasourceService.class);
		Datasource ds = datasourceService.get(datasourceid);

		//增加密码的加密
		try {
			String password = DxDESCipher.DecryptDES(ds.getPassword(), ds.getUsername());
			ds.setPassword(DxDESCipher.EncryptDES(password));
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		Map<String, String> map = new HashMap<String, String>();
		if (ds != null) {
			Field[] fields = Datasource.class.getDeclaredFields();
			Field.setAccessible(fields, true);
			for (Field field : fields) {

				Object value = null;

				//System.out.println(field.getName()+"======"+field.get(ds));

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

	private String getInitializerByFile() throws IOException {
		return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/initializer.xml", "UTF-8");
	}

	private String getFinalizerByFile() throws IOException {
		return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/finalizer.xml", "UTF-8");
	}

	private SourceTargetPair<DBType> getDataxSourceTarget(long jobType) {

		switch (JobType.valueOf((int) jobType)) {
			case FTP_FILE_TO_HDFS:
				System.out.println("FTP_FILE_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.File, DBType.HDFS);
			case FTP_FILE_TO_HDFS_FIVE_MINUTE:
				System.out.println("FTP_FILE_TO_HDFS_FIVE_MINUTE");
				return new SourceTargetPair<DBType>(DBType.File, DBType.HDFS);
			case FTP_FILE_TO_HDFS_YESTERDAY:
				System.out.println("FTP_FILE_TO_HDFS_YESTERDAY");
				return new SourceTargetPair<DBType>(DBType.File, DBType.HDFS);
				
				
				//add zhuzhongji 2015年9月11日09:23:28
			case FTP_FILE_TO_MYSQL:
				System.out.println("FTP_FILE_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.Mysql);
			case FTP_FILE_TO_SQLSERVER:
				System.out.println("FTP_FILE_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.Sqlserver);	
			case FTP_FILE_TO_ORACLE:
				System.out.println("FTP_FILE_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.Oracle);	
			case FTP_FILE_TO_LOCAL_FILE:
				System.out.println("FTP_FILE_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.File);	
			case FTP_FILE_TO_CSV:
				System.out.println("FTP_FILE_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.CSV);	
			case FTP_FILE_TO_GP:
				System.out.println("FTP_FILE_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.Greenplum);	
			case FTP_FILE_TO_FTP_FILE:
				System.out.println("FTP_FILE_TO_FTP_FILE");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.Ftp);	
			case FTP_FILE_TO_HBASE:
				System.out.println("FTP_FILE_TO_HBASE");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.HBase);	
			case FTP_FILE_TO_FTP_HDFS:
				System.out.println("FTP_FILE_TO_FTP_HDFS");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.HDFS);	
				
				

			case HDFS_TO_MYSQL:
				System.out.println("HDFS_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.Mysql);
			case HDFS_TO_ORACLE:
				System.out.println("HDFS_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.Oracle);
			case HDFS_TO_SQLSERVER:
				System.out.println("HDFS_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.Sqlserver);
			case HDFS_TO_LOCAL_FILE:
				System.out.println("HDFS_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.File);
			case HDFS_TO_HDFS:
				System.out.println("HDFS_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.HDFS);
			case HDFS_TO_CSV:
				System.out.println("HDFS_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.CSV);
			case HDFS_TO_GP:
				System.out.println("HDFS_TO_GP");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.Greenplum);
				
				//add by zhuzhongji 2015年9月11日09:24:10
			case HDFS_TO_FTP:
				System.out.println("HDFS_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.Ftp);
			case HDFS_TO_HBASE:
				System.out.println("HDFS_TO_HBASE");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.HBase); 

			case LOCAL_FILE_TO_HDFS:
				System.out.println("LOCAL_FILE_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.File, DBType.HDFS);
			case LOCAL_FILE_TO_MYSQL:
				System.out.println("LOCAL_FILE_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.File, DBType.Mysql);
			case LOCAL_FILE_TO_SQLSERVER:
				System.out.println("LOCAL_FILE_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.File, DBType.Sqlserver);
			case LOCAL_FILE_TO_ORACLE:
				System.out.println("LOCAL_FILE_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.File, DBType.Oracle);
			case LOCAL_FILE_TO_LOCAL_FILE:
				System.out.println("LOCAL_FILE_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.File, DBType.File);
			case LOCAL_FILE_TO_CSV:
				System.out.println("LOCAL_FILE_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.File, DBType.CSV);
			case LOCAL_FILE_TO_GP:
				System.out.println("LOCAL_FILE_TO_GP");
				return new SourceTargetPair<DBType>(DBType.File, DBType.Greenplum);
				
				// add by zhuzhongji 2015年9月11日09:24:39
			case LOCAL_FILE_TO_FTP:
				System.out.println("LOCAL_FILE_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.File, DBType.Ftp);
			case LOCAL_FILE_TO_HBASE:
				System.out.println("LOCAL_FILE_TO_HBASE");
				return new SourceTargetPair<DBType>(DBType.File, DBType.HBase);


			case MYSQL_TO_HDFS:
				System.out.println("MYSQL_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.HDFS);
			case MYSQL_TO_SQLSERVER:
				System.out.println("MYSQL_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.Sqlserver);
			case MYSQL_TO_ORACLE:
				System.out.println("MYSQL_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.Oracle);
			case MYSQL_TO_LOCAL_FILE:
				System.out.println("MYSQL_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.File);
			case MYSQL_TO_MYSQL:
				System.out.println("MYSQL_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.Mysql);
			case MYSQL_TO_CSV:
				System.out.println("MYSQL_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.CSV);
			case MYSQL_TO_GP:
				System.out.println("MYSQL_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.Greenplum);
				
				//add by zhuzhongji 2015年9月11日09:24:57
			case MYSQL_TO_FTP:
				System.out.println("MYSQL_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.Ftp);
			case MYSQL_TO_HBASE:
				System.out.println("MYSQL_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.HBase);

			case ORACLE_TO_HDFS:
				System.out.println("ORACLE_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.HDFS);
			case ORACLE_TO_SQLSERVER:
				System.out.println("ORACLE_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.Sqlserver);
			case ORACLE_TO_ORACLE:
				System.out.println("ORACLE_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.Oracle);
			case ORACLE_TO_LOCAL_FILE:
				System.out.println("ORACLE_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.File);
			case ORACLE_TO_MYSQL:
				System.out.println("ORACLE_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.Mysql);
			case ORACLE_TO_CSV:
				System.out.println("ORACLE_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.CSV);
			case ORACLE_TO_GP:
				System.out.println("ORACLE_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.Greenplum);
				
				//add by zhuzhongji 2015年9月11日09:25:22
			case ORACLE_TO_FTP:
				System.out.println("ORACLE_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.Ftp);
			case ORACLE_TO_HBASE:
				System.out.println("ORACLE_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.HBase);

			case SQLSERVER_TO_HDFS:
				System.out.println("SQLSERVER_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.HDFS);
			case SQLSERVER_TO_SQLSERVER:
				System.out.println("SQLSERVER_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.Sqlserver);
			case SQLSERVER_TO_ORACLE:
				System.out.println("SQLSERVER_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.Oracle);
			case SQLSERVER_TO_LOCAL_FILE:
				System.out.println("SQLSERVER_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.File);
			case SQLSERVER_TO_MYSQL:
				System.out.println("SQLSERVER_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.Mysql);
			case SQLSERVER_TO_CSV:
				System.out.println("SQLSERVER_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.CSV);
			case SQLSERVER_TO_GP:
				System.out.println("SQLSERVER_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.Greenplum);
				
				//add by zhuzhongji 2015年9月11日09:25:40
			case SQLSERVER_TO_FTP:
				System.out.println("SQLSERVER_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.Ftp);
			case SQLSERVER_TO_HBASE:
				System.out.println("SQLSERVER_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.HBase);

			case CSV_TO_HDFS:
				System.out.println("CSV_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.HDFS);
			case CSV_TO_MYSQL:
				System.out.println("CSV_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.Mysql);
			case CSV_TO_SQLSERVER:
				System.out.println("CSV_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.Sqlserver);
			case CSV_TO_ORACLE:
				System.out.println("CSV_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.Oracle);
			case CSV_TO_LOCAL_FILE:
				System.out.println("CSV_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.File);
			case CSV_TO_CSV:
				System.out.println("CSV_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.CSV);
			case CSV_TO_GP:
				System.out.println("CSV_TO_GP");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.Greenplum);
				
				// add by zhuzhongji 2015年9月11日09:25:58
			case CSV_TO_FTP:
				System.out.println("CSV_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.Ftp);
			case CSV_TO_HBASE:
				System.out.println("CSV_TO_HBASE");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.HBase);
				

			case GP_TO_HDFS:
				System.out.println("SQLSERVER_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.HDFS);
			case GP_TO_SQLSERVER:
				System.out.println("SQLSERVER_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.Sqlserver);
			case GP_TO_ORACLE:
				System.out.println("SQLSERVER_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.Oracle);
			case GP_TO_LOCAL_FILE:
				System.out.println("SQLSERVER_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.File);
			case GP_TO_MYSQL:
				System.out.println("SQLSERVER_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.Mysql);
			case GP_TO_CSV:
				System.out.println("SQLSERVER_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.CSV);
			case GP_TO_GP:
				System.out.println("SQLSERVER_TO_GP");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.Greenplum);
				
				//add by zhuzhongji 2015年9月11日09:26:15
			case GP_TO_FTP:
				System.out.println("GP_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.Ftp);
			case GP_TO_HBASE:
				System.out.println("GP_TO_HBASE");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.HBase);
			
			// add by zhuzhongji 2015年9月11日09:26:49
			case HBASE_TO_MYSQL:
				System.out.println("HBASE_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.Mysql);
			case HBASE_TO_SQLSERVER:
				System.out.println("HBASE_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.Sqlserver);
			case HBASE_TO_ORACLE:
				System.out.println("HBASE_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.Oracle);
			case HBASE_TO_HDFS:
				System.out.println("HBASE_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.HDFS);
			case HBASE_TO_LOCAL_FILE:
				System.out.println("HBASE_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.File);
			case HBASE_TO_CSV:
				System.out.println("HBASE_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.CSV);
			case HBASE_TO_GP:
				System.out.println("HBASE_TO_GP");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.Greenplum);
			case HBASE_TO_FTP:
				System.out.println("HBASE_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.Ftp);
			case HBASE_TO_HBASE:
				System.out.println("HBASE_TO_HBASE");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.HBase);

				
				//add by zhoushasha 2016/05/10 17:26:51
			case MONGODB_TO_MYSQL:
				System.out.println("MONGODB_TO_MYSQL");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.Mysql);
			case MONGODB_TO_SQLSERVER:
			System.out.println("MONGODB_TO_SQLSERVER");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.Sqlserver);
			case MONGODB_TO_ORACLE:
			System.out.println("MONGODB_TO_ORACLE");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.Oracle);
			case MONGODB_TO_HDFS:
			System.out.println("MONGODB_TO_HDFS");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.HDFS);
			
			case MONGODB_TO_LOCAL_FILE:
			System.out.println("MONGODB_TO_LOCAL_FILE");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.File);
			case MONGODB_TO_CSV:
			System.out.println("MONGODB_TO_CSV");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.CSV);
			case MONGODB_TO_GP:
				System.out.println("MONGODB_TO_GP");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.Greenplum);
			case MONGODB_TO_FTP:
				System.out.println("MONGODB_TO_FTP");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.Ftp);	
			case MONGODB_TO_HBASE:
				System.out.println("MONGODB_TO_HBASE");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.HBase);
			case MONGODB_TO_MONGODB:
				System.out.println("MONGODB_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.MongoDb, DBType.MongoDb);
			case MYSQL_TO_MONGODB:
				System.out.println("MYSQL_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.Mysql, DBType.MongoDb);
			case SQLSERVER_TO_MONGODB:
				System.out.println("SQLSERVER_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.Sqlserver, DBType.MongoDb);
			case ORACLE_TO_MONGODB:
				System.out.println("ORACLE_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.Oracle, DBType.MongoDb);
			case FTP_TO_MONGODB:
				System.out.println("FTP_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.Ftp, DBType.MongoDb);
			case HDFS_TO_MONGODB:
				System.out.println("HDFS_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.HDFS, DBType.MongoDb);
			case LOCAL_FILE_TO_MONGODB:
				System.out.println("LOCAL_FILE_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.File, DBType.MongoDb);
			case CSV_TO_MONGODB:
				System.out.println("CSV_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.CSV, DBType.MongoDb);
			case GP_TO_MONGODB:
				System.out.println("GP_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.Greenplum, DBType.MongoDb);
			case HBASE_TO_MONGODB:
				System.out.println("HBASE_TO_MONGODB");
				return new SourceTargetPair<DBType>(DBType.HBase, DBType.MongoDb);
			default:
				return null;
		}
	}

	private String createSourcePart(DBType source) {
		try {
			return "   <source>\n" + this.getXMLByFile(source) + "   </source>\n";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private String createTargetPart(DBType target) {
		try {
			return "   <target>\n" + this.getXMLByFile(target) + "   </target>\n";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private String createInitializer() {
		try {
			return this.getInitializerByFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private String createFinalizer() {
		try {
			return this.getFinalizerByFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getXMLByFile(DBType type) throws IOException {
		if (type == null) {
			return "";
		}
		switch (type) {
			case File:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/file.xml", "UTF-8");
			case HDFS:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/hdfs.xml", "UTF-8");
			case Mysql:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/mysql.xml", "UTF-8");
			case Oracle:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/oracle.xml", "UTF-8");
			case Sqlserver:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/sqlserver.xml", "UTF-8");
			case CSV:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/csv.xml", "UTF-8");
			case Greenplum:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/gp.xml", "UTF-8");
			
			// add by zhuzhongi 2015年9月11日09:27:56
			case Ftp:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/ftp.xml", "UTF-8");
			case HBase:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/hbase.xml", "UTF-8");
				//add by zhoushasha   2016/05/10 17:26:51
			case MongoDb:
				return DxFileUtils.file2String(Parameters.dataxFileTemplateDir + "/mongodb.xml", "UTF-8");
		}
		return "";
	}

	/**
	 * 存储源和目标的类型格式
	 * 
	 * @author dly
	 * 
	 * @param <T>
	 */
	public class SourceTargetPair<T> {
		public SourceTargetPair(T source, T target) {
			this.source = source;
			this.target = target;
		}

		private T source;

		public T getSource() {
			return source;
		}

		public void setSource(T source) {
			this.source = source;
		}

		private T target;

		public void setTarget(T target) {
			this.target = target;
		}

		public T getTarget() {
			return target;
		}
	}
}
