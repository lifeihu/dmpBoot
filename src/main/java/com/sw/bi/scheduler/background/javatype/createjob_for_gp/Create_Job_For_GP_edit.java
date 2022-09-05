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

//   /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.createjob_for_gp.Create_Job_For_GP_edit
@Component
public class Create_Job_For_GP_edit {
	
	    private static final Logger log = Logger.getLogger(Create_Job_For_GP_edit.class);
	    
		@Autowired
		private JobService jobService;
		
		@Autowired
		private JobRelationService jobRelationService;
		
		@Autowired
		private JobDatasyncConfigService jobDatasyncConfigService;
		
		
		@Autowired
		private DatasourceService datasourceService;
		
		
		
	
	    public static void main(String args[]) throws IOException{
	    	Create_Job_For_GP_edit.getCreate_Job_For_GP().Create_Job_For_GP();
	    	System.out.println("OK");
	    }
	     
	 	private static Create_Job_For_GP_edit getCreate_Job_For_GP() {
			return BeanFactory.getBean(Create_Job_For_GP_edit.class);
		}
	 	
//修正方法
	 	private void Create_Job_For_GP() throws IOException{
	 		String jobids = "1230,1231,1232,1233,1234,1235,1236,1237,1238,1239,1240,1241,1242,1243,1244,1245,1246,1247,1248,1249,1250,1251,1252,1253,1254,1255,1256,1257,1258,1259,1260,1261,1262,1263,1264,1265,1266,1267,1268,1269,1270,1271,1272,1273,1274,1275,1276,1277,1278,1279,1280,1281,1282,1283,1284,1285,1286,1287,1288,1289,1290,1291,1292,1293,1294,1295,1296,1297,1298,1299,1300,1301,1302,1303,1304,1305,1306,1307,1308,1309,1310,1311,1312,1313,1314,1315,1316,1317,1318,1319,1320,1321,1322,1323,1324,1325,1326,1327,1328,1329,1330,1331,1332,1333,1334,1335,1336,1337,1338,1339,1340,1341,1342,1343,1344,1345,1346,1347,1348,1349,1350,1351,1352,1353,1354,1355,1356,1357,1358,1359,1360,1361,1362,1363,1364,1365,1366,1367,1368,1369,1370,1371,1372,1373,1374,1375,1376,1377,1378,1379,1380,1381,1382,1383,1384,1385,1386,1387,1388,1389,1390,1391,1392,1393,1394,1395,1396,1397,1398,1399,1400,1401,1402,1403";
	 		String[] jobList = jobids.split(",");
	 		for(String job_id_s:jobList){
		 		long job_id = Long.valueOf(job_id_s);
	 			JobDatasyncConfig jobDatasyncConfig = jobDatasyncConfigService.getJobDatasyncConfigByJob(job_id);
		 		
		 		
		 		String target_datapath = jobDatasyncConfig.getTargetDatapath().trim();
		 		System.out.println("now: "+target_datapath);
		 		if(target_datapath!=null&&target_datapath.length()>0){
		 			
		 			if(target_datapath.indexOf("dmn_")>=0){
		 				target_datapath = target_datapath.replaceFirst("dmn_", "dmn.dmn_");
		 			}

		 			if(target_datapath.indexOf("dwd_")>=0){
		 				target_datapath = target_datapath.replaceFirst("dwd_", "dmn.dwd_");
		 			}
		 			
		 			
		 			if(target_datapath.indexOf("balance_")>=0){
		 				target_datapath = target_datapath.replaceFirst("balance_", "report.balance_");
		 			}
		 			
		 			if(target_datapath.indexOf("cub_")>=0){
		 				target_datapath = target_datapath.replaceFirst("cub_", "report.cub_");
		 			}
		 			if(target_datapath.indexOf("data_")>=0){
		 				target_datapath = target_datapath.replaceFirst("data_", "report.data_");
		 			}
		 			if(target_datapath.indexOf("rp_")>=0){
		 				target_datapath = target_datapath.replaceFirst("rp_", "report.rp_");
		 			}
		 			if(target_datapath.indexOf("rpd_")>=0){
		 				target_datapath = target_datapath.replaceFirst("rpd_", "report.rpd_");
		 			}
		 			if(target_datapath.indexOf("rpt_")>=0){
		 				target_datapath = target_datapath.replaceFirst("rpt_", "report.rpt_");
		 			}
		 			
		 		}
		 		jobDatasyncConfig.setTargetDatapath(target_datapath);
		 		
		 		
		 		jobDatasyncConfigService.saveOrUpdate(jobDatasyncConfig);
	 		}
	 	} 
}
