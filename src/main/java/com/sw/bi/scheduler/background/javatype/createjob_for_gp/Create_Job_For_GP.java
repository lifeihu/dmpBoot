package com.sw.bi.scheduler.background.javatype.createjob_for_gp;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.JobRelation;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.service.JobRelationService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.util.Configure.JobType;



// lifeng 2013-08-19
/*
 * 
select aa.job_id,aa.n from (
select job_id,count(*) as n from job_relation where job_id in (
select job_id from job where job_status=1 and job_type in (31,71))
group by job_id) aa
where aa.n>1

要处理的
select group_concat(aa.job_id) from (
select job_id,count(*) as n from job_relation where job_id in (
select job_id from job where job_status=1 and job_type in (31,71))
group by job_id) aa
where aa.n=1
*
*/

//   /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.createjob_for_gp.Create_Job_For_GP
@Component
public class Create_Job_For_GP {
	
	    private static final Logger log = Logger.getLogger(Create_Job_For_GP.class);
	    
		@Autowired
		private JobService jobService;
		
		@Autowired
		private JobRelationService jobRelationService;
		
		@Autowired
		private JobDatasyncConfigService jobDatasyncConfigService;
		
		
		@Autowired
		private DatasourceService datasourceService;
		
		
		
	
	    public static void main(String args[]) throws IOException{
	    	Create_Job_For_GP.getCreate_Job_For_GP().Create_Job_For_GP();
	    	System.out.println("OK");
	    }
	     
	 	private static Create_Job_For_GP getCreate_Job_For_GP() {
			return BeanFactory.getBean(Create_Job_For_GP.class);
		}
	 	

