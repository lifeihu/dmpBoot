package com.sw.bi.scheduler.background.taskexcuter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
import com.sw.bi.scheduler.util.FileUtil;
import com.sw.bi.scheduler.util.FlagCreate;
import com.sw.bi.scheduler.util.JobJsonUtil;
import com.sw.bi.scheduler.util.ReadFile;

/**
 * 新的dataX任务调度
 * 
 * @author qyx
 *
 */
public class NewDataxExcuter extends AbExcuter {

	// 方便测试的初始化类
	// public NewDataxExcuter() {
	//
	// }

	/*
	 * 对任务进行日志记录
	 */
	public NewDataxExcuter(Task task, String logFolder) {
		// 当前的日志路径 /home/tools/logs/etl_log/2012-09-04/
		super(task, logFolder);
	}

	/*
	 * 执行命令
	 */
	@Override
	public boolean excuteCommand() throws Exception {

		// message初始化;
		BufferedReader message = null;
		String line;
		Process process = null;

		// 1.从前端获取输入的路径参数 String path = currentJob.getProgramPath();
		// 这里不是用这个字段来获取json配置路径
		JobDatasyncConfigService service = BeanFactory.getService(JobDatasyncConfigService.class);
		JobDatasyncConfig jobDatasyncConfigByJob = service.getJobDatasyncConfigByJob(this.currentJob.getJobId());
		String userXml = jobDatasyncConfigByJob.getUserXml();

		// String userXml = "/Users/qyx/Desktop/testdatax.json";
		System.out.println("userXml..." + userXml);

		// 2.生成临时文件夹(格式：/home/tools/temp/newdataxtemp/20190103/3855/)
		String fileDir = Parameters.tempNewDataxPath + new SimpleDateFormat("yyyyMMdd").format(new Date());
		// 定义的Python生成dataX的Json模板的路径地址,
		String tempPath = fileDir + "/" + this.currentAction.getActionId() + "/";
		File fileD = new File(tempPath);
		if (!fileD.exists()) {
			fileD.mkdirs();
		}
		System.out.println("定义的Python生成dataX的Json模板的路径地址:"+tempPath);
		// 3.加入3种不同情况的判断,来进行对不同情况的执行任务,

			if (jobDatasyncConfigByJob.getJobType() == 103
					|| (jobDatasyncConfigByJob.getJobType() >= 2030 && jobDatasyncConfigByJob.getJobType() <= 3040)) {
				System.out.println("jobType...:"+jobDatasyncConfigByJob.getJobType());
				// "通用配置"模式
				if (jobDatasyncConfigByJob.getJobType() != 103 ) {
					System.out.println("jobType...:"+jobDatasyncConfigByJob.getJobType());
					// add by mashifeng 2018年12月25日10:23:26
					System.out.println("datax job begin...");
					JobJsonUtil Json = new JobJsonUtil();
					System.out.println("begin to create json file...");
					Json.createByTask(jobDatasyncConfigByJob, this.currentAction.getActionId());
					System.out.println("json file is created...");
					// 重新获取最新的json模板地址
					userXml = jobDatasyncConfigByJob.getUserXml();
					System.out.println("json file is "+userXml+" in here");
					
				}
				// "自定义配置"模式
				process = this.formatShellCommand(userXml,jobDatasyncConfigByJob.getJobType());
				InputStream stdin = process.getErrorStream();
				InputStreamReader isr = new InputStreamReader(stdin,"gbk");
				BufferedReader br = new BufferedReader(isr);
				String line2 = null;
				System.out.println("<OUTPUT>");
				while ( (line2 = br.readLine()) != null)
					System.out.println(line2);
				System.out.println("</OUTPUT>");
				
				process.waitFor();
				System.out.println("我的process值1..." + process.exitValue());

				openLog();
				if (process.exitValue() == 1) {
					return false;
				} else {
					// 生成flag文件
					FlagCreate.NewFlag(ReadFile.readLastNLine(new File(this.getLogPathName()), 8L),
							this.m_logFolder + "tmp/", this.currentAction.getActionId(), userXml);
					// 执行Json模板的时候返回的日志信息
					boolean checkResult = this.checkDataxResult(jobDatasyncConfigByJob);
					System.out.println("this.checkDataxResult():.......这个是datax的日志 " + checkResult);
					return checkResult;
				}
				// 第2种方式间接调用shell<先执行pyton程序，在遍历执行json模板>
			} else if (jobDatasyncConfigByJob.getJobType() == 104) {
				process = formatPythonCommand(userXml, tempPath);
				process.waitFor();
				if (process.exitValue() == 2) { // 这里要写2, 不要写!=0
					return false;			
				} else {
					// TODO 获取程序执行的输出流路径,这里打印的是程序执行的日志记录
					// process.getOutputStream();
					if (process.exitValue() == 0) {
						List<File> files = DxFileUtils.listAllFile(tempPath);
						System.out.println("这个目录下总共有"+files.size()+"个文件");
						for (File file : files) {
							String eachFilePath = file.getPath();
							Process shellProcess = formatShellCommand(eachFilePath,jobDatasyncConfigByJob.getJobType());
							shellProcess.waitFor();
							
							System.out.println("shellProcess ："+shellProcess.exitValue());
							if (shellProcess.exitValue() == 1) {
								System.out.println("shellProcess ："+shellProcess.exitValue());
								return false;
							} else {
								// 执行Json模板的时候返回的日志信息
								//boolean checkResult = this.checkDataxResult();
								//System.out.println("this.checkDataxResult():.......这个是datax的日志 " + checkResult);
								//return checkResult;
							}
						}
						return true;
					} else {
						// 执行Python失败的时候返回错误日志信息
						return false;
					}
				}
			} else {
				return false;// 新dataX暂不支持其他JobType的处理
			}
	}

