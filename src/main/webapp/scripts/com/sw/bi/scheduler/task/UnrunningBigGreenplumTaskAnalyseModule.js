_package('com.sw.bi.scheduler.task');

com.sw.bi.scheduler.task.UnrunningBigGreenplumTaskAnalyseModule = Ext.extend(framework.core.Module, {
	
	/**
	 * 需要分析的任务
	 * @type 
	 */
	greenplumTaskId: null,
	
	center: function() {
		return {
			xtype: 'grid',
			
			columns: [{
				hidden: true,
				dataIndex: 'gateway',
				header: '网关机'
			}, {
				dataIndex: 'jobId',
				header: '作业ID',
				width: 60
			}, {
				dataIndex: 'name',
				header: '作业名称',
				width: 400,
				renderer: function(value, meta, record) {
					if (record.get('taskId') === this.greenplumTaskId) {
						meta.style += 'font-weight:bold;';
					}
					
					return value;
				},
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '预设时间',
				dataIndex: 'settingTime',
				dateTimeColumn: true,
				renderer: function(newValue, value, meta, record) {
					var taskStatus = record.get('taskStatus'),
						foregroundStatus = record.get('merge') === true ? FOREGROUND_TASK_STATUS_NATIVE[taskStatus] : FOREGROUND_TASK_STATUS[taskStatus],
						settingTime = Date.parseDate(newValue, 'Y-m-d G:i:s');
						
					if (foregroundStatus == '未运行' && settingTime.getTime() > new Date().getTime()) {
						meta.style += 'color:purple;font-weight:bold;';
					}
					
					return newValue;
				}
			}, {
				header: '尾号',
				dataIndex: 'tailNumber',
				width: 50
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				width: 50,
				store: S.create('jobCycleType')
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				store: S.create('users'),
				width: 80
			}, {
				header: '作业状态',
				dataIndex: 'taskStatus',
				width: 60,
				renderer: function(value, meta, record) {
					var foregroundStatus = null,
						qtip = [];
						
					if (record.get('merge') === true) {
						foregroundStatus = FOREGROUND_TASK_STATUS_NATIVE[value];
						qtip.push('<div>前台状态: ', FOREGROUND_TASK_STATUS_NATIVE[value], '</div>');
					} else {
						foregroundStatus = FOREGROUND_TASK_STATUS[value];
						qtip.push('<div>前台状态: ', FOREGROUND_TASK_STATUS[value], '</div>');
						qtip.push('<div>后台状态: ', BACKGROUND_TASK_STATUS[value], '</div>');
					}
					
					meta.attr += ' ext:qtitle="作业状态" ext:qtip="' + qtip.join('') + '"';
					
					if (foregroundStatus == '未运行') {
						meta.style += 'color:red;';
					}
					
					return foregroundStatus;
				},
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				width: 80,
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
			}],
			
			view: new Ext.grid.GroupingView(),
			
			store: new Ext.data.GroupingStore({
				autoLoad: true,
				groupField: 'gateway',
				
				url: 'task/analyseUnrunningBigGreenplumTasks',
				
				reader: new Ext.data.JsonReader({}, [
					'gateway', 'tailNumber', 'taskId',
					'jobId', 'name', 'cycleType', 'taskStatus',
					'taskDate', 'settingTime', 'dutyOfficer',
					'taskBeginTime', 'taskEndTime'
				])
			})
		}
	}
	
});