	 	private void Create_Job_For_GP() throws IOException{
	 		String jobids = "41,43,44,45,46,47,48,49,50,51,52,53,54,80,82,83,98,99,100,101,111,127,128,133,134,135,136,137,138,160,187,188,189,190,196,197,198,199,200,201,206,207,208,218,231,232,242,243,261,262,263,287,288,289,293,379,380,381,382,383,384,385,386,387,388,390,391,393,394,395,396,397,399,402,404,405,406,407,408,409,410,412,413,414,421,422,423,424,426,450,476,477,478,479,480,481,485,486,487,488,489,490,491,492,493,499,500,503,512,515,516,518,519,520,521,523,524,525,526,528,530,531,533,534,541,542,543,551,552,553,557,558,559,564,566,567,568,578,579,626,627,628,629,630,631,632,633,637,642,643,645,647,654,660,664,693,694,695,697,740,757,766,767,768,769,770,771,1178,1180,1188,1212,1213,1214,1215";
	 		String[] jobList = jobids.split(",");
	 		for(String job_id_s:jobList){
		 		long job_id = Long.valueOf(job_id_s);
		 		Job job = jobService.get(job_id);
		 		JobRelation jobRelation= jobRelationService.getJobRelationByJob(job_id);
		 	    JobDatasyncConfig jobDatasyncConfig = jobDatasyncConfigService.getJobDatasyncConfigByJob(job_id);
		 				
		 		Job new_job = new Job();
		 		new_job.setAlert(job.getAlert());
		 		new_job.setCreateTime(job.getCreateTime());
		 		new_job.setCycleType(job.getCycleType());
		 		new_job.setDayN(job.getDayN());
		 		new_job.setDownMan(job.getDownMan());
		 		new_job.setDownReason(job.getDownReason());
		 		new_job.setDownTime(job.getDownTime());
		 		new_job.setDutyOfficer(job.getDutyOfficer());
		 		new_job.setFailureRerunInterval(job.getFailureRerunInterval());
		 		new_job.setFailureRerunTimes(job.getFailureRerunTimes());
		 		new_job.setGateway(job.getGateway());
		 		new_job.setJobBusinessGroup(job.getJobBusinessGroup());
		 		new_job.setJobDesc(job.getJobDesc());
		 		new_job.setJobLevel(job.getJobLevel());
		 		new_job.setJobName("GP_"+job.getJobName()); //1. 名称特殊处理
		 		new_job.setJobStatus(0l);  //2. 状态都是未上线
		 		new_job.setJobTime(job.getJobTime());
		 		
		 		//3. job_type
		 		if(job.getJobType()==JobType.SQLSERVER_TO_MYSQL.indexOf()){
		 			new_job.setJobType(JobType.SQLSERVER_TO_GP.indexOf());
		 		}else if(job.getJobType()==JobType.HDFS_TO_MYSQL.indexOf()){
		 			new_job.setJobType(JobType.HDFS_TO_GP.indexOf());
		 		}else{
		 			return;
		 		}
		 		
		 		new_job.setParameters(job.getParameters());
		 		new_job.setPrevJobs(job.getPrevJobs());
		 		new_job.setProgramPath(job.getProgramPath());
		 		new_job.setTasks(job.getTasks());
		 		new_job.setUpdateBy(job.getUpdateBy());
		 		new_job.setUpdateTime(job.getUpdateTime());
		 		jobService.save(new_job);
		 		
		 		long new_job_id = new_job.getJobId();
		 		
		 		JobRelation new_JobRelation = new JobRelation();
		 		new_JobRelation.setCreateTime(jobRelation.getCreateTime());
		 		new_JobRelation.setJobId(new_job_id); //1. 新jobid
		 		new_JobRelation.setParentId(jobRelation.getParentId());
		 		new_JobRelation.setUpdateTime(jobRelation.getUpdateTime());
		 		jobRelationService.save(new_JobRelation);
		 		
		 		
		 		Datasource gp_datasource = datasourceService.get(20l); // 暂时写死
		 		
		 		JobDatasyncConfig new_jobDatasyncConfig = new JobDatasyncConfig();
		 		new_jobDatasyncConfig.setCheckSeconds(jobDatasyncConfig.getCheckSeconds());
		 		new_jobDatasyncConfig.setCreateTableSql(jobDatasyncConfig.getCreateTableSql());
		 		new_jobDatasyncConfig.setCreateTime(jobDatasyncConfig.getCreateTime());
		 		new_jobDatasyncConfig.setDatasourceByFinalyDatasourceId(gp_datasource); //gp_datasource
		 		new_jobDatasyncConfig.setDatasourceByFtpDatasourceId(jobDatasyncConfig.getDatasourceByFtpDatasourceId());
		 		new_jobDatasyncConfig.setDatasourceByInitDatasourceId(gp_datasource); //gp_datasource
		 		new_jobDatasyncConfig.setDatasourceBySourceDatasourceId(jobDatasyncConfig.getDatasourceBySourceDatasourceId());
		 		new_jobDatasyncConfig.setDatasourceByTargetDatasourceId(gp_datasource); //gp_datasource
		 		new_jobDatasyncConfig.setDateTimePosition(jobDatasyncConfig.getDateTimePosition());
		 		new_jobDatasyncConfig.setErrorthreshold(jobDatasyncConfig.getErrorthreshold());
		 		new_jobDatasyncConfig.setFileNumber(jobDatasyncConfig.getFileNumber());
		 		new_jobDatasyncConfig.setFileUniquePattern(jobDatasyncConfig.getFileUniquePattern());
		 		

		 		new_jobDatasyncConfig.setFtpBakDir(jobDatasyncConfig.getFtpBakDir());
		 		new_jobDatasyncConfig.setFtpDir(jobDatasyncConfig.getFtpDir());
		 		new_jobDatasyncConfig.setFtpErrDir(jobDatasyncConfig.getFtpErrDir());
		 		new_jobDatasyncConfig.setHdfsPath(jobDatasyncConfig.getHdfsPath());
		 		new_jobDatasyncConfig.setHiveFields(jobDatasyncConfig.getHiveFields());
		 		new_jobDatasyncConfig.setHiveTableName(jobDatasyncConfig.getHiveTableName());

		 		new_jobDatasyncConfig.setJobId(new_job_id);  //  1. jobid用新的
		 		//new_jobDatasyncConfig.setJobType(jobType); //  2. 作业类型要变更
		 		if(job.getJobType()==JobType.SQLSERVER_TO_MYSQL.indexOf()){
		 			new_jobDatasyncConfig.setJobType(Long.valueOf(JobType.SQLSERVER_TO_GP.indexOf()));
		 		}else if(job.getJobType()==JobType.HDFS_TO_MYSQL.indexOf()){
		 			new_jobDatasyncConfig.setJobType(Long.valueOf(JobType.HDFS_TO_GP.indexOf()));
		 		}else{
		 			return;
		 		}
		 		
		 		
		 		new_jobDatasyncConfig.setLinuxBakDir(jobDatasyncConfig.getLinuxBakDir());
		 		new_jobDatasyncConfig.setLinuxErrDir(jobDatasyncConfig.getLinuxErrDir());
		 		new_jobDatasyncConfig.setLinuxTmpDir(jobDatasyncConfig.getLinuxTmpDir());
		 		new_jobDatasyncConfig.setReferDbName(jobDatasyncConfig.getReferDbName());
		 		new_jobDatasyncConfig.setReferPartName(jobDatasyncConfig.getReferPartName());
		 		new_jobDatasyncConfig.setReferTableName(jobDatasyncConfig.getReferTableName());
		 		new_jobDatasyncConfig.setSourceCharset(jobDatasyncConfig.getSourceCharset());
		 		new_jobDatasyncConfig.setSourceCodec(jobDatasyncConfig.getSourceCodec());
		 		new_jobDatasyncConfig.setSourceColumns(jobDatasyncConfig.getSourceColumns());
		 		new_jobDatasyncConfig.setSourceCommitThreshold(jobDatasyncConfig.getSourceCommitThreshold());
		 		new_jobDatasyncConfig.setSourceDatapath(jobDatasyncConfig.getSourceDatapath());
		 		new_jobDatasyncConfig.setSourceDelimiter(jobDatasyncConfig.getSourceDelimiter());
		 		new_jobDatasyncConfig.setSourceFileType(jobDatasyncConfig.getSourceFileType());
		 		new_jobDatasyncConfig.setSuccessFlag(jobDatasyncConfig.getSuccessFlag());
		 		new_jobDatasyncConfig.setTargetCharset(jobDatasyncConfig.getTargetCharset());
		 		new_jobDatasyncConfig.setTargetCodec(jobDatasyncConfig.getTargetCharset());
		 		new_jobDatasyncConfig.setTargetColumns(jobDatasyncConfig.getTargetColumns());
		 		new_jobDatasyncConfig.setTargetCommitThreshold(jobDatasyncConfig.getTargetCommitThreshold());
		 		new_jobDatasyncConfig.setTargetDelimiter(jobDatasyncConfig.getTargetDelimiter());
		 		new_jobDatasyncConfig.setTargetFileType(jobDatasyncConfig.getTargetFileType());
		 		new_jobDatasyncConfig.setThreadNumber(jobDatasyncConfig.getThreadNumber());
		 		new_jobDatasyncConfig.setTimeoutMinutes(jobDatasyncConfig.getTimeoutMinutes());
		 		new_jobDatasyncConfig.setUpdateTime(jobDatasyncConfig.getUpdateTime());
		 		new_jobDatasyncConfig.setUserXml(jobDatasyncConfig.getUserXml());
		 		new_jobDatasyncConfig.setXmlTemplate(jobDatasyncConfig.getXmlTemplate());
		 		
		 		// delete from dmn_aplus_menu where 1=1
		 		// 表命名规则如下:
		 		// 对于表前缀为balance_，dmn_ ,dwd_ 开头的,都属于维度表,放置在dmn模式下.引用示例为: dmn.dmn_*
		 		// 对于表前缀为cub_ , data_ , rp_ , rpd_ ,rpt_ 开头的属于报表和cub同步数据,放置在 report模式下,引用示例为:report.cub_*

		 		String init_sql=jobDatasyncConfig.getInitSql();
		 		if(init_sql!=null&&init_sql.length()>0){
		 			if(init_sql.indexOf(" dmn_")>=0){
		 				init_sql=init_sql.replaceFirst(" dmn_", " dmn.dmn_");
		 			}

		 			if(init_sql.indexOf(" dwd_")>=0){
		 				init_sql=init_sql.replaceFirst(" dwd_", " dmn.dwd_");
		 			}
		 			
		 			if(init_sql.indexOf(" balance_")>=0){
		 				init_sql=init_sql.replaceFirst(" balance_", " report.balance_");
		 			}
		 			
		 			if(init_sql.indexOf(" cub_")>=0){
		 				init_sql=init_sql.replaceFirst(" cub_", " report.cub_");
		 			}
		 			if(init_sql.indexOf(" data_")>=0){
		 				init_sql=init_sql.replaceFirst(" data_", " report.data_");
		 			}
		 			if(init_sql.indexOf(" rp_")>=0){
		 				init_sql=init_sql.replaceFirst(" rp_", " report.rp_");
		 			}
		 			if(init_sql.indexOf(" rpd_")>=0){
		 				init_sql=init_sql.replaceFirst(" rpd_", " report.rpd_");
		 			}
		 			if(init_sql.indexOf(" rpt_")>=0){
		 				init_sql=init_sql.replaceFirst(" rpt_", " report.rpt_");
		 			}
		 		}
		 		new_jobDatasyncConfig.setInitSql(init_sql);
		 		
		 		String finaly_success_sql = jobDatasyncConfig.getFinalySuccessSql();
		 		if(finaly_success_sql!=null&&finaly_success_sql.length()>0){
		 			if(finaly_success_sql.indexOf(" dmn_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" dmn_", " dmn.dmn_");
		 			}

		 			if(finaly_success_sql.indexOf(" dwd_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" dwd_", " dmn.dwd_");
		 			}
		 			
		 			
		 			if(finaly_success_sql.indexOf(" balance_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" balance_", " report.balance_");
		 			}
		 			
		 			if(finaly_success_sql.indexOf(" cub_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" cub_", " report.cub_");
		 			}
		 			if(finaly_success_sql.indexOf(" data_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" data_", " report.data_");
		 			}
		 			if(finaly_success_sql.indexOf(" rp_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" rp_", " report.rp_");
		 			}
		 			if(finaly_success_sql.indexOf(" rpd_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" rpd_", " report.rpd_");
		 			}
		 			if(finaly_success_sql.indexOf(" rpt_")>=0){
		 				finaly_success_sql=finaly_success_sql.replaceFirst(" rpt_", " report.rpt_");
		 			}
		 		}
		 		new_jobDatasyncConfig.setFinalySuccessSql(finaly_success_sql);
		 		
		 		
		 		String finaly_failsql = jobDatasyncConfig.getFinalyFailSql();
		 		if(finaly_failsql!=null&&finaly_failsql.length()>0){
		 			if(finaly_failsql.indexOf(" dmn_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" dmn_", " dmn.dmn_");
		 			}

		 			if(finaly_failsql.indexOf(" dwd_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" dwd_", " dmn.dwd_");
		 			}
		 			
		 			
		 			if(finaly_failsql.indexOf(" balance_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" balance_", " report.balance_");
		 			}
		 			
		 			if(finaly_failsql.indexOf(" cub_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" cub_", " report.cub_");
		 			}
		 			if(finaly_failsql.indexOf(" data_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" data_", " report.data_");
		 			}
		 			if(finaly_failsql.indexOf(" rp_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" rp_", " report.rp_");
		 			}
		 			if(finaly_failsql.indexOf(" rpd_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" rpd_", " report.rpd_");
		 			}
		 			if(finaly_failsql.indexOf(" rpt_")>=0){
		 				finaly_failsql=finaly_failsql.replaceFirst(" rpt_", " report.rpt_");
		 			}
		 		}
		 		new_jobDatasyncConfig.setFinalyFailSql(finaly_failsql);
		 		
		 		
	/*			 String a = "insert into rp_hi_flow_client_nvidia_m (day_id,";
				 int index1 = a.indexOf("(");
				 int index2 = a.indexOf("insert into ");
				 String b = a.substring(index2+12, index1).trim();
				 System.out.println(b);*/
		 		String target_datapath = jobDatasyncConfig.getTargetDatapath().trim();
		 		if(target_datapath!=null&&target_datapath.length()>0){
		 			int index1 = target_datapath.indexOf("(");
		 			int index2 = target_datapath.indexOf("insert into ");
		 			target_datapath = target_datapath.substring(index2+12, index1).trim();
		 			
		 			if(target_datapath.indexOf(" dmn_")>=0){
		 				target_datapath = target_datapath.replaceFirst("dmn_", "dmn.dmn_");
		 			}

		 			if(target_datapath.indexOf(" dwd_")>=0){
		 				target_datapath = target_datapath.replaceFirst("dwd_", "dmn.dwd_");
		 			}
		 			
		 			
		 			if(target_datapath.indexOf(" balance_")>=0){
		 				target_datapath = target_datapath.replaceFirst("balance_", "report.balance_");
		 			}
		 			
		 			if(target_datapath.indexOf(" cub_")>=0){
		 				target_datapath = target_datapath.replaceFirst("cub_", "report.cub_");
		 			}
		 			if(target_datapath.indexOf(" data_")>=0){
		 				target_datapath = target_datapath.replaceFirst("data_", "report.data_");
		 			}
		 			if(target_datapath.indexOf(" rp_")>=0){
		 				target_datapath = target_datapath.replaceFirst("rp_", "report.rp_");
		 			}
		 			if(target_datapath.indexOf(" rpd_")>=0){
		 				target_datapath = target_datapath.replaceFirst("rpd_", "report.rpd_");
		 			}
		 			if(target_datapath.indexOf(" rpt_")>=0){
		 				target_datapath = target_datapath.replaceFirst("rpt_", "report.rpt_");
		 			}
		 			
		 		}
		 		new_jobDatasyncConfig.setTargetDatapath(target_datapath);
		 		
		 		
		 		jobDatasyncConfigService.save(new_jobDatasyncConfig);
	 		}
	 	} 
}
