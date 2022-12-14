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
	 * @modify by qyx ????????????datax
	 */
	public static NewDataxExcuter getNewDataxExcuter(Task task,String logFolder){
		return new NewDataxExcuter(task,logFolder);
	}

	public static AbExcuter getExcuterByJobType(Task task, String logFolder) {
		Job job = jobService.get(task.getJobId());
		long jobType = job.getJobType();
		System.out.println("Parameters.NewDataxState :"+Arrays.binarySearch(Parameters.NewDataxState, jobType));
		System.out.println("jobType :"+jobType);
		//  Arrays.binarySearch?????? ????????????????????????????????????????????? int ???????????????????????????????????????????????????????????????????????????????????????
		if (Arrays.binarySearch(Parameters.DataxState, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Datax");
			return getDataxExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.HiveSqlState, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:HiveSql");
			return getHiveSqlExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.MapReduceState, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:MapReduce");
			return getMapReduceExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.ShellState, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Shell");
			return getShellExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.FtpState, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Ftp");
			return getFtpExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.ProcState, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Proc");
			return getProcedureExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.MailJob, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:MailJob");
			return getMailJobExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.ReportQuality, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:ReportQuality");
			return getReportQualityExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.CheckDependency, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:CheckDependency");
			return getCheckDependencyExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.VirtualState, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Virtual");
			return getVirtualExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.Branch, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Branch");
			return getBranchExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.Greenplum, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Greenplum");
			return getGreenplumExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.Put2Hdfs, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:Put2Hdfs");
			return getPutHdfsExcuter(task, logFolder);
		} else if (Arrays.binarySearch(Parameters.FileNumberCheck, jobType) > -1) {
			System.out.println(task.getJobName() + ",?????????????????????:FileNumberCheck");
			return getFileNumberCheckExcuter(task, logFolder);
		} else if(Arrays.binarySearch(Parameters.NewDataxState, jobType) > -1){
			//@modify by qyx ????????????datax?????? 
			System.out.println(task.getJobName()+",?????????????????????:NewDatax");
			return getNewDataxExcuter(task, logFolder);
		}
       
		System.out.println("????????????????????????????????????????????????!" + "task name is " + task.getJobName() + "Job type is " + jobType);
		return null;
	}

	//   ????????????????????????????????????.  ????????????????????????????????????20???,???????????????20???.    ??????????????????????????????20???,???????????????21???.
	//   ??????????????????????????????, ????????????????????????????????????????????????,????????????????????????????????????,????????????????????????,??????????????????.  ????????????.
	//   ??????,??????????????????????????????,?????????????????????????????????.  ????????????.

}
