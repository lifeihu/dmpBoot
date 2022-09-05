_package('com.sw.bi.scheduler.task');

/**
 * 作业重跑或补数据
 * @class com.sw.bi.scheduler.task.TaskRedoSupplyModule
 * @extends framework.core.Module
 */
com.sw.bi.scheduler.task.TaskRedoSupplyModule = Ext.extend(framework.core.Module, {
	
	/**
	 * 操作类型
	 * @type String
	 * @description redo: 重跑, supply: 补数据
	 */
	actionType: 'redo',
	
	/**
	 * @cfg task
	 * @type Object
	 */
	task: null,
	
	/**
	 * @cfg containChildren
	 * @type Boolean
	 * @description 包含子作业
	 */
	containChildren: false,
	
	prevJobMapping: {},
	
	initModule: function() {
		// 获得所有配置的前置作业ID
		var prevJobs = framework.syncRequest({
			url: 'job/getOnlinePrevJobIds',
			decode: true
		});

		for (var i = 0; i < prevJobs.length; i++) {
			this.prevJobMapping[prevJobs[i]] = true;
		}
		
		if (this.actionType == 'redo') {
			this.buttons = [{
				text: '重跑',
				iconCls: 'redo',
				handler: this.doAction,
				scope: this
			}, {
				text: '断点重跑',
				iconCls: 'redo',
				handler: this.doAction.createDelegate(this, [true, false]),
				scope: this
			} ];
			
		} else {
			this.buttons = [{
				text: '并行补数据',
				iconCls: 'supply',
				menu: {
					isSerial: false,
					
					items: [{
						text: '仅校验上层父任务',
						iconCls: 'supply',
						isCascadeValidateParentTask: false
					}, {
						text: '校验所有父任务',
						iconCls: 'supply',
						isCascadeValidateParentTask: true
					}],
					
					listeners: {
						click: this.onSupplyItemClick,
						scope: this
					}
				}
			}, {
				text: '串行补数据',
				iconCls: 'supply',
				menu: {
					isSerial: true,
					
					items: [{
						text: '仅校验上层父任务',
						iconCls: 'supply',
						isCascadeValidateParentTask: false
					}, {
						text: '校验所有父任务',
						iconCls: 'supply',
						isCascadeValidateParentTask: true
					}],
					
					listeners: {
						click: this.onSupplyItemClick,
						scope: this
					}
				}
			}];
			
			/*this.buttons = [{
				text: '并行补数据(校验上层父任务)',
				iconCls: 'supply',
				handler: this.doAction.createDelegate(this, [false, false, false]),
				scope: this
			}, {
				text: '并行补数据(校验所有父任务)',
				iconCls: 'supply',
				handler: this.doAction.createDelegate(this, [false, false, true]),
				scope: this
			}, '-', {
				text: '串行补数据(校验上层父任务)',
				iconCls: 'supply',
				handler: this.doAction.createDelegate(this, [false, true, false]),
				scope: this
			}, {
				text: '串行补数据(校验所有父任务)',
				iconCls: 'supply',
				handler: this.doAction.createDelegate(this, [false, true, true]),
				scope: this
			}];*/
		}
		
		this.addEvents(
			'success'
		);
		
		com.sw.bi.scheduler.task.TaskRedoSupplyModule.superclass.initModule.call(this);
	},
	
	center: function() {
		var items = [{
			xtype: 'hidden', name: 'taskId', value: this.task.taskId
		}, {
			xtype: 'textfield',
			name: 'jobName',
			fieldLabel: '作业名称',
			value: this.task.jobName,
			anchor: '100%',
			readOnly: true
		}];
		
		if (this.actionType == 'supply') {
			items.push({
				xtype: 'datefield',
				fieldLabel: '开始日期',
				name: 'startDate',
				anchor: '100%',
				allowBlank: false,
				value: new Date().add(Date.DAY, -1)
			}, {
				xtype: 'datefield',
				fieldLabel: '结束日期',
				name: 'endDate',
				anchor: '100%',
				allowBlank: false,
				value: new Date().add(Date.DAY, -1)
			});
			
		} else {
			items.push({
				xtype: 'datefield',
				name: 'downTime',
				fieldLabel: '任务日期',
				value: this.task.taskDate,
				anchor: '100%',
				readOnly: true
			});
		}
		
		items.push({
			xtype: 'combo',
			hiddenName: 'nextJob',
			fieldLabel: '有后置作业',
			anchor: '100%',
			store: S.create('yesNo'),
			value: this.prevJobMapping[this.task.jobId] === true,
			readOnly: true
		});
		
		return {
			xtype: 'form',
			labelWidth: 70,
			border: false,
			items: items
		};
	},
	
	south: function() {
		return !this.containChildren ? null : {
			xtype: 'grid',
			title: '"' + this.task.jobName + '" 作业具有以下子作业',
			height: this.actionType == 'redo' ? 459 : 431,
			
			columns: [new Ext.grid.RowNumberer(), {
				header: '作业ID',
				// dataIndex: 'job.jobId',
				dataIndex: 'jobId'/*,
				width: 150*/
			}, {
				header: '作业名称',
				// dataIndex: 'jobName',
				dataIndex: 'name',
				width: 400,
				align: 'left',
				renderer: function(value, meta, record) {
					var settingTime = record.get('settingTime');
					
					meta.attr += ' ext:qtip="<b>预设时间</b>: ' + settingTime + '"';
					
					return value;
				}
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				width: 50,
				store: S.create('jobCycleType')
			}, {
				header: '作业状态',
				dataIndex: 'taskStatus',
				renderer: this.taskStatusRenderer // 这个taskStatusRenderer方法是调用了TaskListModule中的方法
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '用户组',
				dataIndex: 'userGroup.name'
			}],
			
			store: new Ext.data.Store({
				proxy: new Ext.data.MemoryProxy([]),
				reader: new Ext.data.JsonReader({
					fields: ['taskId', 'name', 'jobName', 'jobId',/*'job', 'job.jobId',*/ 'taskStatus', 'taskDate', 'settingTime', 'cycleType', 'merge', 'dutyOfficer', 'userGroup.name']
				})
			}),
			
			listeners: {
				rowdblclick: this.onRowDblClick,
				scope: this
			}
		};
	},
	
	loadData: function() {
		var mdl = this;
		
		if (!mdl.containChildren) return;
		
		Ext.Ajax.request({
			url: 'task/children',
			params: {
				taskId: mdl.task.taskId,
				allowFetchParent: false,
				merge: false
			},
			timeout: 1800000, // 30分钟
			waitMsg: '正在加载子作业,请耐心等候...',
			
			success: function(response) {
				var data = null;
				try {
					data = Ext.decode(response.responseText);
				} catch (e) {
					data = null;
				}
				
				// 获得所有配置的前置作业ID
				/*var prevJobs = framework.syncRequest({
					url: 'job/getOnlinePrevJobIds',
					decode: true
				});
				var prevJobMapping = {};
				for (var i = 0; i < prevJobs.length; i++) {
					prevJobMapping[prevJobs[i]] = true;
				}*/
				
				var prevTasks = [],
					tasks = [],
					
					// 已经加入的子任务ID(用来去重)
					taskIds = {};

				mdl.depthTasks = data;
				Ext.iterate(data, function(taskId) {
					var children = data[taskId];
					for (var i = 0; i < children.length; i++) {
						var child = children[i];
						if (mdl.prevJobMapping[child.jobId] === true) {
							child.name = '<span style="color:red;">[有后置作业] </span>' + child.name;
							prevTasks.push(child);
						} else {
							if (taskIds[child.taskId] !== true) {
								tasks.push(child);
								taskIds[child.taskId] = true;
							}
						}
					}
					// tasks = tasks.concat(data[taskId]);
				});
				tasks = prevTasks.concat(tasks);

				var store = mdl.southPnl.store;
				store.on('load', function() {
					Ext.Msg.hide();
				}, null, {single:true});
				
				store.loadData(tasks);
			}
		});
	},
	
	onModuleRender: function(mdl) {
		mdl.setDefaultAction(mdl.doAction, mdl);
		
		com.sw.bi.scheduler.task.TaskRedoSupplyModule.superclass.onModuleRender.call(this);
	},
	
	onRowDblClick: function(grid, row, e) {
		var mdl = this,
			isRedo = mdl.actionType == 'redo';
		
		Ext.Msg.confirm('提示', '是否取消该作业及其子作业的' + (isRedo ? '重跑' : '补数据') + '?', function(btn) {
			if (btn != 'yes') return;
			
			var store = grid.store,
				record = store.getAt(row);
				
				taskId = record.get('taskId'),
				depthTasks = mdl.depthTasks,
				childrenTaskIds = {},
				
				getChildrenTasks = function(taskId) {
					childrenTaskIds[taskId] = true;
					
					var children = depthTasks[taskId];
					if (children != null && children.length > 0) {
						Ext.iterate(children, function(child) {
							getChildrenTasks(child.taskId);
						});
					}
				};
				
			// 获得指定作业的所有子作业
			getChildrenTasks(taskId);
			
			var removeCount = 0;
			for (var i = store.getCount() - 1; i >= 0; i--) {
				var record = store.getAt(i);
				if (!Ext.isEmpty(record) && childrenTaskIds[record.get('taskId')] === true) {
					store.remove(record);
					removeCount += 1;
				}
			}
			
			Ext.Msg.alert('提示', '共计取消 ' + removeCount + ' 个作业的' + (isRedo ? '重跑' : '补数据') + '.');
		});
	},
	
	/**
	 * 执行重跑或补数据操作
	 * @param {Boolean} allowBreakpoint 允许断点
	 * @param {Boolean} isSerialSupply 是否串行补数据
	 * @param {Boolean} isCascadeValidateParentTask 是否级联校验父任务
	 */
	doAction: function(allowBreakpoint, isSerialSupply, isCascadeValidateParentTask) {
		var mdl = this,
			isRedo = mdl.actionType == 'redo'
			
			form = this.centerPnl.form,
			
			params = {
				operateBy: USER_ID,
				breakpoint: allowBreakpoint === true,
				isSerialSupply: isSerialSupply === true,
				isCascadeValidateParentTask: isCascadeValidateParentTask === true
			};
			
		params['taskId'] = form.findField('taskId').getValue();

		if (!isRedo) {
			if (!form.isValid()) {
				return;
			}
			
			var today = new Date().clearTime().getTime();
			
				start = form.findField('startDate').getValue().clearTime(),
				end = form.findField('endDate').getValue().clearTime(),
			
				startDate = start.getTime(),
				endDate = end.getTime();
				
			if (startDate >= today) {
				Ext.Msg.alert('提示', '开始日期必须小于今天!'	);
				return;
			}
			
			if (endDate > today) {
				Ext.Msg.alert('提示', '结束日期必须小于等于今天!'	);
				return;
			}
			
			if (startDate > endDate) {
				Ext.Msg.alert('提示', '开始日期不能大于结束日期!'	);
				return;
			}
			
			var newEndDate = start.add(Date.YEAR, 1);
			if (endDate > newEndDate.getTime()) {
				Ext.Msg.alert('提示', '补数据的日期范围不允许超过一年.')
				return;
			}
			
			params['startDate'] = start.format('Y-n-j');
			params['endDate'] = end.format('Y-n-j')
		}

		if (!Ext.isEmpty(mdl.southPnl)) {
			var store = mdl.southPnl.store,
				childrenTaskIds = [];
			store.each(function(record) {
				childrenTaskIds.push(record.get('taskId'));
			});
			params.childrenTasks = childrenTaskIds.join(',');
		}
		
		var loadMask = mdl.loadMask;
		loadMask.msg = '正在' + (mdl.actionType == 'redo' ? '重跑作业' : '补作业数据') + ',请耐心等候...';
		loadMask.show();
		
		Ext.Ajax.request({
			timeout: 1200000, // 20分钟
			url: 'task/' + mdl.actionType,
			params: params,
			
			success: function() {
				mdl.fireEvent('success', mdl);
				
				loadMask.hide();
				mdl.moduleWindow.close();
			},
			
			failure: function() {
				loadMask.hide();
			}
		});
	},
	
	onSupplyItemClick: function(menu, item) {
		this.doAction(false, menu.isSerial, item.isCascadeValidateParentTask);
	}
});