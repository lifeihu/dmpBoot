_package('com.sw.bi.scheduler.alertsystemlog');

com.sw.bi.scheduler.alertsystemlog.AlertSystemLogListModule = Ext.extend(framework.core.Module, {
	
	north: function() {
		var store = new Ext.data.JsonStore({
				url: 'alertSystemLog/pagingGroup',
				fields: ['alertDate', 'jobId', 'jobName', 'name', 'dutyOfficer', 'alertCount', 'alertTime', 'taskDate', 'cycleType', 'userGroup.name', 'userGroup.userGroupId'],
				totalProperty: 'total',
				root: 'paginationResults'
			});
		
		return {
			xtype: 'grid',
			height: 400,
			split: true,

			bbar: [{
				xtype: 'paging',
				pageSize: 15,
				displayInfo: true,
				store: store
			}],
			
			actions: [{
				iconCls: 'offline',
				tooltip: '暂停告警',
				handler: this.pauseAlert,
				scope: this
			}, {
				iconCls: 'online',
				tooltip: '恢复告警',
				handler: this.resetAlert,
				scope: this
			}],
			
			columns: [{
				xtype: 'customcolumn',
				dateColumn: true,
				header: '告警日期',
				dataIndex: 'alertDate'
			}, {
				header: '作业ID',
				dataIndex: 'jobId',
				width: 60
			}, {
				xtype: 'customcolumn',
				dateColumn: true,
				header: '任务日期',
				dataIndex: 'taskDate'
			}, {
				header: '作业名称',
				dataIndex: 'name',
				width: 400
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				width: 60,
				store: S.create('jobCycleType')
			}, {
				header: '责任人',
				dataIndex: 'dutyOfficer',
				width: 80
			}, {
				header: '用户组',
				dataIndex: 'userGroup.name'
			}, {
				xtype: 'customcolumn',
				header: '告警次数',
				dataIndex: 'alertCount',
				width: 60,
				handler: this.loadDetail,
				scope: this
			}, {
				header: '最近告警时间',
				dataIndex: 'alertTime',
				width: 150
			}],
			
			store: store
		};
	},
	
	center: function() {
		var store = new Ext.data.JsonStore({
				url: 'alertSystemLog/paging',
				fields: ['alertDate', 'jobId', 'taskId', 'jobName', 'dutyOfficer', 'alertType', 'alertWay', 'alertTime', 'jobStatus', 'name', 'taskDate', 'lastActionId', 'userGroup.userGroupId', 'userGroup.name'],
				totalProperty: 'total',
				root: 'paginationResults'
			});
		
		return {
			xtype: 'grid',
			height: 400,

			bbar: [{
				xtype: 'paging',
				pageSize: 15,
				displayInfo: true,
				store: store
			}],
			
			actions: [{
				iconCls: 'offline',
				tooltip: '暂停告警',
				handler: this.pauseAlert,
				scope: this
			}, {
				iconCls: 'online',
				tooltip: '恢复告警',
				handler: this.resetAlert,
				scope: this
			}, {
				iconCls: 'log',
				tooltip: '查看日志',
				handler: this.viewLog,
				scope: this
			}],
			
			columns: [{
				xtype: 'customcolumn',
				dateColumn: true,
				header: '告警日期',
				dataIndex: 'alertDate'
			}, {
				header: '作业ID',
				dataIndex: 'jobId',
				width: 60
			}, {
				xtype: 'customcolumn',
				dateColumn: true,
				header: '任务日期',
				dataIndex: 'taskDate'
			}, {
				header: '任务ID',
				dataIndex: 'taskId',
				width: 60
			}, {
				header: '作业名称',
				dataIndex: 'name',
				width: 400
			}, {
				header: '作业状态',
				dataIndex: 'jobStatus',
				width: 60,
				renderer: function(value, meta, record) {
					var foregroundStatus = FOREGROUND_TASK_STATUS[value];
					
					var qtip = [];
					qtip.push('<div>前台状态: ', foregroundStatus, '</div>');
					qtip.push('<div>后台状态: ', BACKGROUND_TASK_STATUS[value], '</div>');
					
					meta.attr += ' ext:qtitle="作业状态" ext:qtip="' + qtip.join('') + '"';
					
					return foregroundStatus;
				}
			}, {
				header: '责任人',
				dataIndex: 'dutyOfficer',
				width: 80
			}, {
				header: '用户组',
				dataIndex: 'userGroup.name'
			}, {
				xtype: 'customcolumn',
				header: '告警类型',
				dataIndex: 'alertType',
				store: S.create('monitorAlertType')
			}, {
				xtype: 'customcolumn',
				header: '告警方式',
				dataIndex: 'alertWay',
				store: S.create('monitorAlertWay')
			}, {
				header: '告警时间',
				dataIndex: 'alertTime',
				width: 150
			}],
			
			store: store
		};
	},
	
	loadData: function(params) {
		if (Ext.isEmpty(params)) return;
		/*var module = this.ownerCt,
			params = module.getCondition(true);*/
		
		this.northPnl.store.baseParams = {
			start: 0,
			limit: 15,
			condition: params
		};
		
		this.northPnl.store.load();
	},
	
	loadDetail: function(record) {
		var store = this.centerPnl.store,
		
			module = this.ownerCt,
			params = module.getCondition(true);
			
		delete params['alertDate-ge'];
		delete params['alertDate-le'];
		delete params['jobId-in'];
			
		store.baseParams = {condition: Ext.apply(params, {
			'alertDate-eq': record.get('alertDate'),
			'jobId-eq': record.get('jobId'),
			'taskDate-eq': record.get('taskDate')
		})};
		
		store.load({params: {
			start: 0,
			limit: 15
		}});
	},
	
	pauseAlert: function(record) {
		Ext.Msg.confirm('提示', '是否暂停 "' + record.get('jobName') + '" 作业告警?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'task/pauseAlert',
				params: {
					taskDate: record.get('taskDate'),
					// scanDate: new Date(),
					jobId: record.get('jobId'),
					taskId: record.get('taskId')
				},
				waitMsg: '正在暂停告警,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '作业已被暂停告警.');
				}
			});
		});
	},
	
	resetAlert: function(record) {
		Ext.Msg.confirm('提示', '是否恢复 "' + record.get('jobName') + '" 作业告警?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'task/resetAlert',
				params: {
					taskDate: record.get('taskDate'),
					jobId: record.get('jobId'),
					taskId: record.get('taskId')
				},
				waitMsg: '正在恢复告警,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '作业已被恢复告警.');
				}
			});
		});
	},
	
	viewLog: function(record) {
		var actionId = record.get('lastActionId');
		
		framework.createWindow({
			title: '日志信息',
			iconCls: 'log',
			
			module: {
				module: 'com.sw.bi.scheduler.action.ActionLogModule',
				actionId: actionId
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
	},
	
	doLayout: function(mdl) {
		com.sw.bi.scheduler.alertsystemlog.AlertSystemLogListModule.superclass.doLayout.apply(this, arguments);
		
		var mdl = this.ownerCt,
			
			cheight = mdl.getHeight(true) - mdl.northPnl.getHeight();
			
		mdl.centerPnl.setHeight(cheight);
		
		this.northPnl.setHeight(cheight * .4);
		this.centerPnl.setHeight(cheight * .6);
	}
	
});