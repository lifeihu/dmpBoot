package com.sw.bi.scheduler.background.taskexcuter;

import java.util.Arrays;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.JobService;

public class ExcuterFactory {
	private static JobService jobService = BeanFactory.getService(JobService.class);

	public static DataxExcuter getDataxExcuter(Task task, String logFolder) {
		return new DataxExcuter(task, logFolder);
	}

	public static HiveSqlExcuter getHiveSqlExcuter(Task task, String logFolder) {
		return new HiveSqlExcuter(task, logFolder);
	}

	public static MapReduceExcuter getMapReduceExcuter(Task task, String logFolder) {
		return new MapReduceExcuter(task, logFolder);
	}

	public static ShellExcuter getShellExcuter(Task task, String logFolder) {
		return new ShellExcuter(task, logFolder);
	}

	public static FtpExcuter getFtpExcuter(Task task, String logFolder) {
		return new FtpExcuter(task, logFolder);
	}

	public static ProcedureExcuter getProcedureExcuter(Task task, String logFolder) {
		return new ProcedureExcuter(task, logFolder);
	}

	public static MailJobExcuter getMailJobExcuter(Task task, String logFolder) {
		return new MailJobExcuter(task, logFolder);
	}

	public static ReportQualityExcuter getReportQualityExcuter(Task task, String logFolder) {
		return new ReportQualityExcuter(task, logFolder);
	}

	public static CheckDependencyExcuter getCheckDependencyExcuter(Task task, String logFolder) {
		return new CheckDependencyExcuter(task, logFolder);
	}

	public static VirtualExcuter getVirtualExcuter(Task task, String logFolder) {
		return new VirtualExcuter(task, logFolder);
	}

	public static BranchExcuter getBranchExcuter(Task task, String logFolder) {
		return new BranchExcuter(task, logFolder);
	}

	public static GreenplumExcuter getGreenplumExcuter(Task task, String logFolder) {
		return new GreenplumExcuter(task, logFolder);
	}

	public static PutHdfsExcuter getPutHdfsExcuter(Task task, String logFolder) {
		return new PutHdfsExcuter(task, logFolder);
	}

	public static FileNumberCheckExcuter getFileNumberCheckExcuter(Task task, String logFolder) {
		return new FileNumberCheckExcuter(task, logFolder);
	}
	
	/*
	 * @modify by qyx 加入新的datax
	 */
	public static NewDataxExcuter getNewDataxExcuter(Task task,String logFolder){
		return new NewDataxExcuter(task,logFolder);
	}

	public static AbExcuter getExcuterByJobType(Task task, String logFolder) {
		Job job = jobService.get(task.getJobId());
		long jobType = job.getJobType();
		System.out.println("Parameters.NewDataxState :"+Arrays.binarySearch(Parameters.NewDataxState, jobType));
		System.out.println("jobType :"+jobType);
		//  Arrays.binarySearch用法 使用二进制搜索算法来搜索指定的 int 型数组，以获得指定的值。必须在进行此调用之前对数组进行排序
		if (Arrays.binarySearch(Parameters.DataxState, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Datax");
			return getDataxExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.HiveSqlState, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:HiveSql");
			return getHiveSqlExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.MapReduceState, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:MapReduce");
			return getMapReduceExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.ShellState, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Shell");
			return getShellExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.FtpState, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Ftp");
			return getFtpExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.ProcState, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Proc");
			return getProcedureExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.MailJob, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:MailJob");
			return getMailJobExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.ReportQuality, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:ReportQuality");
			return getReportQualityExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.CheckDependency, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:CheckDependency");
			return getCheckDependencyExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.VirtualState, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Virtual");
			return getVirtualExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.Branch, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Branch");
			return getBranchExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.Greenplum, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Greenplum");
			return getGreenplumExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.Put2Hdfs, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:Put2Hdfs");
			return getPutHdfsExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.FileNumberCheck, jobType) > -1) {
			System.out.println(task.getJobName() + ",当前任务的类型:FileNumberCheck");
			return getFileNumberCheckExcuter(task, logFolder);
		} else if(Arrays.binarySearch(Parameters.NewDataxState, jobType) > -1){
			//@modify by qyx 加入新的datax任务 
			System.out.println(task.getJobName()+",当前任务的类型:NewDatax");
			return getNewDataxExcuter(task, logFolder);
		}
       
		System.out.println("当前任务没有匹配到任何任务执行器!" + "task name is " + task.getJobName() + "Job type is " + jobType);
		return null;
	}

	//   如果是天任务依赖小时任务.  那么小时任务的业务日期是20号,任务日期是20号.    天任务的业务日期则是20号,任务日期是21号.
	//   所以要增加一个检验器, 检查前一天的小时父任务的执行情况,如果都是成功或者重做成功,则检验器执行成功,否则执行失败.  每天运行.
	//   同样,对于月任务依赖天任务,也要有一个对应的检验器.  每月运行.

}
