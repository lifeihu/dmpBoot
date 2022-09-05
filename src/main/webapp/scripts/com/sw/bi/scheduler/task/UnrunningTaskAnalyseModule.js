_package('com.sw.bi.scheduler.task');

com.sw.bi.scheduler.task.UnrunningTaskAnalyseModule = Ext.extend(framework.core.Module, {
	
	/**
	 * 需要分析的任务
	 * @type 
	 */
	taskIds: null,
	
	/**
	 * 需要分析的参考点
	 * @type 
	 */
	referTaskIds: null,
	
	center: function() {
		return {
			xtype: 'grid',
			
			columns: [{
				hidden: true,
				dataIndex: 'analyseGroupName',
				header: '分析任务'
			}, {
				dataIndex: 'analyseType',
				header: '分析类型',
				width: 60
			}, {
				dataIndex: 'jobId',
				header: '作业ID',
				width: 60
			}, {
				xtype: 'customcolumn',
				dataIndex: 'name',
				header: '作业名称',
				width: 400,
				renderer: function(value, oldValue, options, record, row, col, store, grid, cc) {
					// 已触发的GP作业允许打开GP任务分析
					if (record.get('jobType') == 8 && (record.get('taskStatus') == 2 || record.get('taskStatus') == 8)) {
						return value;
						
					} else {
						cc.setCellLink(row, col, false);
						return value;
					}
				},
				handler: this.analyseUnrunningBigGreenplumTasks,
				scope: this
			}, {
				header: '参考点顺序',
				dataIndex: 'analyseReferPointIndex',
				width: 70
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
				header: '类型',
				dataIndex: 'jobType',
				width: 250,
				sortable: true,
				store: S.create('jobType')
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
				groupField: 'analyseGroupName',
				
				url: this.taskIds != null ? 'task/analyseUnrunningTasks' : 'task/analyseUnrunningTasksByReferPoint',
				baseParams: {
					taskId: this.taskIds == null ? null : this.taskIds.join(','),
					referTaskId: this.referTaskIds == null ? null : this.referTaskIds.join(',')
				},
				
				reader: new Ext.data.JsonReader({}, [
					'analyseType', 'analyseGroupName', 'jobType',
					'jobId', 'name', 'cycleType', 'taskStatus',
					'taskDate', 'settingTime', 'dutyOfficer', 'taskId',
					'taskBeginTime', 'taskEndTime', 'analyseReferPointIndex'
				])
			}),
			
			listeners: {
				rowdblclick: this.onRowDblClick,
				scope: this
			}
		}
	},
	
	analyseUnrunningBigGreenplumTasks: function(record) {
		var win = this.createWindow({
			title: 'Greenplum任务未运行原因分析',
			
			module: {
				module: 'com.sw.bi.scheduler.task.UnrunningBigGreenplumTaskAnalyseModule',
				
				greenplumTaskId: record.get('taskId')
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
	},
	
	onRowDblClick: function(grid, rowIndex, e) {
		var store = grid.store,
			record = store.getAt(rowIndex);
		
		store.removeAll();
			
		this.taskIds = [record.get('taskId')];
		this.referTaskIds = null;
		
		store.baseParams = {
			taskId: record.get('taskId'),
			referTaskId: null
		};
		store.load();
	}
	
});