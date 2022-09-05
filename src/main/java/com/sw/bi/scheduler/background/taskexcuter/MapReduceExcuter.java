package com.sw.bi.scheduler.background.taskexcuter;

import com.sw.bi.scheduler.model.Task;

public class MapReduceExcuter extends AbExcuter {

	public MapReduceExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		String cmd = currentJob.getProgramPath();
		cmd = this.replaceParams(cmd);

		// 对作业ID是2156的作业进行特殊处理,在末尾加上taskId参数
		if (currentJob.getJobId() == 2156l) {
			cmd += " " + currentTask.getTaskId();
			log("#2156: " + cmd);
			System.out.println("----------------------> #2156: " + cmd);
		}

		Process process;
		try {
			process = programeRun(cmd);
			process.waitFor();
			return process.exitValue() == 0;
		} catch (Exception e) {
			throw e;
		}

	}

	/**
	 * 参数替换函数(在抽象类中实现了该方法)
	 * 
	 * @param cmd
	 * @return
	 */
	/*private String replaceParams(String cmd) {
		java.util.Map<String, String> map = Parameters.getRunTimeParamter(currentTask);
		for (String s : map.keySet()) {
			cmd = cmd.replace(s, map.get(s));
		}
		return cmd;
	}*/

	private Process programeRun(String cmd) throws Exception {
		String[] commands = new String[] { "/bin/bash", "-c", cmd + " > " + this.getLogPathName() + " 2>&1" };
		Process process = Runtime.getRuntime().exec(commands);
		return process;
	}

}
