_package('com.sw.bi.scheduler.task');

com.sw.bi.scheduler.task.TaskCreateModule = Ext.extend(framework.core.Module, {
	
	north: function() {
		return {
			xtype: 'form',
			layout: 'column',
			margins: '0 0 2 0',
			frame: true,
			plain: true,
			border: false,
			
			items: [{
				columnWidth: .2,
				layout: 'form',
				border: false,
				labelWidth: 60,
				items: {
					xtype: 'datefield',
					fieldLabel: '任务日期',
					name: 'taskDate',
					allowBlank: false,
					value: new Date()
				}
			}, {
				columnWidth: .35,
				layout: 'form',
				border: false,
				labelWidth: 60,
				items: {
					xtype: 'textfield',
					fieldLabel: '作业ID',
					name: 'jobId',
					vtype: 'mutilnum'
				}
			}, {
				columnWidth: .03,
				items: {
					xtype: 'button',
					iconCls: 'ok',
					tooltip: '创建任务',
					handler: this.allowCreateTasks,
					scope: this
				}
			}, {
				columnWidth: .03,
				items: {
					xtype: 'button',
					iconCls: 'online',
					tooltip: '作业批量上线',
					handler: this.batchOnline,
					scope: this
				}
			}, {
				columnWidth: .03,
				items: {
					xtype: 'button',
					iconCls: 'offline',
					tooltip: '作业批量下线',
					handler: this.batchOffline,
					scope: this
				}
			}, {
				xtype: 'label',
				text: '注意: 此功能为系统管理员功能，请勿随意使用。使用不当，可能造成调度异常!',
				style: 'color:red;'
			}]
		};
	},
	
	center: function() {
		return {
			xtype: 'grid',

			columns: [new Ext.grid.RowNumberer(), {
				header: '作业ID',
				dataIndex: 'jobId'
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 400,
				sortable: false,
				renderer: function(value, meta, record) {
					var settingTime = record.get('settingTime');
						
					if (!Ext.isEmpty(settingTime, false)) {
						meta.tooltip = {
							qtitle: ' ',
							qtip: '<b>预设时间: </b>' + settingTime
						}
					}
					
					return value;
				}
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				width: 50,
				sortable: false,
				store: S.create('jobCycleType')
			}, {
				header: '业务组',
				dataIndex: 'jobBusinessGroup',
				sortable: false,
				width: 120
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				store: S.create('users'),
				sortable: false,
				width: 80
			}, {
				header: '任务状态',
				dataIndex: 'taskStatus',
				width: 60,
				sortable: false,
				renderer: this.taskStatusRenderer.createDelegate(this),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'jobType',
				width: 250,
				sortable: false,
				store: S.create('jobType')
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				width: 80,
				sortable: false,
				dateColumn: true
			}],
			
			store: new Ext.data.Store({
				proxy: new Ext.data.MemoryProxy(),
				reader: new Ext.data.JsonReader({
					fields: [
						'jobId', 'jobName', 'settingTime', 'cycleType', 'jobBusinessGroup',
						'dutyOfficer', 'taskStatus', 'jobType', 'taskDate', 'merge'
					]
				})
			})
		};
	},
	
	allowCreateTasks: function() {
		var mdl = this;
		
		Ext.Ajax.request({
			url: 'job/allowCreateTasks',
			params: this.northPnl.form.getValues(),
			
			success: function(response) {
				var allowCreateTasks = Ext.decode(response.responseText);
				
				if (allowCreateTasks) {
					Ext.Msg.confirm('提示', '指定的任务日期或作业ID中已经存在任务,是否继续创建?', function(btn) {
						if (btn != 'yes') return;
						
						mdl.createTasks();
					});
				} else {
					Ext.Msg.confirm('提示', '是否需要根据指定任务日期和作业ID生成任务?', function(btn) {
						if (btn != 'yes') return;
						
						mdl.createTasks();
					});
				}
			}
		});
	},
	
	createTasks: function() {
		var mdl = this,
			params = this.northPnl.form.getValues();
		
		Ext.Ajax.request({
			url: 'job/createTasks',
			params: params,
			waitMsg: '正在生成任务,请耐心等候...',
			
			success: function(response) {
				var paging = Ext.decode(response.responseText),
					store = mdl.centerPnl.store;
				
				if (Ext.isEmpty(params.jobId, false)) {
					// 按任务日期生成任务时由于任务太多所以只显示一个总数
					store.removeAll();
					Ext.Msg.alert('提示', '成功生成 ' + paging.total + ' 个任务.');
					
				} else {
					store.loadData(paging.paginationResults, false);
				}
			}
		});
	},
	
	/**
	 * 渲染任务状态
	 */
	taskStatusRenderer: function(value, meta, record) {
		var foregroundStatus = FOREGROUND_TASK_STATUS[value];

		var qtip = [];
		qtip.push('<div>前台状态: ', foregroundStatus, '</div>');
		qtip.push('<div>后台状态: ', BACKGROUND_TASK_STATUS[value], '</div>');
		
		meta.attr += ' ext:qtitle="作业状态" ext:qtip="' + qtip.join('') + '"';
		
		return foregroundStatus;
	},
	
	batchOnline: function() {
		Ext.Ajax.request({
			url: 'job/batchOnline',
			params: this.northPnl.form.getValues(),
			
			success: function() {
				Ext.Msg.alert('提示', '批量上线成功.');
			}
		})
	},
	
	batchOffline: function() {
		var params = this.northPnl.form.getValues();
		params.jobIds = params.jobId;
		params.downMan = USER_ID;
		params.downTime = new Date().format('Y-n-j H:i:s');
		params.downReason = '批量下线';
		
		Ext.Ajax.request({
			url: 'job/offline',
			params: params,
			
			success: function() {
				Ext.Msg.alert('提示', '批量下线成功.');
			}
		})
	},
	
	onModuleRender: function(mdl) {
		mdl.setDefaultAction(mdl.allowCreateTasks, mdl);
		com.sw.bi.scheduler.task.TaskCreateModule.superclass.onModuleRender.call(this, mdl);
	}
	
});