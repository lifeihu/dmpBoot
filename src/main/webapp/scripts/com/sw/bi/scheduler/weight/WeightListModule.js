_package('com.sw.bi.scheduler.weight');

com.sw.bi.scheduler.weight.WeightListModule = Ext.extend(framework.core.Module, {
	
	center: function() {
		return {
			xtype: 'grid',
			border: false,
			
			actions: [{
				iconCls: 'weight',
				tooltip: '加权重',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (FOREGROUND_TASK_STATUS[record.get('taskStatus')] != '未运行') {
						cc.setCellLink(row, col, false);
					}
					
					return newValue;
				},
				handler: this.weightingTaskFlag2,
				scope: this
			}],
			
			columns: [new Ext.grid.RowNumberer(), {
				header: '作业ID',
				dataIndex: 'jobId',
				width: 60
			}, {
				xtype: 'customcolumn',
				header: '作业名称',
				dataIndex: 'name',
				width: 400,
				renderer: this.taskNameRenderer.createDelegate(this),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '任务状态',
				dataIndex: 'taskStatus',
				width: 60,
				renderer: this.taskStatusRenderer.createDelegate(this),
				scope: this
			}, {
				header: '权重',
				dataIndex: 'flag2',
				width: 40
			}],
			
			store: new Ext.data.Store({
				proxy: new Ext.data.MemoryProxy(),
				reader: new Ext.data.JsonReader({
					fields: ['taskId', 'jobId', 'taskName', 'name', 'taskStatus', 'settingTime', 'taskDate', 'flag2']
				})
			})
		};
	},
	
	south: function() {
		var mdl = this,
		
			smRefer = new Ext.grid.CheckboxSelectionModel(),
			smTriggered = new Ext.grid.CheckboxSelectionModel(),
			
			storeTriggered = new Ext.data.JsonStore({
				url: 'task/paging',
				totalProperty: 'total',
				root: 'paginationResults',
				displayInfo: true,
				remoteSort: true,
				fields: ['taskId', 'jobId', 'name', 'jobName', 'taskStatus', 'settingTime', 'taskDate', 'readyTime'],
				baseParams: {
					start: 0,
					limit: 50,
					condition: Ext.encode({
						'taskStatus': 4, // 已触发/重做已触发
						'useScanDate': 'on' // 表示只查scanDate是当天的作业
						/*'scanDateStart': new Date().add(Date.DAY, -200).format('Y-m-d'),
						'scanDateEnd': new Date().format('Y-m-d')*/
					})
				},
				sortInfo: {
					field: 'readyTime',
					direction: 'asc'
				}
			}),
			
			triggeredTbarItems = [];
			
		// 增加模拟后台
		if (USER_IS_ADMINISTRTOR || USER_IS_ETL) {
			var gateways = framework.syncRequest({
				url: 'gateway/list',
				params: {
					sort: 'master',
					dir: 'desc',
					condition: Ext.encode({
						'status-eq': 1
					})
				},
				decode: true
			}),
			menuItems = [];
			
			Ext.iterate(gateways, function(gateway) {
				if (gateway.master === true) {
					menuItems.push({
						text: gateway.name
					});
				}
			});
			
			triggeredTbarItems.push({
				text: '模拟后台',
				tooltip: '模拟后台',
				menu: {
					items: menuItems,
					listeners: {
						click: function(menu, item) {
							var pnlTriggered = mdl.southPnl.getComponent(1),
								selections = pnlTriggered.getSelectionModel().getSelections();
								
							com.sw.bi.scheduler.task.TaskModule.prototype.simulateSchedule.call(this, item.text, selections);
						}
					}
				}
			});
		}
		
		return {
			xtype: 'panel',
			layout: 'border',
			height: 400,
			split: true,
			
			items: [{
				columnWidth: .5,
				region: 'center',
				xtype: 'editorgrid',
				height: 400,
				// width: 600,
				clicksToEdit: 1,
				title: '参考点任务',
				style: 'margin: 1px;',
				
				tbar: [{
					text: '分析未运行原因',
					handler: mdl.analyseUnrunningTasksByReferPoint,
					scope: mdl
				}],
				
				actions: [{
					iconCls: 'weight',
					tooltip: '增加权重',
					renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
						if (Ext.isEmpty(record.get('jobId'), false)) {
							cc.setCellLink(row, col, false);
						}
						
						return newValue;
					},
					handler: this.weightingReference,
					scope: this
				}, {
					iconCls: 'remove',
					tooltip: '删除参考点',
					renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
						if (Ext.isEmpty(record.get('jobId'), false)) {
							cc.setCellLink(row, col, true);
							
						} else {
							if (record.get('scanDate') == record.get('taskDate')) {
								cc.setCellLink(row, col, false);
							}
						}
						
						return newValue;
					},
					handler: this.isAllowRemoveReference,
					scope: this
				}],
				
				sm: smRefer, // new Ext.grid.RowSelectionModel({singleSelect: true}),
				
				columns: [smRefer, new Ext.grid.RowNumberer({width: 50}), {
					xtype: 'customcolumn',
					header: '作业ID',
					dataIndex: 'jobId',
					width: 60,
					tooltip: {
						qtitle: ' ',
						qtip: '<b>任务ID: </b>{taskId}</br><b>作业名称: </b>{taskName}'
					},
					renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
						mdl.highlightInitializeRenderer(newValue, value, meta, record, row, col, store, grid, cc);
						
						if (Ext.isEmpty(record.get('jobId'), false)) {
							meta.style += 'color:red;';
							newValue = -1;
						}
						
						return newValue;
					},
					scope: this
				}, {
					header: '串行',
					dataIndex: 'serial',
					renderer: function(value) {
						return value ? '串' : '';
					}
				}, {
					xtype: 'customcolumn',
					header: '扫描日期',
					dataIndex: 'scanDate',
					width: 150,
					renderer: this.highlightInitializeRenderer.createDelegate(this),
					scope: this,
					dateColumn: true
				}, {
					xtype: 'customcolumn',
					header: '任务日期',
					dataIndex: 'taskDate',
					width: 150,
					renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
						mdl.highlightInitializeRenderer(newValue, value, meta, record, row, col, store, grid, cc);
						
						var scanDate = Date.parseDate(record.get('scanDate'), 'Y-m-d'),
							taskDate = Date.parseDate(record.get('taskDate'), 'Y-m-d');
							
						if (taskDate.getTime() < scanDate.getTime()) {
							meta.style += 'color:red;';
						}
						
						return newValue;
					},
					scope: this,
					dateColumn: true
				}, {
					xtype: 'customcolumn',
					header: '权重',
					dataIndex: 'flag',
					width: 50,
					renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
						mdl.highlightInitializeRenderer(newValue, value, meta, record, row, col, store, grid, cc);
						
						if (Ext.isEmpty(record.get('jobId'), false)) {
							cc.setCellLink(row, col, false);
						}
						
						return newValue;
					},
					handler: this.weightingReference,
					scope: this
				}, {
					xtype: 'customcolumn',
					header: '子作业',
					dataIndex: 'jobIds',
					width: 260,
					tooltip: true,
					editor: {
						xtype: 'combo',
						listWidth: 500,
						store: new Ext.data.Store({
							proxy: new Ext.data.MemoryProxy(),
							reader: new Ext.data.ArrayReader({
								fields: ['childrenJobId', 'childrenJobName', 'jobType']
							})
						}),
						listeners: {
							beforeshow: this.onChildrenJobBeforeShow,
							beforeselect: this.onChildrenJobBeforeSelect,
							scope: this
						}
					},
					renderer: this.childrenJobRenderer.createDelegate(this),
					scope: this
				}, {
					xtype: 'customcolumn',
					header: '创建时间',
					dataIndex: 'createTime',
					dateTimeColumn: true
				}, {
					xtype: 'customcolumn',
					header: '最近扫描时间',
					dataIndex: 'scanTime',
					dateTimeColumn: true
				}, {
					xtype: 'customcolumn',
					header: '扫描次数',
					dataIndex: 'scanTimes'
				}],
				
				store: new Ext.data.Store({
					proxy: new Ext.data.MemoryProxy(),
					reader: new Ext.data.JsonReader({
						fields: ['waitUpdateStatusTaskId', 'taskName', 'jobId', 'taskId', 'scanDate', 'taskDate', 'flag', 'jobId', 'childrenJobs', 'createTime', 'scanTime', 'scanTimes', 'serial']
					})
				})
			}, {
				columnWidth: .5,
				region: 'east',
				xtype: 'grid',
				height: 500,
				width: 550,
				title: '已触发任务',
				style: 'margin: 1px;',
				
				tbar: triggeredTbarItems,
				
				bbar: [{
					xtype: 'paging',
					pageSize: 50,
					store: storeTriggered
				}],
				
				actions: [{
					iconCls: 'weight',
					tooltip: '增加权重',
					handler: this.weightingTaskReadyTime,
					scope: this
				}],
				
				sm: smTriggered,
				
				columns: [smTriggered, new Ext.grid.RowNumberer({width: 60}), {
					xtype: 'customcolumn',
					header: '作业ID',
					dataIndex: 'jobId',
					width: 60,
					renderer: this.highlightTriggeredRenderer.createDelegate(this),
					handler: this.weightingTaskReadyTime,
					scope: this
				}, {
					header: '尾号',
					dataIndex: 'taskId',
					width: 40,
					renderer: function(value, meta, record) {
						var taskId = String(value);
						return taskId.charAt(taskId.length - 1);
					}
				}, {
					xtype: 'customcolumn',
					header: '作业名称',
					dataIndex: 'name',
					width: 400,
					renderer: this.taskNameRenderer.createDelegate(this),
					scope: this
				}, {
					xtype: 'customcolumn',
					header: '任务日期',
					dataIndex: 'taskDate',
					dateColumn: true,
					renderer: this.highlightTriggeredRenderer.createDelegate(this),
					scope: this
				}, {
					xtype: 'customcolumn',
					header: '任务状态',
					dataIndex: 'taskStatus',
					width: 60,
					renderer: this.taskStatusRenderer.createDelegate(this),
					scope: this
				}, {
					xtype: 'customcolumn',
					header: '已触发时间',
					dataIndex: 'readyTime',
					dateTimeColumn: true,
					width: 130,
					renderer: this.highlightTriggeredRenderer.createDelegate(this),
					scope: this
				}],
				
				store: storeTriggered
			}]
		};
	},
	
	loadData: function(params) {
		if (Ext.isEmpty(params, false)) return;
		
		var mdl = this,
		
			storeOther = mdl.centerPnl.store,
			storeInitialize = mdl.southPnl.getComponent(0).store,
			storeTriggered = mdl.southPnl.getComponent(1).store;
			
		mdl.loadMask.msg = '正在查询数据,请耐心等候...';
		mdl.loadMask.show();
			
		storeOther.removeAll();
		storeInitialize.removeAll();
		storeTriggered.removeAll();
		
		Ext.Ajax.request({
			url: 'weight/getReferenceOrTriggeredTasks',
			params: params,
			
			success: function(response) {
				var result = Ext.decode(response.responseText);
				
				mdl.initializeTaskIds = result['initializeTaskIds'];
				mdl.triggeredTaskIds = result['triggeredTaskIds'];
				
				storeOther.loadData(result['otherStatusTasks'] || []);
				storeInitialize.loadData(result['referenceTasks'] || []);
				storeTriggered.load({params: {start: 0}});
				
				mdl.loadMask.hide();
			}
		});
	},
	
	/**
	 * 渲染任务名称
	 * @param {} value
	 * @param {} meta
	 * @param {} record
	 * @return {}
	 */
	taskNameRenderer: function(newValue, value, meta, record) {
		var settingTime = record.get('settingTime');
							
		if (!Ext.isEmpty(settingTime, false)) {
			meta.tooltip = {
				qtitle: ' ',
				qtip: '<b>预设时间: </b>' + settingTime
			}
		}
		
		this.highlightTriggeredRenderer(newValue, value, meta, record);
		
		return value;
	},
	
	/**
	 * 渲染任务状态
	 */
	taskStatusRenderer: function(newValue, value, meta, record) {
		var foregroundStatus = FOREGROUND_TASK_STATUS[value];

		var qtip = [];
		qtip.push('<div>前台状态: ', foregroundStatus, '</div>');
		qtip.push('<div>后台状态: ', BACKGROUND_TASK_STATUS[value], '</div>');
		
		meta.attr += ' ext:qtitle="作业状态" ext:qtip="' + qtip.join('') + '"';
		
		this.highlightTriggeredRenderer(newValue, value, meta, record);
		
		return foregroundStatus;
	},
	
	/**
	 * 子作业清单渲染
	 * @param {} newValue
	 * @param {} value
	 * @param {} meta
	 * @param {} record
	 */
	childrenJobRenderer: function(newValue, value, meta, record) {
		var jobId = this.initializeTaskIds[record.get('taskId')],
			childrenJobs = Ext.isEmpty(record.get('childrenJobs'), false) ? [] : record.get('childrenJobs');
		
		if (!Ext.isEmpty(jobId, false)) {
			meta.style += 'background: #76ae00;color:white;';
			record.data.jobId = jobId;
		}
			
		return '子作业清单(' + childrenJobs.length + '个)';
	},
	
	highlightInitializeRenderer: function(newValue, value, meta, record) {
		var jobId = this.initializeTaskIds[record.get('taskId')];
		if (!Ext.isEmpty(jobId, false)) {
			meta.style += 'background: #76ae00;color:white;';
			record.data.jobId = jobId;
		}
		
		return newValue;
	},
	
	highlightTriggeredRenderer: function(newValue, value, meta, record) {
		if (Ext.isEmpty(this.triggeredTaskIds)) {
			return newValue;
		}
		
		// var jobId = this.triggeredTaskIds && this.triggeredTaskIds[record.get('taskId')];
		if (this.triggeredTaskIds.indexOf(record.get('taskId')) > -1) {
			meta.style += 'background: orange;color:white;';
			// record.data.jobId = jobId;
		}
		
		return value;
	},
	
	weightingReference: function(record) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '是否执行加权操作?', function(btn) {
			if (btn != 'yes') return;

			Ext.Ajax.request({
				url: 'weight/weightingReference',
				params: {
					waitUpdateStatusTaskId: record.get('waitUpdateStatusTaskId'),
					jobId: record.get('jobId')
				},
				waitMsg: '正在对参考点进行加权操作,请耐心等候...',
				
				success: function() {
					mdl.ownerCt.loadData();
					
					Ext.Msg.alert('提示', '成功对参考点进行加权.');
				}
			});
		});
	},
	
	weightingTaskFlag2: function(record) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '是否执行加权操作?', function(btn) {
			if (btn != 'yes') return;

			Ext.Ajax.request({
				url: 'weight/weightingTaskFlag2',
				params: {
					taskId: record.get('taskId')
				},
				waitMsg: '正在对任务进行加权操作,请耐心等候...',
				
				success: function() {
					record.set('flag2', 5);
					Ext.Msg.alert('提示', '成功对任务进行加权.');
				}
			});
		});
	},
	
	weightingTaskReadyTime: function(record) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '是否执行加权操作?', function(btn) {
			if (btn != 'yes') return;

			Ext.Ajax.request({
				url: 'weight/weightingTaskReadyTime',
				params: {
					taskId: record.get('taskId')
				},
				waitMsg: '正在对任务进行加权操作,请耐心等候...',
				
				success: function() {
					mdl.southPnl.getComponent(1).store.load({params: {start: 0}});
					
					Ext.Msg.alert('提示', '成功对任务进行加权.');
				}
			});
		});
	},
	
	/**
	 * 是否允许删除指定参考点
	 * @param {} record
	 */
	isAllowRemoveReference: function(record) {
		var mdl = this,
			waitUpdateStatusTaskId = record.get('waitUpdateStatusTaskId'),
		
			isAllowRemove = Ext.isEmpty(record.get('jobId'), false) ? true : framework.syncRequest({
				decode: true,
				url: 'waitUpdateStatusTask/isAllowRemove',
				params: {waitUpdateStatusTaskId: waitUpdateStatusTaskId}
			});
			
		if (isAllowRemove) {
			Ext.Msg.confirm('提示', '是否删除指定参考点?', function(btn) {
				if (btn != 'yes') return;
				
				mdl.removeReference(waitUpdateStatusTaskId);
			});
			
		} else {
			Ext.Msg.confirm('提示', '需要被删除的参考点有未执行完成的子任务,是否继续删除?', function(btn) {
				if (btn != 'yes') return;
				
				mdl.removeReference(waitUpdateStatusTaskId);
			});
		}
	},
	
	/**
	 * 删除参考点任务
	 * @param {Number} waitUpdateStatusTaskId
	 */
	removeReference: function(waitUpdateStatusTaskId) {
		var mdl = this;
		
		Ext.Ajax.request({
			url: 'waitUpdateStatusTask/remove',
			params: {
				id: waitUpdateStatusTaskId
			},
			
			success: function(response) {
				mdl.ownerCt.loadData();
				Ext.Msg.alert('提示', '成功删除参考点.');
			}
		});
	},
	
	analyseUnrunningTasksByReferPoint: function() {
		var mdl = this,
			selections = mdl.southPnl.getComponent(0).getSelectionModel().getSelections(),
			referTaskIds = [];

		if (selections.length == 0) {
			Ext.Msg.alert('提示', '请选择需要分析的参考点.');
			return;
		}

		for (var i = 0; i < selections.length; i++) {
			var record = selections[i];
			referTaskIds.push(record.get('taskId'));
		}
		
		Ext.Msg.confirm('提示', '是否需要分析选取的参考点?', function(btn) {
			if (btn !== 'yes') return;
			
			var win = mdl.createWindow({
				title: '任务未运行原因分析',
				
				module: {
					module: 'com.sw.bi.scheduler.task.UnrunningTaskAnalyseModule',
					
					referTaskIds: referTaskIds
				}
				
			}, 'framework.widgets.window.ModuleWindow').open();
		});
	},
	
	onChildrenJobBeforeShow: function(combo) {
		var record = combo.record,
			childrenJobs = Ext.isEmpty(record.get('childrenJobs'), false) ? [] : record.get('childrenJobs'),
			
			data = [];
			
		Ext.iterate(childrenJobs, function(childrenJob) {
			data.push([childrenJob.jobId, childrenJob.jobId + ' - ' + childrenJob.jobName, childrenJob.jobType]);
		});

		combo.store.loadData(data);
	},
	
	onChildrenJobBeforeSelect: function(combo, record) {
		var jobId = record.get('childrenJobId');
		
		if (!Ext.isEmpty(jobId, false)) {
			this.createWindow({
				title: '作业查看',
				
				module: {
					module: S.JOB_MAINTAIN_MODULES(record.get('jobType')),
					buttonSaveHidden: true
				}
			}, 'framework.widgets.window.MaintainWindow').viewOnly({'jobId': jobId});
		}
	},
	
	doLayout: function(mdl) {
		var mdl = this,
		
			grdOther = mdl.centerPnl,
			grdInitialize = mdl.southPnl.getComponent(0),
			grdTriggered = mdl.southPnl.getComponent(1),
			
			size = mdl.body.getSize(true);

		grdTriggered.setWidth(size.width / 2);
		/*grdInitialize.setHeight(height);
		grdTriggered.setHeight(height);
		mdl.southPnl.setHeight(height);*/
		mdl.southPnl.doLayout();
	}
	
});

Ext.reg('weightlistmodule', com.sw.bi.scheduler.weight.WeightListModule);