	/*
	 * 执行方式1 :通过调用shell语句来执行datax运行命令来构建执行命令
	 * 
	 * @Param path : json配置文件的完整路径
	 */
	private Process formatShellCommand(String path,Long jobtype ) {
		StringBuilder cmdBuilder = new StringBuilder();
		if(jobtype == 103 || (jobtype>= 2030 && jobtype <= 3040)){
			cmdBuilder.append("datax.py ").append(path).append(" > ").append(this.getLogPathName()).append(" 2>&1");	
		}else if (jobtype == 104)
			cmdBuilder.append("datax.py ").append(path).append(" >> ").append(this.getLogPathName()).append(" 2>&1");
		// cmdBuilder.append("D:/DataX-master/bin/datax.py ").append(path);
		Process process = null;
		try {
			// 本地测试
			// process = Runtime.getRuntime().exec(arguments);
			System.out.println("执行前，新datax的执行命令......" + cmdBuilder);
			// 服务器上测试
//			String[] cmd = { "/bin/sh", "-c", cmdBuilder.toString() };
			// 本地测试
			String[] cmd = { "cmd ", "/c", cmdBuilder.toString() };
			System.out.println(cmd);
			// 把每个元素按次序拼接转成字符串
			closeLog();
			process = Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return process;
	}

	/*
	 * 执行方式2: 通过shell调用python文件,生成相关的json文件,读取json文件,调用datax来执行
	 * pyPath:指python程序的存储路径;tempPath:生成Json模板的存储路径
	 */
	private Process formatPythonCommand(String pyPath, String tempPath) {
		StringBuilder builder = new StringBuilder();
		builder.append("python ").append(pyPath).append(" " + tempPath + " > " + this.getLogPathName() + " 2>&1");
		Process process = null;
		System.out.println("执行前，新datax的执行命令......" + builder.toString());
		try {
			String[] cmd = { "/bin/sh", "-c", builder.toString() };
			process = Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return process;
	}

	/**
	 * 检查任务运行结果
	 * 
	 * @return 把datax执行结果保存到数据库表中,并判断"实际出错率"是否超过"容错率"
	 * @throws IOException
	 * @throws ParseException
	 */
	private boolean checkDataxResult(JobDatasyncConfig jobDatasyncConfig) throws IOException, ParseException {
		// 现在DataX的日志都将存放在tmp目录下，所以这里需要到tmp目录中取flag文件
		//ReadFile.readLastNLine(new File(this.getLogPathName()), 8L);

		String flagPath = this.m_logFolder + "tmp/" + this.currentAction.getActionId() + ".flag";

		System.out.println("log的路径1" + flagPath);
		File file = new File(flagPath);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;

		DataxExecuteSummary dataxSummary = new DataxExecuteSummary();
		dataxSummary.setActionid(this.currentAction.getActionId());
		int index = 0;
		// CustomDateFormat format = new CustomDateFormat();
		while ((line = br.readLine()) != null && index < 9) {
			System.out.println("index......." + index);
			switch (index) {
			case 1:
				// 读写失败总数
				dataxSummary.setTotalFailedLines(new Integer(line));
				break;
			case 2:
				// 读出记录总数
				dataxSummary.setTotalReadSuccessLines(new Integer(line));
				break;
			case 3:
				// 记录写入速度
				dataxSummary.setReadSpeed(new Integer(line.replace("rec/s", "")));
				break;
			case 4:
				// 任务平均流量
				dataxSummary.setTaskFlow(new Double(line.replace("KB/s", "").replace("B/s", "").replace("M", "")));
				break;
			case 5:
				// 任务总计耗时
				dataxSummary.setRunTime(new Long(line.replace("s", "")));
				break;
			case 6:
				// 任务结束时刻
				dataxSummary.setEndTime(new Timestamp(DateUtil.parse(line, "yyyy-MM-dd HH:mm:ss").getTime()));
				break;
			case 7:
				// 任务启动时刻
				dataxSummary.setBeginTime(new Timestamp(DateUtil.parse(line, "yyyy-MM-dd HH:mm:ss").getTime()));
				break;
			case 8:
				dataxSummary.setXmlFile(line);
				break;
			}
			index++;
		}
		if (index == 9) {

			dataxSummary.setCreateTime(new Timestamp(new Date().getTime()));
			dataxSummary.setUpdateTime(new Timestamp(new Date().getTime()));

			DataxExecuteSummaryService dataxExcuteService = BeanFactory.getService(DataxExecuteSummaryService.class);
			dataxExcuteService.saveOrUpdate(dataxSummary);
			System.out.println("datax执行概况信息已经保存到数据库中.");
			br.close();
			return this.checkFailedOutRange(dataxSummary, jobDatasyncConfig);
		}

		return false;
	}

	/**
	 * 检查文件夹下是否存在Json文件
	 * 
	 * @return 抛出Json文件不存在信息
	 */
	private boolean FileIsNull() {

		// 将错误信息写入到日志里
		File logfile = new File(this.getLogPathName());
		FileUtil.write(logfile, "文件夹下没有json文件！");
		return false;

	}

	/**
	 * 判断读写失败总数
	 * 
	 * @param dataxSummary
	 * @return
	 */
	private boolean checkFailedOutRange(DataxExecuteSummary dataxSummary, JobDatasyncConfig jobDatasyncConfig ) {
		
		
		if ((dataxSummary.getTotalFailedLines() / dataxSummary.getTotalReadSuccessLines()) > (Double.valueOf(jobDatasyncConfig.getErrorthreshold() )/ 100)) {
			return false;
		}
		
		if (dataxSummary.getTotalFailedLines() > jobDatasyncConfig.getErrorLimitrecords().intValue()) {
			return false;
		} else {
			return true;
		}
		
	}

	/*
	 * 执行测试
	 */
	public static void main(String[] args) {
		// NewDataxExcuter newDataxExcuter = new NewDataxExcuter();
		// newDataxExcuter.excuteCommand();
		// try {
		// System.out.println("start test new datax task");
		//// String windowcmd = "cmd /c python datax.py
		// /Users/qyx/Desktop/testdatax.json";
		// String localcmd = "python datax.py
		// /Users/qyx/Desktop/testdatax.json";
		// System.out.println("datax 的执行命令为:" + localcmd);
		// // .exec("你的命令",null,new File("datax安装路径"));
		// Process pr = Runtime.getRuntime().exec(localcmd, null,
		// new File("/Users/qyx/datax/bin"));
		// BufferedReader in = new BufferedReader(new
		// InputStreamReader(pr.getInputStream()));
		// String line = null;
		// while ((line = in.readLine()) != null) {
		// System.out.println("输出的内容为:" + line);
		// }
		// in.close();
		// pr.waitFor();
		// System.out.println("end test new datax task");
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

	}
}
