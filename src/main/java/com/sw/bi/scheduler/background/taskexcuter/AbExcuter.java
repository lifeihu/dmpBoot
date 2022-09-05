package com.sw.bi.scheduler.background.taskexcuter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.RedoAndSupplyHistoryService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.ActionStatus;
import com.sw.bi.scheduler.util.DateUtil;

public abstract class AbExcuter {
	public abstract boolean excuteCommand() throws Exception;

	private static final Logger log = Logger.getLogger(AbExcuter.class);
	protected Task currentTask;
	protected Job currentJob;
	protected Action currentAction;
	protected String m_logFolder;

	protected ActionService actionService = BeanFactory.getService(ActionService.class);

	protected TaskService taskService = BeanFactory.getService(TaskService.class);

	protected JobService jobService = BeanFactory.getService(JobService.class);

	private RedoAndSupplyHistoryService redoAndSupplyHistoryService = BeanFactory.getService(RedoAndSupplyHistoryService.class);

	protected FileWriter logFileWriter;

	protected Map<String, String> runtimeParamters;

	public AbExcuter() {}

	/**
	 * @param task
	 * @param logFolder
	 *            当前的日志路径 /home/tools/logs/etl_log/2012-09-04/
	 */
	public AbExcuter(Task task, String logFolder) {
		this.currentTask = taskService.get(task.getTaskId());
		this.currentJob = jobService.get(task.getJobId());
		this.m_logFolder = logFolder;

		// 获得当前任务的运行时参数
		this.runtimeParamters = Parameters.getRunTimeParamter(this.currentTask);
	}
	
	protected void closeLog() {
		IOUtils.closeQuietly(logFileWriter);
	}

	
	protected void openLog() {
		try {
			File loggerFile = new File(this.getLogPathName());
			if (!loggerFile.exists()) {
				loggerFile.createNewFile();
			}

			logFileWriter = new FileWriter(loggerFile, true);

			return;

		} catch (IOException e) {
			e.printStackTrace();
			log.error("日志文件创建失败(" + this.getLogPathName() + ")");
			return;
		}
	}
	
