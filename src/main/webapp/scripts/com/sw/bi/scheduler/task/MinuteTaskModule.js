_package('com.sw.bi.scheduler.task');

_import([
	'com.sw.bi.scheduler.task.TaskModule'
]);

com.sw.bi.scheduler.task.MinuteTaskModule = Ext.extend(com.sw.bi.scheduler.task.TaskModule, {
	autoLoadData: false,
	
	/**
	 * @property task
	 * @type 
	 */
	
	center: function() {
		return {
			xtype: 'grid',
			height: 300,
			
			columns: [new Ext.grid.RowNumberer(), {
				header: '作业ID',
				dataIndex: 'jobId'
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 400,
				sortable: true,
				tooltip: {
					qtitle: '预设时间',
					qtip: '{settingTime}'
				},
				renderer: function(value, meta, record) {
					var settingTime = Date.parseDate(record.get('settingTime'), 'Y-m-d H:i:s');
					return value + '(' + settingTime.format('H:i') + ')';
				}
			}, {
				header: '业务组',
				dataIndex: 'jobBusinessGroup',
				sortable: true,
				width: 120
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				store: S.create('users'),
				sortable: true,
				width: 80
			}, {
				header: '作业状态',
				dataIndex: 'taskStatus',
				width: 60,
				sortable: true,
				renderer: function(value) {
					return FOREGROUND_TASK_STATUS_NATIVE[value];
				},
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'jobType',
				width: 250,
				sortable: true,
				store: S.create('jobType')
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				width: 80,
				sortable: true,
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '开始时间',
				dataIndex: 'taskBeginTime',
				width: 130,
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '结束时间',
				dataIndex: 'taskEndTime',
				width: 130,
				dateTimeColumn: true
			}/*, {
				header: '运行时长(分钟)',
				dataIndex: 'runTime',
				width: 110,
				sortable: true,
				renderer: function(value) {
					if (!Ext.isEmpty(value, false) && value > 0) {
						return parseInt(value / 1000 / 60, 10);
					}
					
					return value;
				}
			}*/],

			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'task/groupMinuteTasks',
				fields: ['jobId', 'jobName', 'jobBusinessGroup', 'dutyOfficer', 'taskStatus', 'jobType', 'taskDate', 'taskBeginTime', 'taskEndTime', 'runTime', 'settingTime'],
				baseParams: {
					jobId: this.task.jobId,
					taskDate: this.task.taskDate
				}
			}),
			
			listeners: {
				rowdblclick: this.onHourTaskDblClick,
				scope: this
			}
		};
	},
	
	onTaskChange: function(tp, tab) {
		com.sw.bi.scheduler.task.MinuteTaskModule.superclass.onTaskChange.call(this, tp, tab);
		
		var searcher = tab.getComponent(0).form;
		if (!searcher) return;
		
		searcher.findField('taskDateStart').setReadOnly(true);
		searcher.findField('taskDateEnd').setReadOnly(true);
		
		searcher.findField('settingTimeStart').setReadOnly(true);
		searcher.findField('settingTimeEnd').setReadOnly(true);
	},
	
	onHourTaskDblClick: function(grid, rowIndex, e) {
		var record = grid.store.getAt(rowIndex),
			settingTime = Date.parseDate(record.get('settingTime'), 'Y-m-d H:i:s');
		
		this.loadTaskData({
			'jobId': record.get('jobId'),
			
			'taskDateStart': record.get('taskDate'),
			'taskDateEnd': record.get('taskDate'),
			
			'settingTimeStart': settingTime.format('Y-m-d H:00:00'),
			'settingTimeEnd': settingTime.format('Y-m-d H:59:59')
		});
	}
	
});