package com.sw.bi.scheduler.background.taskexcuter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.taskexcuter.xml.DxFileUtils;
import com.sw.bi.scheduler.background.taskexcuter.xml.XmlCreator;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.DataxExecuteSummary;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.DataxExecuteSummaryService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.util.DateUtil;

// 事先准备: 需要将模板文件拷贝到/root/scheduler/datax_file_template/下面.
public class DataxExcuter extends AbExcuter {

	public DataxExcuter() {}

	public DataxExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	private String errorThreshold;

	@Override
	public boolean excuteCommand() throws Exception {
		/////////////////////////////////////////////////////////
		System.out.println("datax job begin...");
		XmlCreator xmlCreator = new XmlCreator();
		System.out.println("begin to create xml file...");
		String XML = xmlCreator.createByTask(this.currentTask);
		this.errorThreshold = xmlCreator.getErrorThreshold();
		System.out.println("xml file is created...");
		/////////////////////////////////////////////////////////
		
		//lifeng  2015.1.6  打印XML文件的，调试时才用。正式使用时屏蔽这句，避免将XML文件内容记录到日志中
		System.out.println("xml file is: \n" + XML); 

		if (XML.length() > 0) {
			try {
				String fileDir = Parameters.tempXmlPath + "/" + new SimpleDateFormat("yyyyMMdd").format(new Date());
				System.out.println("log的路径1"+fileDir);
				String tempPath = fileDir + "/" + this.currentAction.getActionId() + ".xml";
				System.out.println("log的路径2"+fileDir);
				File fileD = new File(fileDir);
				if (!fileD.exists()) {
					fileD.mkdirs();
				}

				DxFileUtils.string2File(XML, tempPath, "UTF-8");
				Process process = this.programeRun(tempPath);
				process.waitFor();

				/*				下面这段暂时屏蔽. 正式上线时取消注释.
				                File tmphivesql = new File(tempPath);
								if (tmphivesql.exists()) {
									tmphivesql.delete();
								}*/

				//这里还要再完善一下.  当datanode2忘记将datax工具部署上去的时候,应该提示没有找到datax工具.
				//目前是直接去执行checkDataxResult这个方法了. 然后提示/home/tools/logs/etl_log/2012-04-06/815.flag (No such file or directory)

				//提示 /home/tools/logs/etl_log/2012-04-06/815.flag (No such file or directory) 还有个原因可能是: 
				//将hadoop conf下面的3个配置文件拷贝到datax目录下面
				//将最新的hadoop,hive的jar包拷贝到datax的lib下面
				//conf目录下engine.properties和system.properties的安装路径没写对. 也可能是datax_hadoop中的路径没写对.
				//也有可能是datax工具没有可执行权限。 chmod +x -R datax/

				//  datax返回的是检查容错率后的结果,而不是进程的返回状态. 而发生运行时异常时,那个统计数字又不准确. 其实根本没写入. 统计数字写失败的数字是0,导致最终checkDataxResult返回是OK	
				//  如果datax的返回码是2(运行时错误),则任务最终运行状态是运行失败. 并且本次执行 不记录到datax执行概况信息表中
				if (process.exitValue() == 2) { //这里要写2,  不要写!=0
					return false;
				} else {
					boolean checkResult = this.checkDataxResult();
					System.out.println("this.checkDataxResult():.......这个是datax的日志 " + checkResult);

					if (checkResult) {
						return this.referenceHdfs2Table();
					}

					return checkResult;
				}

			} catch (IOException e) {
				throw e;
			} catch (InterruptedException e) {
				throw e;
			}
		} else {
			throw new Exception("XMLError");
		}

	}

