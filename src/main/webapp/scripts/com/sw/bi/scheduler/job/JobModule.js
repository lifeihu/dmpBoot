_package('com.sw.bi.scheduler.job');

_import([
	'framework.modules.SearchGridModule',
	'framework.widgets.form.MultiCombo',
	'framework.widgets.window.ChooseWindow'
]);

Ext.QuickTips.init();
com.sw.bi.scheduler.job.JobModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'job',
	actionViewConfig: false,
	actionUpdateConfig: false,
	
	initModule: function() {
		com.sw.bi.scheduler.job.JobModule.superclass.initModule.call(this);
		
		this.actionUpdateConfig = Ext.apply(this.actionUpdateConfig, {
			renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
				if (record.get('jobId') == 1) {
					cc.setCellLink(row, col, false);
					return;
				}
				
				return newValue;
			}
		});
	},
	
	searcher: function() {
		return {
			items: [{
				columnWidth: .12,
				name: 'jobId-in',
				fieldLabel: '作业ID',
				vtype: 'mutilnum'
			}, {
				columnWidth: .15,
				name: 'jobName',
				fieldLabel: '作业名称'
			}, {
				columnWidth: .13,
				name: 'jobBusinessGroup',
				fieldLabel: '业务组'
			}, {
				columnWidth: .13,
				xtype: 'combo',
				hiddenName: 'dutyOfficer-eq',
				fieldLabel: '责任人',
				store: S.create('users')
			}, {
				columnWidth: .12,
				xtype: 'multicombo',
				hiddenName: 'jobStatus-in',
				fieldLabel: '状态',
				store: S.create('jobStatus')
			}, {
				columnWidth: .12,
				xtype: 'multicombo',
				hiddenName: 'cycleType-in',
				fieldLabel: '周期',
				store: S.create('jobCycleType')
			}, {
				columnWidth: .1,
				xtype: 'combo',
				hiddenName: 'alert',
				fieldLabel: '告警',
				store: S.create('alertType')
			}, {
				columnWidth: .12,
				xtype: 'combo',
				hiddenName: 'gateway-eq',
				fieldLabel: '网关机',
				store: S.create('gateways')
			}, {
				columnWidth: .15,
				xtype: 'combo',
				hiddenName: 'userGroupId',
				fieldLabel: '用户组',
				store: S.create('userGroups')
				// store: S.create('userGroupsByUser', USER_ID)
			}, {
				columnWidth: .15,
				xtype: 'multicombo',
				hiddenName: 'jobLevel-in',
				fieldLabel: '优先级',
				store: S.create('jobLevel')
			}, {
				columnWidth: .47,
				xtype: 'multicombo',
				hiddenName: 'jobType-in',
				fieldLabel: '类型',
				store: S.create('jobType')
			}, {
				columnWidth: .1,
				xtype: 'checkbox',
				style: 'height: 100%', //修改处
				name: 'prevJobs-nnl',
				fieldLabel: '前置作业不为空',
				checked: false
			}]
		};
	},
	
	detailer: function() {
		return {
			allowDefaultButtons: false,
			columns: [{
				header: '作业ID',
				dataIndex: 'jobId',
				sortable: true,
				width: 80
			}, {
				xtype: 'customcolumn', 
				iconCls: 'remove',
				//tooltip: '删除',
				width: 40,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					meta.attr += ' ext:qtip="删除" ';
					if (!S.IS_AUTHORIZED_USER_GROUP(record.get('userGroup.userGroupId'))) {
						cc.setCellLink(row, col, false);
					}
					return null;
				},
				handler: function(record){
					this.removed(record);
				},
				scope: this
			}, {
				xtype: 'customcolumn',
				iconCls: 'edit',
				//tooltip: '修改',
				handler: 'updateOnly',
				scope: this,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					meta.attr += ' ext:qtip="修改" ';
					if (!S.IS_AUTHORIZED_USER_GROUP(record.get('userGroup.userGroupId'))) {
						cc.setCellLink(row, col, false);
					}
					
					return null;
				}
			}, {
				xtype: 'customcolumn',
				iconCls: 'copy',
				//tooltip: '复制',
				handler: this.copy,
				scope: this,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					meta.attr += ' ext:qtip="复制" ';
					if (!S.IS_AUTHORIZED_USER_GROUP(record.get('userGroup.userGroupId'))) {
						cc.setCellLink(row, col, false);
					}
					
					return null; 
				}
			}, {
				xtype: 'customcolumn',
				iconCls: 'children-job',
				tooltip: '查看作业关系',
				handler: this.viewRelation,
				scope: this,
				renderer: function(newValue, value, meta) { 
					meta.attr += 'ext:qtip="查看作业关系" ';
					return null; 
				}
			}, {
				xtype: 'customcolumn',
				width: 20,
				handler: function(record) {
					this[record.get('jobStatus') == JOB_STATUS.ONLINE ? 'offline' : 'online'](record);
				},
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('jobId') == 1) {
						cc.setCellLink(row, col, false);
						return null;
					}
					
					var	status, action = '上线',
						jobStatus = record.get('jobStatus');
						
					if (jobStatus == JOB_STATUS.UNLINE) {
						meta.css += ' unline';
						
						status = '未上线';
					} else if (jobStatus == JOB_STATUS.ONLINE) {
						meta.css += ' online';
						
						status = '已上线';
						action = '下线';
					} else if (jobStatus == JOB_STATUS.OFFLINE) {
						meta.css += ' offline';
						
						status = '已下线';
					}
					
					if (!S.IS_AUTHORIZED_USER_GROUP(record.get('userGroup.userGroupId'))) {
						meta.attr += ' ext:qtip="状态: ' + status + '"';
						cc.setCellLink(row, col, false);
						
					} else {
						meta.attr += ' ext:qtip="状态: ' + status + '<br>点击后可将作业<b>' + action + '</b>"'; 
					}
					
					return '';
				},
				scope: this
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 370,
				sortable: true,
				align: 'left'
			}, {
				header: '业务组',
				dataIndex: 'jobBusinessGroup',
				width: 100,
				sortable: true
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				width: 100,
				sortable: true,
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '用户组',
				dataIndex: 'userGroup.name'
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				sortable: true,
				width: 50,
				store: S.create('jobCycleType')
			}, {
				header: '启动时间',
				dataIndex: 'jobTime',
				width: 60,
				renderer: function(value) {
					if (Ext.isEmpty(value, false)) {
						return null;
					}
					
					return value.indexOf(':') == -1 ? null : value;
				}
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'jobType',
				sortable: true,
				width: 240,
				store: S.create('jobType')
			}, {
				xtype: 'customcolumn',
				header: '优先级',
				dataIndex: 'jobLevel',
				sortable: true,
				width: 100,
				store: S.create('jobLevel')
			}, {
				xtype: 'customcolumn',
				header: '告警类型',
				sortable: true,
				dataIndex: 'alert',
				store: S.create('alertType')
			}],
			
			store: {
				fields: ['jobStatus', 'userGroup.userGroupId']
			}
		};
	},
	/** 
	 * @description 删除列表中选中的数据
	 */
	removed: function(record) {
		var mdl = this,
			item = mdl.getActiveTab();
		if (record.get('jobStatus') !== JOB_STATUS.OFFLINE){
			Ext.Msg.alert('提示', '该任务未下线，请先将该任务下线');
			return;
		};	
		if (Ext.isEmpty(item)) return;
		
		if (item instanceof Ext.grid.GridPanel) {
			var removeColumn = item.removeColumn || item.gridKeyColumn || mdl.gridKeyColumn,
				//removeColumn = '',
				maintainCfg = mdl.maintain || {},
				gridMaintainCfg = item.maintain || {},
				url = item.removeUrl || gridMaintainCfg.removeUrl || maintainCfg.removeUrl || mdl.removeUrl;

			if (Ext.isEmpty(url, false)) return;
			
			var selections = mdl.getSelections(),
				len = selections.length;
			if (len == 0) return;
			
			var ids = [], params = {};
			Ext.each(selections, function(record) {
				ids.push(record.get(removeColumn));
			});

			if (mdl.fireEvent('beforeremove', mdl, ids, params) !== false) {
				Ext.Msg.confirm('提示', String.format('是否删除选中的 {0} 条记录?', len), function(btn) {
					if (btn != 'yes') return;
					
					params['id'] = ids.join(',');

					var loadMask = mdl.loadMask;
					loadMask.msg = '正在删除数据, 请耐心等候...';
					loadMask.show();
					
					Ext.Ajax.request({
						url: url,
						params: params,
						// waitMsg: '正在删除数据,请耐心等候...',
						
						success: function(response) {
							mdl.onAfterAction();

							mdl.clearStoreData([mdl.model]);
							mdl.fireEvent('removecomplete', mdl, response);
							
							loadMask.hide();
						},
						
						failure: function() {
							//mdl.fireEvent('removefailure', mdl);
							//mdl.loadData();
							//loadMask.show();
							location.reload();
						}
					});
				});
			}
			
		} else if (!Ext.isEmpty(item.removed))
			item.removed();
	},
	/**
	 * 复制作业
	 */
	copy: function(record) {
		this.updateOnly({
			jobId: record.get('jobId'),
			isCopy: true
		});
	},
	
	viewOnly: function(params) {
		var record = this.getSelected(),
			jobType = record.get('jobType'),
			module = S.JOB_MAINTAIN_MODULES(jobType);
		
		this.createWindow({
			title: '作业维护',
			
			module: {
				module: module,
				buttonSaveHidden: true
			}
		}, 'framework.widgets.window.MaintainWindow').viewOnly(params);
	},
	
	updateOnly: function(params) {
		var record = this.getSelected(),
			jobType = record.get('jobType'),
			module = S.JOB_MAINTAIN_MODULES(jobType);
		
		this.createWindow({
			title: '作业维护',
			
			module: {
				module: module,
				buttonSaveHidden: true
			}
		}, 'framework.widgets.window.MaintainWindow').updateOnly(params);
	},
	
	/**
	 * 上线指定作业
	 * @param {Ext.data.Record} record
	 */
	online: function(record) {
		var mdl = this,
		
			jobId = record.get('jobId'),
			jobName = record.get('jobName');
		
		Ext.Msg.confirm('提示', '是否需要将 "' + jobName + '" 作业上线?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'job/online',
				params: {
					jobId: jobId,
					updateBy: USER_ID
				},
				
				waitMsg: '正在上线作业,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '"' + jobName + '" 作业已成功上线!', function() {
						// record.set('jobStatus', JOB_STATUS.ONLINE);
						mdl.loadData();
					});
				},
				
				failure: function() {
					Ext.Msg.hide();
				}
			});
		});
	},
	
	/**
	 * 下线指定作业
	 * @param {Ext.data.Record} record
	 */
	offline: function(record) {
		var mdl = this,
		
			jobId = record.get('jobId'),
			jobName = record.get('jobName');
			
		var win = this.createWindow({
			title: '作业下线',
			width: 700,
			height: 600,
			
			relayEvent: ['success'],
			
			module: {
				module: 'com.sw.bi.scheduler.job.JobOfflineModule',
				job: record.data
			}
		}, 'framework.widgets.window.ModuleWindow');
		
		win.on('success', function() {
			// record.set('jobStatus', JOB_STATUS.OFFLINE);
			mdl.loadData();
		});
		
		win.open();
	},
	
	viewRelation: function(record) {
		framework.createWindow({
			title: '查看作业关系',
			iconCls: 'children-job',
			width: 1020,
			height: 600,
			
			module: {
				module: 'com.sw.bi.scheduler.job.JobChildrenModule',
				jobId: record.get('jobId')
			}
		}, 'framework.widgets.window.ModuleWindow').open();
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
},
});