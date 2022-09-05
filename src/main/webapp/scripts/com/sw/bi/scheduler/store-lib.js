S.CONFIG = {
	'yesNo': {
		data: [
			[true, '是'],
			[false, '否']
		]
	},
	'ways': {
		data: [
			[0, '通用配置'],
			[1, '自定义单文件配置'],
			[2, '自定义多文件配置']
		]
	},
	'fileFormat': {
		data: [
			[0, 'text'],
			[1, 'csv']
		]
	},
	'resourceType': {
		data: [
			[0, 'Module'],
			[1, "URL"]
		]
	},
	
	'sevenDays': {
		data: [
			[new Date(), new Date().format('Y-m-d')],
			[new Date().add(Date.DAY, -1), new Date().add(Date.DAY, -1).format('Y-m-d')],
			[new Date().add(Date.DAY, -2), new Date().add(Date.DAY, -2).format('Y-m-d')],
			[new Date().add(Date.DAY, -3), new Date().add(Date.DAY, -3).format('Y-m-d')],
			[new Date().add(Date.DAY, -4), new Date().add(Date.DAY, -4).format('Y-m-d')],
			[new Date().add(Date.DAY, -5), new Date().add(Date.DAY, -5).format('Y-m-d')],
			[new Date().add(Date.DAY, -6), new Date().add(Date.DAY, -6).format('Y-m-d')]
		]
	},
	
	'days': {
		data: [
			[1, 1], [2, 2], [3, 3], [4, 4], [5, 5], [6, 6], [7, 7], [8, 8], [9, 9],
			[10, 10], [11, 11], [12, 12], [13, 13], [14, 14], [15, 15], [16, 16], [17, 17], [18, 18], [19, 19],
			[20, 20], [21, 21], [22, 22], [23, 23], [24, 24], [25, 25], [26, 26], [27, 27], [28, 28]
		]
	},
	
	'weeks': {
		data: [
			[2, '周一'],
			[3, '周二'],
			[4, '周三'],
			[5, '周四'],
			[6, '周五'],
			[7, '周六'],
			[1, '周日']
		]
	},
	
	'hours': {
		data: [
			['00', 0], ['01', 1], ['02', 2], ['03', 3], ['04', 4], ['05', 5], ['06', 6], ['07', 7], ['08', 8], ['09', 9],
			['10', 10], ['11', 11], ['12', 12], ['13', 13], ['14', 14], ['15', 15], ['16', 16], ['17', 17], ['18', 18], ['19', 19],
			['20', 20], ['21', 21], ['22', 22], ['23', 23]
		]
	},
	
	'minutes': {
		data: [
			['01', 1], ['02', 2], ['03', 3], ['04', 4], ['05', 5], ['06', 6], ['07', 7], ['08', 8], ['09', 9],
			['10', 10], ['11', 11], ['12', 12], ['13', 13], ['14', 14], ['15', 15], ['16', 16], ['17', 17], ['18', 18], ['19', 19],
			['20', 20], ['21', 21], ['22', 22], ['23', 23], ['24', 24], ['25', 25], ['26', 26], ['27', 27], ['28', 28], ['29', 29],
			['30', 30], ['31', 31], ['32', 32], ['33', 33], ['34', 34], ['35', 35], ['36', 36], ['37', 37], ['38', 38], ['39', 39],
			['40', 40], ['41', 41], ['42', 42], ['43', 43], ['44', 44], ['45', 45], ['46', 46], ['47', 47], ['48', 48], ['49', 49],
			['50', 50], ['51', 51], ['52', 52], ['53', 53], ['54', 54], ['55', 55], ['56', 56], ['57', 57], ['58', 58], ['59', 59]
		]
	},
	
	'userStatus': {
		data: [
			[0, '正常'],
			[1, '删除']
		]
	},
	
	'userType': {
		data: [
			[0, '本部门用户'],
			[1, '其他部门用户']
		]
	},
	
	'datasourceType': {
		data: [
			[0, 'MySQL'],
			[1, 'SQLServer'],
			[2, 'Oracle'],
			[3, 'FTP'],
			[7, 'Greenplum'],
			[8, 'HBase'],
			//add by zhoushasha
			 [9,'MongoDb'],
			 [10,'SunDb']
		]
	},
	
	'datasourceViewType': {
		data: [
			[0, '所有用户可见'],
			[1, '仅创建用户可见']
		]
	},
	
	'charset': {
		data: [
			['UTF-8', 'UTF-8'],
			['GBK', 'GBK']
		]
	},
	
	'jobStatus': {
		data: [
			[0, '未上线'],
			[1, '已上线'],
			[2, '已下线']
		]
	},
	
	'jobType': {
		data: [
			[20, 'HiveSQL计算'],
			[21, 'Mapreduce计算'],
			
			[40, 'Shell脚本'],
			// [41, 'Perl脚本'],
			[42, '存储过程'],
            [90, '邮件发送作业'],
            [91, '报表质量监控'],
            
            [92, '天依赖小时'],
            [93, '月依赖天'],
			
			[100, '虚拟作业'],
			[101, '使用自定义XML配置文件'],
			[102, '分支作业'],
			[103, '使用自定义JSON配置文件'],
			[104, '使用shell调用python'],
			
			[6, 'FTP文件同步到本地(间隔n分钟)'],
			[5, 'FTP文件同步到本地(需要成功标记)'],
			[7, 'FTP文件同步到本地(不需要成功标记)'],
			
			[9, 'HDFS作业'],
			[8, 'Greenplum函数'],
			[10, '文件数量校验'],
			
			[30, 'HDFS导出到Oracle'],
			[34, 'HDFS导出到HDFS'],
			[2, 'MySQL同步到HDFS'],
			[4, '本地文件同步到HDFS'],
			[3, 'SQLServer同步到HDFS'],
			[116, 'Greenplum导出到HDFS'],
			[126, 'CSV导出到HDFS'],
			
			[31, 'HDFS导出到MySQL'],
			[61, 'MySQL导出到MySQL'],
			[51, '本地文件导出到MySQL'],
			[71, 'SQLServer导出到MySQL'],
			[111, 'Greenplum导出到MySQL'],
			[121, 'CSV导出到MySQL'],
			
			[32, 'HDFS导出到SQLServer'],
			[62, 'MySQL导出到SQLServer'],
			[52, '本地文件导出到SQLServer'],
			[72, 'SQLServer导出到SQLServer'],
			[112, 'Greenplum导出到SQLServer'],
			[122, 'CSV导出到SQLServer'],
			
			[33, 'HDFS导出到本地文件'],
			[63, 'MySQL导出到本地文件'],
			[53, '本地文件导出到本地文件'],
			[73, 'SQLServer导出到本地文件'],
			[113, 'Greenplum导出到本地文件'],
			[123, 'CSV导出到本地文件'],
			[35, 'HDFS导出到CSV'],
			[64, 'MySQL导出到CSV'],
			[54, '本地文件导出到CSV'],
			[74, 'SQLServer导出到CSV'],
			[114, 'Greenplum导出到CSV'],
			[124, 'CSV导出到CSV'],
			[36, 'HDFS导出到Greenplum'],
			[65, 'MySQL导出到Greenplum'],
			[55, '本地文件导出到Greenplum'],
			[75, 'SQLServer导出到Greenplum'],
			[115, 'Greenplum导出到Greenplumn'],
			[125, 'CSV导出到Greenplumn'],
			[60, 'MySQL导出到Oracle'],
			[50, '本地文件导出到Oracle'],
			[70, 'SQLServer导出到Oracle'],
			[1, 'Oracle同步到HDFS'],
			[80, 'Oracle导出到Oracle'],
			[81, 'Oracle导出到MySQL'],
			[82, 'Oracle导出到SQLServer'],
			[83, 'Oracle导出到本地文件'],
			[85, 'Oracle导出到Greenplum'],
			
			// add by zhuzhongji 2015年9月11日09:32:54
			[66, 'MySQL导出到FTP'],
			[67, 'MySQL导出到HBase'],
			[76, 'SQLServer导出到FTP'],
			[77, 'SQLServer导出到HBase'],
			[86, 'Oracle导出到FTP'],
			[87, 'Oracle导出到HBase'],
			[37, 'HDFS导出到FTP'],
			[38, 'HDFS导出到HBase'],
			[56, '本地文件导出到FTP'],
			[57, '本地文件导出到HBase'],
			[127, 'CSV导出到FTP'],
			[128, 'CSV导出到HBase'],
			[117, 'Greenplum导出到FTP'],
			[118, 'Greenplum导出到HBase'],

			[131, 'FTP文件同步到MySQL'],
			[132, 'FTP文件同步到SQLServer'],
			[130, 'FTP文件同步到Oracle'],
			[138, 'FTP文件同步到HDFS'],
			[133, 'FTP文件同步到本地文件'],
			[134, 'FTP文件同步到CSV'],
			[135, 'FTP文件同步到Greenplum'],
			[136, 'FTP文件同步到FTP'],
			[137, 'FTP文件同步到HBase'],
			
			[141, 'HBase导出到MySQL'],
			[142, 'HBase导出到SQLServer'],
			[140, 'HBase导出到Oracle'],
			[146, 'HBase导出到HDFS'],
			[143, 'HBase导出到本地文件'],
			[144, 'HBase导出到CSV'],
			[145, 'HBase导出到Greenplum'],
			[147, 'HBase导出到FTP'],
			[148, 'HBase导出到HBase'],
			
			//add by zhoushasha
			[151,'MongoDb导出到MySQL'],
			[152,'MongoDb导出到SQLServer'],
			[150,'MongoDb导出到Oracle'],
			[156,'MongoDb导出到HDFS'],
			[153,'MongoDb导出到本地文件'],
			[154,'MongoDb导出到CSV'],
			[155,'MongoDb导出到Greenplum'],
			[157,'MongoDb导出到FTP'],
			[158,'MongoDb导出到HBase'],
			[159,'MongoDb导出到MongoDb'],
			
			[68,'MySQL导出到MongoDb'],
			[78,'SQLServer导出到MongoDb'],
			[88,'Oracle导出到MongoDb'],
			[139,'FTP导出到MongoDb'],
			[39,'HDFS导出到MongoDb'],
			[58,'本地文件导出到MongoDb'],
			[129,'CSV导出到MongoDb'],
			[119,'Greenplum导出到MongoDb'],
			[149,'HBase导出到MongoDb'],
			
			// added by zhengdandan
			[2030, 'MySQL导出到MySQL(新)'],
			[2031, 'MySQL导出到SQLServer(新)'],
			[2032, 'MySQL导出到Oracle(新)'],
			[2033, 'MySQL导出到FTP(新)'],
			[2034, 'MySQL导出到HDFS(新)'],
			[2035, 'MySQL导出到本地文件(新)'],
			[2036, 'MySQL导出到CSV(新)'],
			[2037, 'MySQL导出到Greenplum(新)'],
			//[2038, 'MySQL导出到HBase(新)'],
			[2039, 'MySQL导出到MongoDb(新)'],
			[2040, 'MySQL导出到SunDb(新)'],
			
			
			[2130, 'SQLServer导出到MySQL(新)'],
			[2131, 'SQLServer导出到SQLServer(新)'],
			[2132, 'SQLServer导出到Oracle(新)'],
			[2133, 'SQLServer导出到FTP(新)'],
			[2134, 'SQLServer导出到HDFS(新)'],
			[2135, 'SQLServer导出到本地文件(新)'],
			[2136, 'SQLServer导出到CSV(新)'],
			[2137, 'SQLServer导出到Greenplum(新)'],
			//[2138, 'SQLServer导出到HBase(新)'],
			[2139, 'SQLServer导出到MongoDb(新)'],
			[2140, 'QLServer导出到SunDb(新)'],
			
			
			[2230, 'Oracle导出到MySQL(新)'],
			[2231, 'Oracle导出到SQLServer(新)'],
			[2232, 'Oracle导出到Oracle(新)'],
			[2233, 'Oracle导出到FTP(新)'],
			[2234, 'Oracle导出到HDFS(新)'],
			[2235, 'Oracle导出到本地文件(新)'],
			[2236, 'Oracle导出到CSV(新)'],
			[2237, 'Oracle导出到Greenplum(新)'],
			//[2238, 'Oracle导出到HBase(新)'],
			[2239, 'Oracle导出到MongoDb(新)'],
			[2240, 'Oracle导出到SunDb(新)'],
			
			[2330, 'FTP导出到MySQL(新)'],
			[2331, 'FTP导出到SQLServer(新)'],
			[2332, 'FTP导出到Oracle(新)'],
			[2333, 'FTP导出到FTP(新)'],
			[2334, 'FTP导出到HDFS(新)'],
			[2335, 'FTP导出到本地文件(新)'],
			[2336, 'FTP导出到CSV(新)'],
			[2337, 'FTP导出到Greenplum(新)'],
			//[2338, 'FTP导出到HBase(新)'],
			[2339, 'FTP导出到MongoDb(新)'],
			[2340, 'FTP导出到SunDb(新)'],
			
			[2430, 'HDFS导出到MySQL(新)'],
			[2431, 'HDFS导出到SQLServer(新)'],
			[2432, 'HDFS导出到Oracle(新)'],
			[2433, 'HDFS导出到FTP(新)'],
			[2434, 'HDFS导出到HDFS(新)'],
			[2435, 'HDFS导出到本地文件(新)'],
			[2436, 'HDFS导出到CSV(新)'],
			[2437, 'HDFS导出到Greenplum(新)'],
			//[2438, 'HDFS导出到HBase(新)'],
			[2439, 'HDFS导出到MongoDb(新)'],
			[2440, 'HDFS导出到SunDb(新)'],
			
			[2530, '本地文件导出到MySQL(新)'],
			[2531, '本地文件导出到SQLServer(新)'],
			[2532, '本地文件导出到Oracle(新)'],
			[2533, '本地文件导出到FTP(新)'],
			[2534, '本地文件导出到HDFS(新)'],
			[2535, '本地文件导出到本地文件(新)'],
			[2536, '本地文件导出到CSV(新)'],
			[2537, '本地文件导出到Greenplum(新)'],
			//[2538, '本地文件导出到HBase(新)'],
			[2539, '本地文件导出到MongoDb(新)'],
			[2540, '本地文件导出到SunDb(新)'],
			
			[2630, 'CSV导出到MySQL(新)'],
			[2631, 'CSV导出到SQLServer(新)'],
			[2632, 'CSV导出到Oracle(新)'],
			[2633, 'CSV导出到FTP(新)'],
			[2634, 'CSV导出到HDFS(新)'],
			[2635, 'CSV导出到本地文件(新)'],
			[2636, 'CSV导出到CSV(新)'],
			[2637, 'CSV导出到Greenplum(新)'],
			//[2638, 'CSV导出到HBase(新)'],
			[2639, 'CSV导出到MongoDb(新)'],
			[2640, 'CSV导出到SunDb(新)'],
			
			[2730, 'Greenplum导出到MySQL(新)'],
			[2731, 'Greenplum导出到SQLServer(新)'],
			[2732, 'Greenplum导出到Oracle(新)'],
			[2733, 'Greenplum导出到FTP(新)'],
			[2734, 'Greenplum导出到HDFS(新)'],
			[2735, 'Greenplum导出到本地文件(新)'],
			[2736, 'Greenplum导出到CSV(新)'],
			[2737, 'Greenplum导出到Greenplum(新)'],
			//[2738, 'Greenplum导出到HBase(新)'],
			[2739, 'Greenplum导出到MongoDb(新)'],
			[2740, 'Greenplum导出到SunDb(新)'],
			
			/*[2830, 'HBase导出到MySQL(新)'],
			[2831, 'HBase导出到SQLServer(新)'],
			[2832, 'HBase导出到Oracle(新)'],
			[2833, 'HBase导出到FTP(新)'],
			[2834, 'HBase导出到HDFS(新)'],
			[2835, 'HBase导出到本地文件(新)'],
			[2836, 'HBase导出到CSV(新)'],
			[2837, 'HBase导出到Greenplum(新)'],
			[2838, 'HBase导出到HBase(新)'],
			[2839, 'HBase导出到MongoDb(新)'],*/
			
			[2930, 'MongoDb导出到MySQL(新)'],
			[2931, 'MongoDb导出到SQLServer(新)'],
			[2932, 'MongoDb导出到Oracle(新)'],
			[2933, 'MongoDb导出到FTP(新)'],
			[2934, 'MongoDb导出到HDFS(新)'],
			[2935, 'MongoDb导出到本地文件(新)'],
			[2936, 'MongoDb导出到CSV(新)'],
			[2937, 'MongoDb导出到Greenplum(新)'],
			//[2938, 'MongoDb导出到HBase(新)'],
			[2939, 'MongoDb导出到MongoDb(新)'],
			[2940, 'MongoDb导出到SunDb(新)'],
			
			[3030, 'SunDb导出到MySQL(新)'],
			[3031, 'SunDb导出到SQLServer(新)'],
			[3032, 'SunDb导出到Oracle(新)'],
			[3033, 'SunDb导出到FTP(新)'],
			[3034, 'SunDb导出到HDFS(新)'],
			[3035, 'SunDb导出到本地文件(新)'],
			[3036, 'SunDb导出到CSV(新)'],
			[3037, 'SunDb导出到Greenplum(新)'],
			[3039, 'SunDb导出到MongoDb(新)'],
			[3040, 'SunDb导出到SunDb(新)'],
			
		]
	},
	
	'jobTypeForFtp': {
		data: [
			[6, 'FTP文件同步到本地(间隔n分钟)'],
			[5, 'FTP文件同步到本地(需要成功标记)'],
			[7, 'FTP文件同步到本地(不需要成功标记)']
		]
	},
	
	'jobTypeForDependency': {
		data: [
			[92, '天依赖小时'],
            [93, '月依赖天']
		]
	},
	
	'dataType': {
		data: [
			[0, 'MySQL'],
			[1, 'SQLServer'],
			[2, 'Oracle'],
			// 为了方便value值与数据源的value对应，3预留给了FTP
			[3, 'FTP'],
			[4, 'HDFS'],
			[5, 'File'],
			[6, 'CSV'],
			[7, 'Greenplum'],
			//[8, 'HBase'],
			//add by zhoushasha
			[9,'MongoDb'],
			[10,'SunDb']
		]
	},
	
	'jobCycleType': {
		data: [
			[3, '天'],
			[4, '小时'],
			[5, '分钟'],
			[1, '月'],
			[2, '周'],
			[6, '待触发']
		]
	},
	
	'jobLevel': {
		data: [
			[3, 'lv3: 今天任务'],
			[4, 'lv4: 晚18点任务'],
			[5, 'lv5: 午13点任务'],
			[6, 'lv6: 早9点任务'],
			[7, 'lv7: 高层报表'],
			[8, 'lv8: 前台(对内)'],
			[9, 'lv9: 前台(对外)']
		]		
	},
	
	'taskStatus': {
		data: [
			[0, '未运行'],
			[1, '正在运行'],
			[2, '运行失败'],
			[3, '运行成功']
		]
	},
	
	'taskStatusForHistory': {
		data: [
			[0, '未运行'],
			[1, '正在运行'],
			[2, '运行失败'],
			[3, '运行成功'],
			[4, '取消补数据']
		]
	},
	
	'taskBackgroundStatus': {
		data: [
			[0, '初始化状态'],
			[1, '未触发'],
			[2, '已触发'],
			[3, '运行中'],
			[4, '运行失败'],
			[5, '运行成功'],
			[6, '重做初始化'],
			[7, '重做未触发'],
			[8, '重做已触发'],
			[9, '重做运行中'],
			[10, '重做失败'],
			[11, '重做成功']
		]
	},
	
	'taskFlag': {
		data: [
			[0, '补数据'],
			[1, '系统'],
			[2, '重跑']/*,
			[3, '新上线'],
			[4, '加权']*/
		]
	},
	
	'actionStatus': {
		data: [
			[0, '正在运行'],
			[1, '运行失败'],
			[2, '运行成功'],
			[3, '进程异常'],
			[4, '日志异常']
		]
	},
	
	'alertType': {
		data: [
		    [0, '需要告警'],
		    [2, '不需要告警']
			//[0, '需要告警,告警给本部门值班人员'],
			//[1, '需要告警,告警给其他部门接口人'],
			//[2, '不需要告警'],
			//[3, '暂停告警']
		]
	},
	
	/**
	 * 告警方式
	 * @type 
	 */
	'alertWays': {
		data: [
			[0, '工作时间以邮件告警,非工作时间以短信告警'],
			[1, '全天以邮件告警'],
			[2, '全天以短信告警'],
			[3, '全天短信、邮件同时告警'],
			[4, '工作时间以邮件告警,非工作时间短信、邮件同时告警']
		]
	},
	
	/**
	 * 告警目标
	 * @type 
	 */
	'alertTargets': {
		data: [
			[0, '告警给作业责任人'],
			[1, '告警给值周人'],
			[2, '同时告警给作业责任人和值周人']
		]
	},
	
	/**
	 * 短信组成方式
	 * @type 
	 */
	'smsContentWays': {
		data: [
			[0, '3个作业信息组成1条短信'],
			[1, '4个作业信息组成1条短信'],
			[2, '5个作业信息组成1条短信'],
			[3, '以70个字为单位组成一条短信，如果超过70个字，则分为2条短信发送']
		]
	},
	
	/**
	 * 监控告警系统中的告警类型
	 * @type 
	 */
	'monitorAlertType': {
		data: [
			[0, '错误告警'],
			[1, '延迟告警']
		]
	},
	
	/**
	 * 监控告警系统中的告警方式
	 * @type 
	 */
	'monitorAlertWay': {
		data: [
			[0, '邮件告警'],
			[1, '短信告警']
		]
	},
	
	/**
	 * 分区类型
	 * @type 
	 */
	'partitionType': {
		data: [
			['yyyyMMdd', 'yyyyMMdd'],
			['yyyyMMddHH', 'yyyyMMddHH']
		]
	},
	
	/**
	 * 文件准备完成的判断方式
	 * @type 
	 */
	'hudsonProjects': {
		data: [
			['ganrong', 'GanRong'],
			['daijin', 'DaiJin'],
			['mr', 'MR'],
			['py', 'PySrc']/*,
			['sw_udf', 'SW_UDF']*/
		]
	},
	
	/**
	 * 运行时长范围
	 * @type 
	 */
	'runTimeRanges': {
		data: [
			['0,300000', '5分钟以下'],
			['300000,600000', '5分钟至10分钟'],
			['600000,1800000', '10分钟至30分钟'],
			['1800000,3600000', '30分钟至60分钟'],
			['3600000,10800000', '60分钟至180分钟'],
			['10800000,0', '180分钟以上']
		]
	},
	
	'quickDateRanges': {
		data: [
			[0, '昨天'],
			[6, '最近七天'],
			[9, '最近十天'],
			[14, '最近十五天'],
			[29, '最近三十天'],
			[182, '最近半年'],
			[364, '最近一年']
		]
	},
	
	/**
	 * 调度系统状态
	 * @deprecated
	 * @type 
	 */
	'schedulerSystemStatus': {
		data: [
			[0, '关闭'],
			[1, '开启']
		]
	},
	
	/**
	 * 选取任务优先级
	 * @type 
	 */
	'referJobLevel': {
		data: [
			[0, '根据readTime从小到大优先选取任务'],
			[1, '根据任务的jobLevel从高到低选取任务']
		]
	},
	
	/**
	 * 任务运行优先级
	 * @type 
	 */
	'taskRunningPriority': {
		data: [
			[0, '只选取当日任务参考点'],
			[1, '只选取历史任务参考点'],
			[2, '按比例选取当日及历史参考点']
		]
	},
	
	/**
	 * 是否随机选取需要被修改为未触发状态的任务
	 * @type 
	 */
	'referPointRandom': {
		data: [
			[0, '按固定顺序选取参考点'],
			[1, '随机选取参考点'],
			[2, '自动切换参考点选取(9:30-22:30按固定顺序选取,其余时间随机选取)']
		]
	},
	
	/**
	 * 网关机状态
	 * @type 
	 */
	'gatewayStatus': {
		data: [
			[1, '启用'],
			[0, '禁用']
		]
	},
	
	/**
	 * 网关机调度方式
	 * @type 
	 */
	'schedulerWay': {
		data: [
			[0, '并行'],
			[1, '串行']
		]
	},
	
	/**
	 * 轮循方式
	 * @type 
	 */
	'roundWays': {
		data: [
			[0, '参考点'],
			[1, '模拟']
		]
	},
	
	'hdfsFileType': {
		data: [
			['rcfile', 'RC File'],
			['csv', 'CSV File'],
			['text', 'Text File']
		]
	},
	
	/**
	 * 压缩格式
	 * @type 
	 */
	'codecFormat': {
		data: [
			['org.apache.hadoop.io.compress.BZip2Codec', 'Bzip2'],
			['org.apache.hadoop.io.compress.GzipCodec', 'Gzip']
		]
	},
	/**
	 * Hudson项目发布状态
	 * @type 
	 */
	'hudsonPublishStatus': {
		data: [
			[0, '未发布'],
			[1, '正在发布'],
			[2, '发布成功'],
			[3, '发布失败']
		]
	},
	/* 
	 CREATE("添加"), UPDATE("修改"), DELETE("删除"), LOGIC_DELETE("禁用"), RECOVERY("启用"),

	PASSWORD("修改密码"),

	ASSIGN("分配"), UNASSIGN("解除"), UNAUTHORIZED("越权"),

	ONLINE("上线"), OFFLINE("下线"),

	REDO("重跑"), SUPPLY("补数据"), CANCEL_SUPPLY("取消补数据"), KILL_PID("杀进程"),

	PAUSE_ALERT("暂停告警"), RESET_ALERT("恢复告警"),

	SIMULATE("模拟调度"),

	PUBLISH("发布"),
	 */
	
	/**
	 * 操作动作
	 * @type 
	 */
	'operateActions': {
		data: [
			['CREATE', '创建'],
			['UPDATE', '修改'],
			['DELETE', '删除'],
			['LOGIC_DELETE', '禁用'],
			['RECOVERY', '启用'],
			['ASSIGN', '分配'],
			['UNASSIGN', '解除'],
			['UNAUTHORIZED', '越权'],
			['ONLINE', '上线'],
			['OFFLINE', '下线'],
			['SIMULATE', '模拟调度'],
			['PASSWORD', '修改密码'],
			['REDO', '重跑'],
			['SUPPLY', '补数据'],
			['CANCEL_SUPPLY', '取消补数据'],
			['TASK_UPDATE', '手工修改'],
			['KILL_PID', '杀进程'],
			['PAUSE_ALERT', '暂停告警'],
			['RESET_ALERT', '恢复告警'],
			['PUBLISH', '发布']
		]
	},
	
	'roles': {
		model: 'role',
		url: 'role/list',
		fields: ['roleId', 'roleName', 'isAdmin', 'createTime']
	},
	
	/**
	 * 所有正常用户
	 * @type 
	 */
	'users': {
		model: 'user',
		url: 'user/list',
		fields: ['userId', 'realName', 'mobilePhone'],
		baseParams: {condition: {status: 0}}
	},
	
	/**
	 * 指定用户组及子用户组下所有用户
	 * @type 
	 */
	'usersByUserGroup': {
		model: 'userGroupRelation',
		url: 'userGroupRelation/users',
		fields: ['userId', 'realName'],
		
		paramNames: ['userGroupId'],
		
		baseParams: {'userGroupCascade': true}
	},
	
	/**
	 * 根据指定用户过滤用户组
	 * @type 
	 */
	'userGroupsByUser': {
		model: 'userGroupRelation',
		url: 'userGroupRelation/userGroups',
		fields: ['userGroupId', 'name'],
		
		paramNames: ['userId']
	},
	
	/**
	 * 所有用户组
	 * @type 
	 */
	'userGroups': {
		model: 'userGroup',
		url: 'userGroup/list',
		fields: ['userGroupId', 'name'],
		baseParams: {condition: {'active-eq': true}},
		
		sortInfo: {
			field: 'userGroupId',
			direction: 'asc'
		}
	},
	
	/**
	 * 数据源
	 * @type 
	 */
	'datasources': {
		model: 'datasource',
		url: 'datasource/list',
		fields: ['datasourceId', 'name', 'ip', 'port', 'databaseName', 'charset', 'username', 'password', 'connectionString'],
		
		baseParams: {condition: {
			'active-eq': true,
			'userGroupId': USER_GROUP_ID
		}},
		
		conditionParamNames: ['type-eq']
	},
	
	/**
	 * 所有数据库的数据源
	 * @type 
	 */
	'databaseDatasources': {
		model: 'datasource',
		url: 'datasource/list',
		fields: ['datasourceId', 'name', 'ip', 'port', 'databaseName', 'charset', 'username', 'password', 'connectionString'],
		
		baseParams: {condition: {
			'active-eq': true,
			'userGroupId': USER_GROUP_ID,
			//'type-in': '0,1,2,7'
			'type-in': '0,1,2,7,3,8'
		}}
	},
	
	/**
	 * 所有网关机
	 * @type 
	 */
	'gateways': {
		model: 'gateway',
		url: 'gateway/list',
		fields: ['name'],
		
		baseParams: {
			sort: 'master',
			dir: 'desc'
		}
	},
	
	/**
	 * 获得可执行指定作业类型的网关机
	 * @type 
	 */
	'gatewaysByJobType': {
		model: 'gateway',
		url: 'gateway/getGatewaysByJobType',
		fields: ['name'],
		
		baseParams: {
			sort: 'master',
			dir: 'desc'
		},
		
		paramNames: ['jobType']
	},
	
	/**
	 * 所有被启用的网关机
	 * @type 
	 */
	'activeGateways': {
		model: 'gateway',
		url: 'gateway/list',
		fields: ['name'],
		
		baseParams: {
			sort: 'master',
			dir: 'desc',
			
			condition: {
				'status-eq': 1
			}
		}
	},
	
	/**
	 * 服务器
	 * @type 
	 */
	'servers': {
		model: 'server',
		url: 'server/list',
		fields: ['serverId', 'name']
	},
	
	/**
	 * 服务器帐号
	 * @type 
	 */
	'serverUsers': {
		model: 'serverUser',
		url: 'serverUser/list',
		fields: ['serverUserId', 'description']
	},
	
	/**
	 * 服务器脚本
	 * @type 
	 */
	'serverShells': {
		model: 'serverShell',
		url: 'serverShell/list',
		fields: ['serverShellId', 'name']
	}
	
};

