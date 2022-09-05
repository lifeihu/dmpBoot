_package('com.sw.bi.scheduler.schedulersystemstatus');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.schedulersystemstatus.SchedulerSystemStatusMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	master: function() {
		return {
			model: 'scheduleSystemStatus',
			
			items: [{
				columnWidth: 1,
				name: 'gateway',
				fieldLabel: '网关机编号',
				labelWidth: 190
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'status',
				fieldLabel: '调度系统状态',
				labelWidth: 190,
				allowBlank: false,
				store: S.create('schedulerSystemStatus')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'referToJobLevel',
				fieldLabel: '选取任务优先级',
				labelWidth: 190,
				allowBlank: false,
				store: S.create('referJobLevel')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'taskRunningPriority',
				fieldLabel: '参考点选取',
				labelWidth: 190,
				allowBlank: false,
				store: S.create('taskRunningPriority')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'referPointRandom',
				fieldLabel: '参考点选取是否随机',
				labelWidth: 190,
				allowBlank: false,
				store: S.create('referPointRandom')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'taskFailReturnTimes',
				fieldLabel: '任务出错重跑次数',
				labelWidth: 190,
				allowBlank: false,
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[0, 0],
						[1, 1],
						[2, 2],
						[3, 3],
						[4, 4],
						[5, 5],
						[6, 6],
						[7, 7],
						[8, 8],
						[9, 9],
						[10, 10]
					]
				})
			}, {
				columnWidth: 1,
				name: 'taskRunningMax',
				fieldLabel: '调度系统同时运行的最大任务数',
				labelWidth: 190,
				// anchor: '50%',
				allowBlank: false
			}, {
				columnWidth: 1,
				name: 'waitUpdateStatusTaskCount',
				fieldLabel: '调度系统选取的最大参考点数',
				labelWidth: 190,
				// anchor: '50%',
				allowBlank: false
			}, {
				columnWidth: 1,
				name: 'taskCountExceptJobs',
				fieldLabel: '统计执行任务数需要排除的作业ID',
				labelWidth: 190,
				vtype: 'mutilnum'
			}]
		};
	}
	
});