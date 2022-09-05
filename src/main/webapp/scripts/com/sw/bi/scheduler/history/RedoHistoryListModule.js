_package('com.sw.bi.scheduler.history');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.history.RedoHistoryListModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'redoAndSupplyHistory',
	
	actionViewConfig: false,
	actionUpdateConfig: false,
	
	detailer: function() {
		return {
			allowDefaultButtons: false,
			
			actions: [{
				iconCls: 'log',
				tooltip: '日志',
				width: 40,
				renderer: function(newValue, oldValue, meta, record, row, col, store, grid, cc) {
					if (Ext.isEmpty(record.get('actionId'), false)) {
						cc.setCellLink(row, col, false);
					}
					
					return newValue;
				},
				handler: this.viewLog,
				scope: this
			}],
			
			columns: [{
				header: '操作批号',
				dataIndex: 'operateNo',
				width: 150
			}, {
				header: '作业ID',
				dataIndex: 'jobId',
				width: 80
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 150,
				renderer: function(value, meta, record) {
					var cycleType = record.get('cycleType'),
						settingTime = record.get('settingTime');

					if (!Ext.isEmpty(settingTime, false)) {
						meta.attr += ' ext:qtip="<b>预设时间</b>: ' + settingTime + '"';
						
						if (cycleType == JOB_CYCLE_TYPE.HOUR || cycleType == JOB_CYCLE_TYPE.MINUTE) {
							var hm = settingTime.substring(settingTime.indexOf(' ') + 1, settingTime.lastIndexOf(':'));
							value += '(' + hm + ')';
						}
					}
					
					return value;
				}
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyMan',
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '任务状态',
				dataIndex: 'taskStatus',
				store: S.create('taskStatus')
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '操作日期',
				dataIndex: 'operateDate',
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '操作人',
				dataIndex: 'operateMan',
				store: S.create('users')
			}, {
				header: '操作人用户组',
				dataIndex: 'operateUserGroup.name'
			}, {
				xtype: 'customcolumn',
				header: '创建时间',
				dataIndex: 'createTime',
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '开始时间',
				dataIndex: 'beginTime',
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '结束时间',
				dataIndex: 'endTime',
				dateTimeColumn: true
			}, {
				header: '时长',
				width: 40,
				renderer: function(value, meta, record) {
					var beginTime = record.get('beginTime'),
						endTime = record.get('endTime');

					if (!Ext.isEmpty(beginTime, false) && !Ext.isEmpty(endTime, false)) {
						return parseInt((Date.parseDate(endTime, 'Y-n-j H:i:s').getTime() - Date.parseDate(beginTime, 'Y-n-j H:i:s').getTime()) / 1000 / 60, 10);
					}
					
					return null;
				}
			}],
			
			store: {
				fields: ['actionId', 'cycleType', 'settingTime', 'operateUserGroup.name', 'operateUserGroup.userGroupId']
			}
		};
	},
	
	viewLog: function(record) {
		var actionId = record.get('actionId');
		
		framework.createWindow({
			title: '日志查看',
			iconCls: 'log',
			
			module: {
				module: 'com.sw.bi.scheduler.action.ActionLogModule',
				actionId: actionId
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
	}
});