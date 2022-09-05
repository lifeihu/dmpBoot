_package('com.sw.bi.scheduler.action');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.action.ActionModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'action',
	autoLoadData: false,
	
	actionUpdateConfig: false,
	actionViewConfig: false,
	
	initModule: function() {
		this.initActionMenu();
		
		com.sw.bi.scheduler.action.ActionModule.superclass.initModule.call(this);
	
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			scope: this
		});
	},
	
	searcher: function() { 
		return {
			items: [{
				xtype: 'hidden', name: 'taskId'
			}, {
				xtype: 'hidden', name: 'actionId'
			}, {
				xtype: 'hidden', name: 'scanDate'
			}, {
				xtype: 'hidden', name: 'startTime'
			}, {
				xtype: 'hidden', name: 'endTime'
			}, {
				columnWidth: .3,
				name: 'jobId',
				fieldLabel: '作业ID'
			}, {
				columnWidth: .3,
				name: 'jobName',
				fieldLabel: '作业名称'
			}, {
				columnWidth: .2,
				xtype: 'datetimefield',
				name: 'settingTime',
				fieldLabel: '预设时间',
				format: 'Y-m-d H:i:00'
			}, {
				columnWidth: .2,
				xtype: 'combo',
				hiddenName: 'actionStatus',
				fieldLabel: '作业状态',
				store: S.create('actionStatus')
			}, {
				columnWidth: .2,
				xtype: 'combo',
				hiddenName: 'flag',
				fieldLabel: '任务标志',
				store: S.create('taskFlag')
			}, {
				columnWidth: .2,
				xtype: 'combo',
				name: 'operator',
				fieldLabel: '操作人',
				store: S.create('users')
			}, {
				columnWidth: .2,
				xtype: 'datefield',
				format: 'Y-m-d',
				name: 'taskDateStart',
				fieldLabel: '开始日期',
				value: new Date(),
				allowBlank: false
			}, {
				columnWidth: .2,
				xtype: 'datefield',
				format: 'Y-m-d',
				name: 'taskDateEnd',
				fieldLabel: '结束日期',
				value: new Date(),
				allowBlank: false
			}, {
				columnWidth: .17,
				xtype: 'combo',
				name: 'gateway',
				fieldLabel: '网关机',
				store: S.create('gateways')
			}, {
				columnWidth: .28,
				xtype: 'multicombo',
				hiddenName: 'jobType',
				fieldLabel: '类型',
				store: S.create('jobType')
			}, {
				columnWidth: .08,
				xtype: 'checkbox',
				name: 'useScanDate',
				fieldLabel: '扫描日期',
				checked: false,
				listeners: {
					check: this.onScanDateCheck,
					scope: this
				}
			}]
		};
	},
	
	detailer: function() {
		var storeActionStatus = S.create('actionStatus');
		
		return {
			title: '任务明细',
			allowDefaultButtons: false,
			
			actions: [{
				iconCls: 'log',
				tooltip: '日志',
				width: 40,
				handler: this.viewLog,
				scope: this
			}],
			
			columns: [{
				header: '网关机',
				dataIndex: 'gateway',
				sortable: true
			}, {
				header: '作业ID',
				dataIndex: 'jobId',
				sortable: true
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 400,
				sortable: true
			}, {
				header: '作业状态',
				dataIndex: 'actionStatus',
				sortable: true,
				renderer: function(value) {
					return storeActionStatus.queryUnique('value', value).get('name');
				}
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				width: 80,
				dateColumn: true,
				sortable: true
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'jobType',
				width: 250,
				sortable: true,
				store: S.create('jobType')
			}, {
				xtype: 'customcolumn',
				header: '任务标志',
				dataIndex: 'flag',
				width: 60,
				store: S.create('taskFlag'),
				sortable: true
			}, {
				xtype: 'customcolumn',
				header: '操作人',
				dataIndex: 'operator',
				width: 80,
				sortable: true
			}, {
				xtype: 'customcolumn',
				header: '开始时间',
				dataIndex: 'startTime',
				width: 130,
				dateTimeColumn: true,
				sortable: true
			}, {
				xtype: 'customcolumn',
				header: '结束时间',
				dataIndex: 'endTime',
				width: 130,
				dateTimeColumn: true,
				sortable: true
			}, {
				header: '运行时长(分钟)',
				dataIndex: 'runTime',
				sortable: true
			}],
			
			listeners: {
				rowcontextmenu: this.onRowContextMenu,
				scope: this
			}
		};
	},
	
	initActionMenu: function() {
		this.actionMenu = new Ext.menu.Menu({
			items: [{
				name: 'viewLog',
				text: '查看执行日志',
				iconCls: 'log'
			}],
			
			listeners: {
				beforeshow: this.onActionMenuBeforeShow,
				itemclick: this.onActionMenuItemClick,
				scope: this
			}
		});
	},
	
	viewLog: function(record) {
		if (record instanceof Ext.data.Record) {
			record = record.data;
		}
		
		var actionId = record.actionId;
		
		framework.createWindow({
			title: '查看来自 "' + record.gateway + '" 网关机的日志信息',
			iconCls: 'log',
			
			module: {
				module: 'com.sw.bi.scheduler.action.ActionLogModule',
				actionId: actionId
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
	},
	
	onBeforeLoad: function(store, options) {
		var condition = this.getCondition(),
			startDate = Date.parseDate(condition.taskDateStart, 'Y-m-d'),
			endDate = Date.parseDate(condition.taskDateEnd, 'Y-m-d');
		
		if (Ext.isEmpty(condition.jobId, false) && !Ext.isEmpty(condition.taskDateStart, false) && !Ext.isEmpty(condition.taskDateEnd, false)) {
			if (startDate > endDate) {
				Ext.Msg.alert('提示', '开始日期必须小于结束日期!');
				return false;
			}
			
			var minStartDate = endDate.add(Date.MONTH, -3);
			if (startDate < minStartDate) {
				Ext.Msg.alert('提示', '只允许查询三个月以内的作业!');
				return false;
			}
		}
		
		return com.sw.bi.scheduler.task.TaskListModule.superclass.onBeforeLoad.apply(this, arguments);
	},
	
	onLoadDataComplete: function() {
		this.setCondition({taskId: null});
	},
	
	onScanDateCheck: function(chk, checked) {
		var mdl = this,
		
			fldStart = mdl.findField('taskDateStart'), // Ext.getCmp('taskDateStartId'),
			fldEnd = mdl.findField('taskDateEnd'), // Ext.getCmp('taskDateEndId'),
			fldJobId = mdl.findField('jobId'); // Ext.getCmp('jobIdId');
			
		if (checked) {
			fldStart.allowBlank = true;
			fldEnd.allowBlank = true;
			
			fldStart.setValue(null);
			fldEnd.setValue(null);
		} else {
			fldStart.allowBlank = !Ext.isEmpty(fldJobId.getValue(), false);
			fldEnd.allowBlank = !Ext.isEmpty(fldJobId.getValue(), false);
			
			fldStart.setValue(new Date());
			fldEnd.setValue(new Date());
		}
	},
	
	onRowContextMenu: function(grid, row, e) {
		e.stopEvent();
		
		grid.getSelectionModel().selectRow(row, false);
		
		var mdl = this,
			menu = mdl.actionMenu,
			
			record = grid.store.getAt(row);
			
		if (record == null) {
			return;
		}
		
		menu.currentTask = record.data;
		menu.from = grid;
		
		menu.showAt(e.getXY());
	},
	
	onActionMenuBeforeShow: function(menu) {
		var mdl = this,
		
			mnuViewLog = menu.getComponent(0);
			
		
		menu.items.each(function(item) {
			item.hide();
		});
		
		mnuViewLog.show();
	},
	
	onActionMenuItemClick: function(item, e) {
		var parent = item.ownerCt,
			task = null;

		while (parent) {
			task = parent.currentTask;
			if (!Ext.isEmpty(task)) {
				break;
			}
			
			parent = parent.parentMenu;
		}

		if (Ext.isEmpty(task)) {
			return;
		}
		
		var mdl = this,
			itemId = item.name;
			
		if (itemId == 'viewLog') {
			mdl.viewLog(task);
		}
	},
	//add by zhoushasha  2016/5/5
	onBeforeLoad: function(store, options) {
		var mdl = this,
		condition = mdl.getCondition();
	mdl.fireEvent('beforeload', mdl, condition, store, options);
	var bp = store.baseParams || {};
	console.dir(bp);
	bp.condition = Ext.apply({}, condition);
	console.dir(bp.condition);
	bp.condition.userGroupId=USER_GROUP_ID;
	console.dir(bp.condition);
},
});

Ext.reg('actionmodule', com.sw.bi.scheduler.action.ActionModule);