	public boolean referenceHdfs2Table() {
		JobDatasyncConfigService jobDatasyncConfigService = BeanFactory.getService(JobDatasyncConfigService.class);
		JobDatasyncConfig config = jobDatasyncConfigService.getJobDatasyncConfigByJob(this.currentTask.getJobId());

		String referTableName = config.getReferTableName();
		String referPartName = config.getReferPartName();

		if (StringUtils.hasText(referTableName) && StringUtils.hasText(referPartName)) {
			String location = config.getTargetDatapath();
			int pos = location.lastIndexOf("/");
			String fileName = location.substring(pos + 1);
			if (fileName.indexOf(".") > -1) {
				location = location.substring(0, pos);
			}

			StringBuffer sql = new StringBuffer();
			sql.append("use ").append(config.getReferDbName()).append(";");
			sql.append("alter table ").append(this.replaceParams(referTableName));
			sql.append(" add if not exists partition(").append(this.replaceParams(referPartName)).append(")");
			sql.append(" location '").append(this.replaceParams(location)).append("';");

			System.out.println("引用HDFS目录到指定表.");
			System.out.println(sql.toString());

			try {
				String[] commands = new String[] { "/bin/bash", "-c", "hiveclient -e \"" + sql.toString() + "\"" };
				Process process = Runtime.getRuntime().exec(commands);
				process.waitFor();

				boolean result = process.exitValue() == 0;
				if (!result) {
					System.out.println("exit value: " + process.exitValue());

					System.out.println("error stream: " + IOUtils.toString(process.getErrorStream(), "utf-8"));
					System.out.println("input stream: " + IOUtils.toString(process.getInputStream(), "utf-8"));
				}

				return result;

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	/**
	 * 检查任务运行结果
	 * 
	 * @return 把datax执行结果保存到数据库表中,并判断"实际出错率"是否超过"容错率"
	 * @throws IOException
	 * @throws ParseException
	 */
	private boolean checkDataxResult() throws IOException, ParseException {
		// 现在DataX的日志都将存放在tmp目录下，所以这里需要到tmp目录中取flag文件
		String flagPath = this.m_logFolder + "tmp/" + this.currentAction.getActionId() + ".flag"; // this.getLogPathName() + "".replace(".log", ".flag");
		System.out.println("log的路径1"+flagPath);
		File file = new File(flagPath);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;

		DataxExecuteSummary dataxSummary = new DataxExecuteSummary();
		dataxSummary.setActionid(this.currentAction.getActionId());
		int index = 0;
		// CustomDateFormat format = new CustomDateFormat();
		while ((line = br.readLine()) != null && index < 8) {
			System.out.println("index......."+index);
			switch (index) {
				case 0:
					dataxSummary.setXmlFile(line);
					break;
				case 1:
					// dataxSummary.setBeginTime(new Timestamp( format.parse(line).getTime()));
					dataxSummary.setBeginTime(new Timestamp(DateUtil.parse(line, "yyyy-MM-dd HH:mm:ss").getTime()));
					break;
				case 2:
					// dataxSummary.setEndTime(new Timestamp( format.parse(line).getTime()));
					dataxSummary.setEndTime(new Timestamp(DateUtil.parse(line, "yyyy-MM-dd HH:mm:ss").getTime()));
					break;
				case 3:
					dataxSummary.setRunTime(new Long(line));
					break;
				case 4:
					dataxSummary.setTotalLines(new Integer(line));//notice
					break;
				case 5:
					dataxSummary.setTotalReadFailedLines(new Integer(line));
					break;
				case 6:
					dataxSummary.setTotalWriteSuccessLines(new Integer(line));
					break;
				case 7:
					dataxSummary.setTotalWriteFailedLines(new Integer(line));
					break;
			}
			index++;
		}
		if (index == 8) {
			dataxSummary.setTotalFailedLines(dataxSummary.getTotalReadFailedLines() + dataxSummary.getTotalWriteFailedLines());

			dataxSummary.setTotalReadSuccessLines(dataxSummary.getTotalLines() - dataxSummary.getTotalReadFailedLines());

			dataxSummary.setCreateTime(new Timestamp(new Date().getTime()));
			dataxSummary.setUpdateTime(new Timestamp(new Date().getTime()));

			DataxExecuteSummaryService dataxExcuteService = BeanFactory.getService(DataxExecuteSummaryService.class);
			dataxExcuteService.saveOrUpdate(dataxSummary);
			System.out.println("datax执行概况信息已经保存到数据库中.");
			br.close();
			return this.checkFailedOutRange(dataxSummary);
		}

		return false;
	}

	/**
	 * 判断执行出错率是否超出最大容错率
	 * 
	 * @param dataxSummary
	 * @return
	 */
	private boolean checkFailedOutRange(DataxExecuteSummary dataxSummary) {
		System.out.println("begin to checkFailedOutRange......");
		if (errorThreshold != null && !errorThreshold.isEmpty()) {
			float errorth = new Float(errorThreshold);
			if (errorth >= 1) {
				System.out.println("错误记录:" + dataxSummary.getTotalFailedLines() + "条");
				System.out.println("设置最多允许错误" + errorth + "条");
				boolean result = dataxSummary.getTotalFailedLines() <= errorth;
				System.out.println("result: " + result);
				return result;
			} else {
				if (dataxSummary.getTotalLines() == 0 && dataxSummary.getTotalFailedLines() <= 0) {
					System.out.println("total_line is 0");
					return true;
				}
				float trueErrorth = dataxSummary.getTotalFailedLines().floatValue() / dataxSummary.getTotalLines().floatValue();
				System.out.println("实际错误率:" + trueErrorth);
				System.out.println("设置的最大错误率: " + errorth);
				boolean result = trueErrorth <= errorth;
				System.out.println("result: " + result);
				return result;
			}
		}
		return false;
	}

	public Process programeRun(String tmp_xml_config_path) throws IOException {
		// 为了能准确查到进程ID，尽量通过>方式来输出日志，但DataX执行器之前日志输出是通过传递日志目录由DataX程序去输出的
		// 但如果改成了>方式，则可能会出现二种方式都同时往同一个文件写日志，所以这里将传入DataX的目录转向了tmp目录
		String[] commands = new String[] {
				"/bin/bash",
				"-c",
				"cd " + Parameters.dataxToolPath + " && ./run.sh" + " " + tmp_xml_config_path + " " + this.m_logFolder + "tmp/ " + this.currentAction.getActionId() + " > " + this.getLogPathName() +
						" 2>&1" };
		
		//把每个元素按次序拼接转成字符串
		StringBuffer str2 = new StringBuffer();
		for (String string : commands) {
			str2.append(string);
			}
		System.out.println("老datax的执行命令"+str2.toString());
		
		Process child = Runtime.getRuntime().exec(commands);
		return child;
	}

}
