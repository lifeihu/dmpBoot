package com.sw.bi.scheduler.background.mytest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.WaitUpdateStatusTaskService;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.DateUtil;

//   /home/tools/scheduler/scheduler_test jar   /home/tools/scheduler/scheduler_test.jar com.sw.bi.scheduler.background.mytest.SupplyGP
@Component
public class SupplyGP {

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	
	@Autowired
	private JobService jobService;
	
	
	@Autowired
	private TaskService taskService;

	public static void main(String args[]) {
		 

		
		
		
		SupplyGP.getRecoverReferDB().supplyRefers();
	}

	public static SupplyGP getRecoverReferDB() {
		return BeanFactory.getBean(SupplyGP.class);
	}

 

	//  对某一批作业ID,指定日期范围内,批量补数据操作。 并行处理。
	//  先把对应的任务记录删除，然后生成。  生成的时候，已经考虑了月和周任务的特殊性
	public void supplyRefers() {
		int[] jobids = {1230,1231,1232,1233,1234,1235,1236,1237,1238,1239,1240,1241,1242,1243,1244,1245,1246,1247,1248,1249,1250,1251,1252,1253,1254,1255,1256,1257,1258,1259,1260,1261,1262,1263,1264,1265,1266,1267,1268,1269,1270,1271,1272,1273,1274,1275,1276,1277,1278,1279,1280,1281,1282,1283,1284,1285,1286,1287,1288,1289,1290,1291,1292,1293,1294,1295,1296,1300,1301,1302,1303,1304,1305,1306,1307,1308,1309,1310,1311,1312,1313,1314,1315,1316,1318,1319,1320,1321,1322,1323,1324,1325,1326,1327,1328,1329,1330,1331,1332,1333,1334,1335,1336,1337,1341,1342,1343,1344,1345,1346,1347,1348,1349,1350,1351,1352,1353,1354,1355,1356,1357,1358,1359,1360,1361,1362,1363,1364,1365,1366,1367,1368,1369,1370,1371,1372,1373,1374,1375,1376,1377,1378,1379,1380,1381,1382,1383,1385,1386,1387,1388,1389,1391,1392,1393,1394,1395,1396,1400,1401,1402,1403};
		String[] supply_dates = {"2013-08-29","2013-08-30","2013-08-31","2013-09-01"};
		
		for(int jobid : jobids){
			for(String supply_date : supply_dates){
				taskService.delete(taskService.getTasksByJob(Long.valueOf(jobid).longValue(), DateUtil.parse(supply_date, "yyyy-MM-dd")));

				jobService.createTasks(jobService.get(Long.valueOf(jobid).longValue()), DateUtil.parse(supply_date, "yyyy-MM-dd"), TaskFlag.REDO.indexOf());
				
			}
		}
	}

}