S.MODEL_STORE_TYPE = {};
(function() {
	Ext.iterate(S.CONFIG, function(key) {
		var store = S.CONFIG[key],
			storeTypes = S.MODEL_STORE_TYPE[store.model] || [];
		
		storeTypes.push(key);
		S.MODEL_STORE_TYPE[store.model] = storeTypes;
	});
})();

/**
 * 数据源类型
 * @type 
 */
DATASOURCE_TYPE = {
	'MYSQL': 0,
	'SQLSERVER': 1,
	'ORACLE': 2,
	'FTP': 3,
	'GREENPLUM': 7,
	'DATABASE': 4, // 此类型包含了MySQL、SQLServer、GP和Oracle
	
	'HBASE': 8
};

/**
 * 作业状态
 * @type 
 */
JOB_STATUS = {
	UNLINE: 0, // 未上线
	ONLINE: 1, // 已上线
	OFFLINE: 2 // 已下线
};

/**
 * 作业周期
 * @type 
 */
JOB_CYCLE_TYPE = {
	MONTH: 1,
	WEEK: 2,
	DAY: 3,
	HOUR: 4,
	MINUTE: 5,
	NONE: 6
};

/**
 * 服务器执行状态
 * @type 
 */
SERVER_OPERATE_STATUS = {
	UNLINE: 0, // 未上线
	ONLINE: 1, // 已上线
	OFFLINE: 2 // 已下线
};