	public boolean excute() {
		if (this.prepare()) {
			try {
				
				boolean result = this.excuteCommand(); //真正执行任务的地方。  返回值就是执行实例的执行结果
				this.updateStatus(result); //状态回填(任务状态回填,action状态回填,重跑/补数据操作状态回填)

				return result;
			} catch (Exception e) {
				try {
					logFileWriter.write(e.getMessage() + "\r\n");
					for (StackTraceElement stack : e.getStackTrace()) {
						logFileWriter.write(stack.toString() + "\r\n");
					}
				} catch (IOException ie) {
					ie.printStackTrace();
				}

				this.updateStatus(false);

			} finally {
				if (logFileWriter != null) {
					try {
						logFileWriter.close();
						logFileWriter = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return false;
	}

	/**
	 * 根据指定的执行结果修改Action的状态
	 * 
	 * @param result
	 */
	public void updateStatus(boolean result) {
		currentAction.setEndTime(DateUtil.now());
		currentAction.setActionStatus(result ? ActionStatus.RUN_SUCCESS.indexOf() : ActionStatus.RUN_FAILURE.indexOf());
		currentAction.setActionLog(this.getLogPathName());

		if (taskService.runFinished(currentTask, currentAction)) {
			actionService.saveOrUpdate(currentAction);
			actionService.flush();
		}

		log("保存任务及Action状态");
		taskService.flush();
	}

	/**
	 * <pre>
	 * 	设置执行Action
	 * 	根据2014-04-24的解决方法，Action不再由执行器创建，所以需要由ExcuterCenter传入
	 * </pre>
	 * 
	 * @param executeAction
	 */
	public void setExecuteAction(Action executeAction) {
		this.currentAction = actionService.get(executeAction.getActionId());
	}

	/**
	 * 获得日志文件全路径 /home/tools/logs/etl_log/2012-09-04/actionid.log
	 * 
	 * @return
	 */
	protected String getLogPathName() {
		return this.m_logFolder + this.currentAction.getActionId() + ".log";
	}

	/**
	 * 写入执行日志
	 * 
	 * @param content
	 */
	protected void log(String content) {
		try {
			logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - " + content + "\r\n");
			logFileWriter.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


    /**
     * 对指定内容进行所有参数替换
     * 
     * @param text
     * 
     * modify by chenpp 20160615
     */
    protected String replaceParams(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        for (Entry<String, String> entry : this.runtimeParamters.entrySet()) {
            String key = entry.getKey().replace("$", "\\$").replace("{", "\\{").replace("}", "\\}");
            text = text.replaceAll(key, entry.getValue());

        }
		/*****************************************************************************************************************/
		Calendar calendar = DateUtil.cloneCalendar();
		calendar.setTime(this.currentTask != null ? this.currentTask.getSettingTime() : new Date());
        if(text.contains("month_now")){
            String regHour = "\\$\\{month_now[^\\$]*\\}";    
            Pattern p = Pattern.compile(regHour);
            Matcher m = p.matcher(text);
            while(m.find()){
                calendar.add(Calendar.MONTH, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
                text= text.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMM"));
            }
        }      
        if(text.contains("date_now")){
            String reg = "\\$\\{date_now[^\\$]*\\}";    
            Pattern p = Pattern.compile(reg);
            Matcher m = p.matcher(text);
            while(m.find()){
                //calendar.add(Calendar.DATE, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
                //text= text.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMMdd"));
                
                String[] config = m.group().split(",");
                config[config.length - 1] = config[config.length - 1].substring(0, config[config.length - 1].length() - 1);
            	calendar.add(Calendar.DATE, 0-Integer.parseInt(config[1]));
                if (config.length == 3) {
                	text = text.replace(m.group(), DateUtil.format(calendar.getTime(), config[2]));
                } else {
                	text = text.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMMdd"));
                }
            }
        }
        if(text.contains("hour_now")){
            String regHour = "\\$\\{hour_now[^\\$]*\\}";    
            Pattern p = Pattern.compile(regHour);
            Matcher m = p.matcher(text);
            while(m.find()){
                calendar.add(Calendar.HOUR, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
                text= text.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
            }
        }

		/*****************************************************************************************************************/

		return text;
    }

	/**
	 * 根据父任务状态等条件判断自己是否真的要执行
	 * 
	 * @return
	 */
	private boolean prepare() {
		// 如果日志文件记录器有打开则先关闭，再覆盖写入(理论上不会有这种情况发生的)
		if (logFileWriter != null) {
			IOUtils.closeQuietly(logFileWriter);
		}

		boolean success = true;

		// 通过ExcuterCenter过来的任务Action都有传,但分支执行比较特殊暂时不想太多改动导致不可控的结果所以还是按原来的逻辑(Action为空)
		if (currentAction == null) {
			if (taskService.runBegin(currentTask)) { //runBegin: 将任务状态先设置为运行状态,然后进行反查,如果反查成功,则表示该任务可以真正被执行了。
				// 为任务创建一条执行明细
				currentAction = actionService.create(currentTask);
				currentAction.setActionLog(m_logFolder);
				currentAction.setStartTime(DateUtil.now());
				currentAction.setActionStatus(ActionStatus.RUNNING.indexOf());
				currentAction.setGateway(Configure.property(Configure.GATEWAY));
				currentAction.setUpdateTime(DateUtil.now());
				actionService.saveOrUpdate(currentAction);

				// 需要将新建的ActionID回填到Task表
				currentTask.setLastActionId(currentAction.getActionId());
				taskService.update(currentTask);

				// 回填重跑/补数据操作历史中的数据
				redoAndSupplyHistoryService.taskRunBegin(currentTask);

			} else {
				success = false;
			}
		}

		if (!success) {
			return false;
		}

		try {
			File loggerFile = new File(this.getLogPathName());
			if (!loggerFile.exists()) {
				loggerFile.createNewFile();
			}

			logFileWriter = new FileWriter(loggerFile, true);

			return true;

		} catch (IOException e) {
			e.printStackTrace();
			log.error("日志文件创建失败(" + this.getLogPathName() + ")");
			return false;
		}
	}
}
