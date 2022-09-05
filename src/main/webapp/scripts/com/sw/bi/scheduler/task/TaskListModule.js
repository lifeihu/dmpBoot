_package('com.sw.bi.scheduler.task');

_import([
	'framework.modules.SearchGridModule',
	'framework.widgets.form.MultiCombo',
	'framework.widgets.form.DateTimeField'
]);

com.sw.bi.scheduler.task.TaskListModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'task',
	pageSize: 25,
	
	actionUpdateConfig: false,
	actionViewConfig: false,
	
	maintain: {
		title: '作业维护',
		module: 'com.sw.bi.scheduler.task.TaskMaintainModule'
	},
	
	/**
	 * @type com.sw.bi.scheduler.task.TaskModule 
	 */
	module: null,
	
	searcher: function() {
		return {
			items: [{
				xtype: 'hidden', name: 'runTimeStart'
			}, {
				xtype: 'hidden', name: 'runTimeEnd'
			}, {
				xtype: 'hidden', name: 'taskId'
			}, {
				xtype: 'hidden', name: 'settingTimeEnd'
			}, {
				// id: 'jobIdId',
				columnWidth: .22,
				name: 'jobId',
				fieldLabel: '作业ID',
				vtype: 'mutilnum',
				listeners: {
					blur: this.onJobIdBlur,
					scope: this
				}
			}, {
				columnWidth: .22,
				name: 'jobName',
				fieldLabel: '作业名称'
			}, {
				columnWidth: .12,
				xtype: 'multicombo',
				hiddenName: 'cycleType',
				fieldLabel: '作业周期',
				store: S.create('jobCycleType')
			}, {
				columnWidth: .17,
				xtype: 'datetimefield',
				name: 'settingTimeStart',
				fieldLabel: '预设时间',
				format: 'Y-m-d H:i:00',
				listeners: {
					blur: this.onSettingTimeBlur,
					scope: this
				}
			}/*, {
				columnWidth: .12,
				xtype: 'datetimefield',
				name: 'settingTimeEnd',
				fieldLabel: '至',
				format: 'Y-m-d H:i:59',
				listeners: {
					blur: this.onSettingTimeBlur,
					scope: this
				}
			}*/, {
				columnWidth: .12,
				name: 'jobBusinessGroup',
				fieldLabel: '业务组'
			}, {
				columnWidth: .12,
				xtype: 'combo',
				hiddenName: 'dutyOfficer',
				fieldLabel: '责任人',
				store: S.create('users')
			}, {
				columnWidth: .15,
				xtype: 'multicombo',
				hiddenName: 'taskStatus',
				fieldLabel: '作业状态',
				store: S.create('taskStatus')
			}, {
				columnWidth: .2,
				xtype: 'multicombo',
				hiddenName: 'jobLevel',
				fieldLabel: '优先级',
				store: S.create('jobLevel')
			}, {
				// id: 'taskDateStartId',
				columnWidth: .18,
				xtype: 'datefield',
				format: 'Y-m-d',
				name: 'taskDateStart',
				fieldLabel: '任务日期',
				value: new Date(),
				allowBlank: false
			}, {
				// id: 'taskDateEndId',
				columnWidth: .14,
				xtype: 'datefield',
				format: 'Y-m-d',
				name: 'taskDateEnd',
				fieldLabel: '至',
				labelWidth: 10,
				labelSeparator: ' ',
				value: new Date(),
				allowBlank: false
			}, {
				columnWidth: .18,
				xtype: 'datetimefield',
				name: 'taskBeginTime',
				fieldLabel: '运行时间'
			}, {
				columnWidth: .14,
				xtype: 'datetimefield',
				name: 'taskEndTime',
				fieldLabel: '至',
				labelWidth: 10,
				labelSeparator: ' '
			}, {
				columnWidth: .15,
				xtype: 'combo',
				hiddenName: 'userGroupId',
				fieldLabel: '用户组',
				store: S.create('userGroups')
			}, {
				columnWidth: .28,
				xtype: 'multicombo',
				hiddenName: 'jobType',
				fieldLabel: '类型',
				store: S.create('jobType')
			}, {
				// id: 'useScanDateId',
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
		var mdl = this,
			module = mdl.module,
		
			actions = [],
			gateways = framework.syncRequest({
				url: 'gateway/list',
				params: {
					sort: 'master',
					dir: 'desc',
					condition: encodeURIComponent( Ext.encode({
						'status-eq': 1
					}))
//					condition: 'status-eq = 1'
				},
				decode: true
			}),
		
			tbuttons = [];
		
		if (USER_IS_ADMINISTRTOR) {
			// 2014-07-04
			// 调整任务权重的功能被屏蔽
			/*actions.push({
				iconCls: 'weight',
				tooltip: '给指定任务加权,使其有更高的执行权',
				width: 40,
				handler: module.weighting,
				scope: mdl
			});*/
			
			var today = new Date().format('Y-m-d'),
				yesterday = new Date().add(Date.DAY, -1).format('Y-m-d'),
				
				mnuGatewayItems = [{
					text: '所有网关机'
				}, '-'];
			
			Ext.iterate(gateways, function(gateway) {
				mnuGatewayItems.push({
					text: gateway.name
				});
			});
			
			tbuttons.push({
				text: '调度维护',
				tooltip: '调度维护当前只修改状态,不杀进程',
				menu: {
					items: mnuGatewayItems,
					
					listeners: {
						click: function(menu, item) {
							module.killRunningTaskPID(item.text == '所有网关机' ? null : item.text);
						}
					}
				}
			}, '-');
		}
		
		var batchItems = [{
			text: '批量重跑选定作业',
			iconCls: 'redo',
			handler: module.batchRedoOrSupply.createDelegate(module, [true, false]),
			scope: module
		}, {
			text: '批量重跑选定作业及子作业',
			iconCls: 'redo',
			handler: module.batchRedoOrSupply.createDelegate(module, [true, true]),
			scope: module
		}, '-', {
			text: '批量补选定作业数据',
			iconCls: 'supply',
			handler: module.batchRedoOrSupply.createDelegate(module, [false, false]),
			scope: module
		}, {
			text: '批量补选定作业及子作业数据',
			iconCls: 'supply',
			handler: module.batchRedoOrSupply.createDelegate(module, [false, true]),
			scope: module
		},  '-', {
			text: '分析任务未运行原因',
			handler: module.analyseUnrunningTasks.createDelegate(module),
			scope: module
		}];
		
		if (USER_IS_ADMINISTRTOR) {
			batchItems.push('-', {
				text: '批量添加参考点(按扫描日期)',
				handler: module.addReferPointsByScanDate.createDelegate(module),
				scope: module
			}, {
				text: '批量添加参考点(按选取任务)',
				handler: module.addReferPoints.createDelegate(module),
				scope: module
			});
		}
		
		tbuttons.push({
			text: '批量操作',
			menu: {
				items: batchItems
			}
		});
		
		if (USER_IS_ADMINISTRTOR || USER_IS_ETL) {
			var menuItems = [];
			
			Ext.iterate(gateways, function(gateway) {
				if (gateway.master === true) {
					menuItems.push({
						text: gateway.name
					});
				}
			});
			
			tbuttons.push('-', {
				text: '模拟后台',
				tooltip: '模拟后台',
				menu: {
					items: menuItems,
					listeners: {
						click: function(menu, item) {
							module.simulateSchedule(item.text);
						}
					}
				}
			}/* 
			2014-07-04
			现在FTP上文件不会被删除该功能已没有意义
			, '-', {
				text: '恢复FTP文件',
				tooltip: '恢复FTP备份目录下的文件',
				menu: {
					items: [{
						text: new Date().format('Y-m-d')
					}, {
						text: new Date().add(Date.DAY, -1).format('Y-m-d')
					}, {
						text: new Date().add(Date.DAY, -2).format('Y-m-d')
					}, {
						text: new Date().add(Date.DAY, -3).format('Y-m-d')
					}, {
						text: new Date().add(Date.DAY, -4).format('Y-m-d')
					}, {
						text: new Date().add(Date.DAY, -5).format('Y-m-d')
					}, {
						text: new Date().add(Date.DAY, -6).format('Y-m-d')
					}],
					
					listeners: {
						click: function(menu, item) {
							module.restoreBackupFile(item.text);
						}
					}
				}
			}*/);
		}
			
		return {
			title: '任务',
			allowDefaultButtons: false,
			
			tbuttons: tbuttons,			
			actions: actions,
			
			columns: [{
				xtype: 'customcolumn',
				header: '作业ID',
				dataIndex: 'jobId',
				sortable: true,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (!S.IS_AUTHORIZED_USER_GROUP(record.get('userGroup.userGroupId'))) {
						cc.setCellLink(row, col, false);
					}
					
					return newValue; 
				},
				handler: function(record) { this.updateOnly({ taskId: record.get('taskId') }); },
				scope: this
			}, {
				xtype: 'customcolumn',
				iconCls: 'maintain',
				tooltip: '查看执行明细',
				width: 40,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (!module.allowView(record)) {
						cc.setCellLink(row, col, false);
					}
					
					return null;
				},
				handler: module.viewDetail,
				scope: module
			}, {
				xtype: 'customcolumn',
				iconCls: 'log',
				tooltip: '查看执行日志',
				width: 40,
				renderer: function(newValue, oldValue, meta, record, row, col, store, grid, cc) {
					if (!module.allowView(record)) {
						cc.setCellLink(row, col, false);
					}
					
					return null;
				},
				handler: module.viewLog,
				scope: module
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 400,
				sortable: true,
				align: 'left',
				tooltip: {
					qtitle: '预设时间',
					qtip: '{settingTime}'
				}
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				width: 50,
				sortable: true,
				store: S.create('jobCycleType')
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				store: S.create('users'),
				sortable: true,
				width: 80
			}, {
				header: '用户组',
				dataIndex: 'userGroup.name'
			}, {
				header: '作业状态',
				dataIndex: 'taskStatus',
				width: 60,
				sortable: true,
				renderer: this.module.taskStatusRenderer,
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				width: 80,
				sortable: true,
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '预设时间',
				dataIndex: 'settingTime',
				width: 80,
				sortable: true,
				renderer: function(value) {
					return value.substring(value.indexOf(' ') + 1, value.lastIndexOf(':'));
				}
			}, {
				xtype: 'customcolumn',
				header: '开始时间',
				dataIndex: 'taskBeginTime',
				width: 130,
				sortable: true,
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '结束时间',
				dataIndex: 'taskEndTime',
				width: 130,
				sortable: true,
				dateTimeColumn: true
			}, {
				header: '参考运行时长',
				dataIndex: 'referRunTime',
				width: 110,
				sortable: true,
				renderer: function(value) {
					if (!Ext.isEmpty(value, false) && value > 0) {
						return parseInt(value / 1000 / 60, 10);
					}
					
					return value;
				}
			}, {
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
			}, {
				header: '业务组',
				dataIndex: 'jobBusinessGroup',
				sortable: true,
				width: 120
			}, {
				xtype: 'customcolumn',
				header: '优先级',
				dataIndex: 'jobLevel',
				width: 100,
				store: S.create('jobLevel')
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'jobType',
				width: 250,
				sortable: true,
				store: S.create('jobType')
			}],
			
			store: {
				// stype: 'storeTaskListModule'
				fields: ['lastActionId', 'settingTime', 'userGroup', 'userGroup.userGroupId']
			},
			
			listeners: {
				rowcontextmenu: this.onRowContextMenu,
				rowdblclick: this.onRowDblclick,
				scope: this
			}
		};
	},
	
	copyJobIds: function(event, copyMode) {
		var grid = this.getActiveTab(),
			records = null,
			jobIds = [];
			
		if (copyMode == 'all') {
			records = grid.store.getRange();
		} else if (copyMode == 'selected') {
			records = grid.getSelectionModel().getSelections();
		}
		
		Ext.iterate(records, function(record) {
			jobIds.push(record.get('jobId'));
		});
		
		ZeroClipboard.setData('text/plain', jobIds.length == 0 ? ' ' : jobIds.join(','));
	},
	
	//////////////////////////////////////////////////////
	
	onBeforeLoad: function(store, options) {
		var condition = this.getCondition();
		
		// 作业ID不为空时三个月的范围限制不需要
		if (Ext.isEmpty(condition.jobId, false) && !Ext.isEmpty(condition.taskDateStart, false) && !Ext.isEmpty(condition.taskDateEnd, false)) {
			var startDate = Date.parseDate(condition.taskDateStart, 'Y-m-d'),
				endDate = Date.parseDate(condition.taskDateEnd, 'Y-m-d');
		
			if (startDate > endDate) {
				Ext.Msg.alert('提示', '开始日期必须小于结束日期!');
				this.loading = false;
				return false;
			}
			
			var minStartDate = endDate.add(Date.MONTH, -3);
			if (startDate < minStartDate) {
				Ext.Msg.alert('提示', '只允许查询三个月以内的作业!');
				this.loading = false;
				return false;
			}
		}
		
		return com.sw.bi.scheduler.task.TaskListModule.superclass.onBeforeLoad.apply(this, arguments);
	},
	
	onRowContextMenu: function(grid, row, e) {
		e.stopEvent();
		
		grid.getSelectionModel().selectRow(row, false);
		
		var mdl = this,
			menu = mdl.module.taskActionMenu,
			
			record = grid.store.getAt(row);
			
		if (record == null) {
			return;
		}
		
		menu.currentTask = record.data;
		menu.from = grid;
		
		menu.showAt(e.getXY());
	},
	
	onRowDblclick: function(grid, row) {
		var mdl = this,
			record = grid.store.getAt(row);
		
		mdl.module.southPnl.collapse();
		
		(function() {
			mdl.module.centerPnl.getComponent(0).addRootTask(record.data);
		}).defer(500);
	},
	
	onJobIdBlur: function(txt) {
		var mdl = this,
		
			fldStart = mdl.findField('taskDateStart'), // Ext.getCmp('taskDateStartId'),
			fldEnd = mdl.findField('taskDateEnd'), // Ext.getCmp('taskDateEndId'),
			fldUseSacnDate = mdl.findField('useScanDate'); // Ext.getCmp('useScanDateId');
			
		if (Ext.isEmpty(txt.getValue(), false)) {
			fldStart.allowBlank = fldUseSacnDate.getValue();
			fldEnd.allowBlank = fldUseSacnDate.getValue();
		} else {
			fldStart.allowBlank = true;
			fldEnd.allowBlank = true;
		}
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
	
	onSettingTimeBlur: function(fld) {
		var mdl = this,
		
			fldStart = mdl.findField('taskDateStart'), // Ext.getCmp('taskDateStartId'),
			fldEnd = mdl.findField('taskDateEnd'), // Ext.getCmp('taskDateEndId'),
			fldJobId = mdl.findField('jobId'); // Ext.getCmp('jobIdId'),
			fldSettingTimeStart = mdl.findField('settingTimeStart'),
			fldSettingTimeEnd = mdl.findField('settingTimeEnd');

		if (Ext.isEmpty(fld.getValue(), false)) {
			fldStart.allowBlank = !Ext.isEmpty(fldJobId.getValue(), false);
			fldEnd.allowBlank = !Ext.isEmpty(fldJobId.getValue(), false);
			
			fldStart.setValue(new Date());
			fldEnd.setValue(new Date());
		} else {
			fldStart.allowBlank = true;
			fldEnd.allowBlank = true;
			
			fldStart.setValue(null);
			fldEnd.setValue(null);
		}
	},
	
	onCopyMenuRender: function(menu) {
		var mdl = this;
		
		menu.items.each(function(mi) {
			mi.on('render', function(item) {
				var client = new ZeroClipboard(item.el.dom);
				client.on('copy', mdl.copyJobIds.createDelegate(mdl, [item.copyMode], true));
			});
		});
	},
	//add by zhoushasha 2016/5/5
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
	}
	
});

Ext.reg('tasklistmodule', com.sw.bi.scheduler.task.TaskListModule);