/**
 * 作业维度Module
 */
S.JOB_MAINTAIN_MODULES = function(jobType) {
	var module;
	switch (jobType) {
		case 5:
		case 6:
		case 7:
			module = 'com.sw.bi.scheduler.job.FtpMaintainModule';
		break;
		case 8:
			module = 'com.sw.bi.scheduler.job.GreenplumMaintainModule';
		break;
		case 9:
			module = 'com.sw.bi.scheduler.job.PutHdfsMaintainModule';
		break;
		case 10:
			module = 'com.sw.bi.scheduler.job.FileNumberCheckMaintainModule';
		break;
		case 20:
			module = 'com.sw.bi.scheduler.job.HiveMaintainModule';
		break;
		case 21:
			module = 'com.sw.bi.scheduler.job.MapreduceMaintainModule'
		break;
		case 40:
			module = 'com.sw.bi.scheduler.job.ShellMaintainModule'
		break;
		case 42:
			module = 'com.sw.bi.scheduler.job.MySQLProcedureMaintainModule';
		break;
		case 90:
			module = 'com.sw.bi.scheduler.job.MailMaintainModule';
		break;
		case 91:
			module = 'com.sw.bi.scheduler.job.ReportQuotaMonitorMaintainModule';
		break;
		case 92:
		case 93:
			module = 'com.sw.bi.scheduler.job.DependencyCheckMaintainModule';
		break;
		case 100:
			module = 'com.sw.bi.scheduler.job.VirtualMaintainModule';
			break;
		case 102:
			module = 'com.sw.bi.scheduler.job.BranchMaintainModule';
			break;
		case 103:
		case 104:
		case 2030:
		case 2031:
		case 2032:
		case 2033:
		case 2034:
		case 2035:
		case 2036:
		case 2037:
		case 2038:
		case 2039:
		case 2040:
		case 2130:
		case 2131:
		case 2132:
		case 2133:
		case 2134:
		case 2135:
		case 2136:
		case 2137:
		case 2138:
		case 2139:
		case 2140:
		case 2230:
		case 2231:
		case 2232:
		case 2233:
		case 2234:
		case 2235:
		case 2236:
		case 2237:
		case 2238:
		case 2239:
		case 2240:
		case 2330:
		case 2331:
		case 2332:
		case 2333:
		case 2334:
		case 2335:
		case 2336:
		case 2337:
		case 2338:
		case 2339:
		case 2340:
		case 2430:
		case 2431:
		case 2432:
		case 2433:
		case 2434:
		case 2435:
		case 2436:
		case 2437:
		case 2438:
		case 2439:
		case 2440:
		case 2530:
		case 2531:
		case 2532:
		case 2533:
		case 2534:
		case 2535:
		case 2536:
		case 2537:
		case 2538:
		case 2539:
		case 2540:
		case 2630:
		case 2631:
		case 2632:
		case 2633:
		case 2634:
		case 2635:
		case 2636:
		case 2637:
		case 2638:
		case 2639:
		case 2640:
		case 2730:
		case 2731:
		case 2732:
		case 2733:
		case 2734:
		case 2735:
		case 2736:
		case 2737:
		case 2738:
		case 2739:
		case 2740:
		case 2830:
		case 2831:
		case 2832:
		case 2833:
		case 2834:
		case 2835:
		case 2836:
		case 2837:
		case 2838:
		case 2839:
		case 2840:
		case 2930:
		case 2931:
		case 2932:
		case 2933:
		case 2934:
		case 2935:
		case 2936:
		case 2937:
		case 2938:
		case 2939:
		case 2940:
		case 3030:
		case 3031:
		case 3032:
		case 3033:
		case 3034:
		case 3035:
		case 3036:
		case 3037:
		case 3038:
		case 3039:
		case 3040:
			module = 'com.sw.bi.scheduler.job.NewDataXMaintainModule';
			break;
		default:
			module = 'com.sw.bi.scheduler.job.DataXMaintainModule';
	}
	return module;
};

/**
 * 校验当前登录用户对指定用户组是否有权限
 * @param {Long} userGroupId
 */
S.IS_AUTHORIZED_USER_GROUP = function(userGroupId) {
	if (Ext.isEmpty(USER_GROUP_ID)) {
		return false;
	}
	
	if (USER_GROUP_IS_ADMINISTRTOR === true) {
		return true;
	}
	
	var regexp = new RegExp('^' + USER_GROUP_ID, 'i');
	return regexp.test(String(userGroupId));
};