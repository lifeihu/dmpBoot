_package('com.sw.bi.scheduler.schedulersystemstatus');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.schedulersystemstatus.SchedulerSystemStatusModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'scheduleSystemStatus',
	actionViewConfig: false,
	
	maintain: {
		title: '调度系统配置',
		width: 600,
		height: 300,
		
		module: {
			module: 'com.sw.bi.scheduler.schedulersystemstatus.SchedulerSystemStatusMaintainModule'
		}
	},
	
	detailer: function() {
		return {
			allowDefaultButtons: false,
			
			columns: [{
				header: '网关机编号',
				dataIndex: 'gateway',
				width: 100
			}, {
				xtype: 'customcolumn',
				header: '调度系统状态',
				dataIndex: 'status',
				store: S.create('schedulerSystemStatus')
			}, {
				xtype: 'customcolumn',
				header: '选取任务优先级',
				dataIndex: 'referToJobLevel',
				width: 200,
				tooltip: true,
				store: S.create('referJobLevel')
			}, {
				xtype: 'customcolumn',
				header: '参考点选取',
				dataIndex: 'taskRunningPriority',
				width: 200,
				tooltip: true,
				store: S.create('taskRunningPriority')
			}, {
				xtype: 'customcolumn',
				header: '随机选取参考点',
				dataIndex: 'referPointRandom',
				width: 200,
				tooltip: true,
				store: S.create('referPointRandom')
			}, {
				xtype: 'customcolumn',
				header: '任务出错重跑次数',
				dataIndex: 'taskFailReturnTimes',
				width: 120
			}, {
				xtype: 'customcolumn',
				header: '调度系统同时运行的最大任务数',
				dataIndex: 'taskRunningMax',
				width: 200
			}, {
				xtype: 'customcolumn',
				header: '调度系统选取的最大参考点数',
				dataIndex: 'waitUpdateStatusTaskCount',
				width: 200
			}, {
				xtype: 'customcolumn',
				header: '统计执行任务数需要排除的作业ID',
				dataIndex: 'taskCountExceptJobs',
				width: 230
			}]
		};
	}
});
