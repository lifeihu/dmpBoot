_package('com.sw.bi.scheduler.history');

com.sw.bi.scheduler.history.SupplyHistoryListModule = Ext.extend(framework.core.Module, {
	model: 'redoAndSupplyHistory',
	
	/**
	 * @cfg operateNo
	 * @type String
	 */
	operateNo: null,
	
	north: function() {
		return {
			xtype: 'grid',
			height: 350,
			
			/*actions: [{
				iconCls: 'cancel',
				tooltip: '取消补数据',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (!S.IS_AUTHORIZED_USER_GROUP(record.get('operateUserGroup.userGroupId'))) {
						cc.setCellLink(row, col, false);
						return null;
					}
					
					if (record.get('taskStatus') != 0) {
						cc.setCellLink(row, col, false);
					}
					
					return newValue;
				},
				handler: this.cancelSupply,
				scope: this
			}],*/
			
			columns: [new Ext.grid.RowNumberer(), {
				header: '操作批号',
				dataIndex: 'operateNo',
				width: 150
			}, {
				xtype: 'customcolumn',
				iconCls: 'cancel',
				tooltip: '取消补数据',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (!S.IS_AUTHORIZED_USER_GROUP(record.get('operateUserGroup.userGroupId'))) {
						cc.setCellLink(row, col, false);
						return null;
					}
					
					if (record.get('taskStatus') != 0) {
						cc.setCellLink(row, col, false);
					}
					
					return null;
				},
				handler: this.cancelSupply,
				scope: this
			}, {
				header: '操作类型',
				dataIndex: 'operateType',
				width: 80,
				renderer: function(value, meta, record) {
					return record.get('operateNo').indexOf('_SS') > -1 ? '串行补' : '并行补';
				}
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}, {
				header: '作业ID',
				dataIndex: 'jobId',
				width: 80
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 400,
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
				store: S.create('taskStatusForHistory')
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
			
			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'redoAndSupplyHistory/supplyMasters',
				fields: ['operateNo', 'jobId', 'jobName', 'cycleType', 'dutyMan', 'taskStatus', 'settingTime', 'operateDate', 'taskDate', 'operateMan', 'createTime', 'beginTime', 'endTime', 'operateUserGroup.name', 'operateUserGroup.userGroupId'],
				
				totalProperty: "total",
				root: "paginationResults",
				
				baseParams: {
					start: 0,
					limit: 1000,
					condition: Ext.encode({
						'operateNo-eq': this.operateNo,
						'isMaster-eq': true
					})
				}
			}),
			
			listeners: {
				rowdblclick: this.onRowDblClick,
				scope: this
			}
		};
	},
	
	center: function() {
		return {
			xtype: 'grid',
			height: 350,
			
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
			
			columns: [new Ext.grid.RowNumberer(), {
				header: '操作批号',
				dataIndex: 'operateNo',
				width: 150
			}, {
				header: '操作类型',
				dataIndex: 'operateType',
				width: 80,
				renderer: function(value, meta, record) {
					return record.get('operateNo').indexOf('_SS') > -1 ? '串行补' : '并行补';
				}
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}, {
				header: '作业ID',
				dataIndex: 'jobId',
				width: 80
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 400,
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
				store: S.create('taskStatusForHistory')
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
			
			store: new Ext.data.JsonStore({
				url: 'redoAndSupplyHistory/list',
				fields: ['operateNo', 'jobId', 'jobName', 'cycleType', 'dutyMan', 'taskStatus', 'settingTime', 'operateDate', 'taskDate', 'operateMan', 'createTime', 'beginTime', 'endTime', 'actionId']
			})
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
	},
	
	cancelSupply: function(record) {
		var mdl = this,
		
			operateNo = record.get('operateNo'),
			taskDate = record.get('taskDate'),
			taskDate = taskDate.substring(0, taskDate.indexOf(' ')),
		
			isSerialSupply = operateNo.indexOf('_SS') > -1,
			msg = '是否取消任务(批号: ' + operateNo + ', 任务日期: ' + taskDate + (isSerialSupply ? '<span style="color:red;">及以后日期</span>' : '') + ')的补数据操作?';
			
		Ext.Msg.confirm('提示', msg, function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'task/cancelSupply',
				params: {
					operateNo: operateNo,
					taskDate: taskDate
				},
				waitMsg: '正在取消任务的补数据操作,请耐心等候...',
				
				success: function(response) {
					mdl.northPnl.store.load();
					mdl.centerPnl.store.removeAll();
				}
			});
		});
	},
	
	////////////////////////////////////////////////////////////////////////////////////
	
	onRowDblClick: function(grid, row, e) {
		var record = grid.getSelectionModel().getSelected();
		this.centerPnl.store.load({params:{condition: {
			'operateNo-eq': this.operateNo,
			'taskDate-eq': record.get('taskDate')
		}}});
	}
});