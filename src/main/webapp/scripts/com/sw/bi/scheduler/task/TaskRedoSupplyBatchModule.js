_package('com.sw.bi.scheduler.task');

/**
 * 作业批量重跑/补数据
 * @class com.sw.bi.scheduler.task.TaskRedoSupplyBatchModule
 * @extends framework.core.Module
 */
com.sw.bi.scheduler.task.TaskRedoSupplyBatchModule = Ext.extend(framework.core.Module, {
	
	/**
	 * 操作类型
	 * @type String
	 * @description redo: 重跑, supply: 补数据
	 */
	actionType: 'redo',
	
	/**
	 * @cfg tasks
	 * @type Array<Task>
	 */
	tasks: null,
	
	masterTaskMapping: {},
	
	/**
	 * @cfg containChildren
	 * @type Boolean
	 * @description 包含子作业
	 */
	containChildren: false,
	
	/**
	 * 前置作业
	 * @type 
	 */
	prevJobMapping: {},
	
	/**
	 * 已经处理过的任务(用于去重)
	 * @type 
	 */
	processedTasks: {},
	
	initModule: function() {
		// 指定重跑/补数据的作业的所有子作业
		this.depthChildren = {};
		this.prevJobMapping = {};
		this.processedTasks = {};
		this.masterTaskMapping = {};
		
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
			}];
			
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
		
		com.sw.bi.scheduler.task.TaskRedoSupplyBatchModule.superclass.initModule.call(this);
	},

	north: function() {
		var items = [{
			xtype: 'hidden', name: 'taskId'
		}, {
			xtype: 'textarea',
			name: 'jobName',
			fieldLabel: '作业名称',
			anchor: '100%',
			readOnly: true,
			height: this.actionType == 'redo' ? 190 : 140
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
			
		}
		
		return {
			xtype: 'form',
			height: 200,
			labelWidth: 70,
			border: false,
			items: items
		};
	},
	
	/**
	 * 创建子任务列表
	 * @return {}
	 */
	center: function() {
		return {
			xtype: 'grid',
			title: '子作业清单',
			
			columns: [new Ext.grid.RowNumberer(), {
				header: '主作业',
				dataIndex: 'masterTasks',
				renderer: function(value) {
					var masterJobIds = [];
					Ext.iterate(value, function(masterTask) {
						masterJobIds.push(masterTask.jobId)
					});
					
					return masterJobIds.join(',');
				}
			}, {
				header: '作业ID',
				dataIndex: 'jobId'
			}, {
				header: '作业名称',
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
					fields: [
						'taskId', 'name', 'jobName', 'jobId', 'taskStatus', 
						'taskDate', 'settingTime', 'cycleType', 'merge', 
						'masterTasks', 'allMasterTaskIds',
						'dutyOfficer', 'userGroup.name'
					]
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
		
		for (var i = 0, len = mdl.tasks.length; i < len; i++) {
			var masterTask = mdl.tasks[i];
			masterTask.invalid = false;
			
			mdl.masterTaskMapping[mdl.tasks[i].taskId] = masterTask;
		}
		
		if (!mdl.containChildren) {
			mdl.setMasterTask();
			return;
		}
		
		var tasks = [];
		Ext.iterate(mdl.tasks, function(task) {
			tasks = tasks.concat(mdl.processChildren(task));
		});
		
		mdl.setMasterTask();
		
		if (tasks.length == 0) {
			return;
		}
		
		// 重新遍历一次所有子任务,需要将这些子任务中去除无效的主任务
		// 必须在这里去除无效主任务,因为在上面遍历子任务时还没完成分析
		// 出哪些主任务是无效的
		Ext.iterate(tasks, function(childTask) {
			var masterTasks = childTask.masterTasks,
				
				newMasterTasks = [],
				allMasterTaskIds = [];
				
			Ext.iterate(masterTasks, function(masterTask) {
				masterTask = mdl.masterTaskMapping[masterTask.taskId];
				allMasterTaskIds.push(masterTask.taskId);
				
				if (masterTask.invalid !== true) {
					newMasterTasks.push(masterTask);
				}
			});
			
			childTask.masterTasks = newMasterTasks;
			
			// 这里存放了包含了有效无效的所有主任务ID
			// 该变量主要是为了双击删除的功能,如果没有
			// 这个变量可能删除时无法准确删除其子任务
			childTask.allMasterTaskIds = allMasterTaskIds;
		});
		
		var store = mdl.centerPnl.store;
		store.on('load', function() {
			Ext.Msg.hide();
		}, null, {single:true});
		
		store.loadData(tasks);
	},
	
	processChildren: function(masterTask) {
		var mdl = this,
			masterTaskId = masterTask.taskId,
		
			masterChildren = framework.syncRequest({
				url: 'task/children',
				params: {
					taskId: masterTaskId,
					allowFetchParent: false,
					merge: false
				},
				decode: true
			});
		
		var prevTasks = [],
			tasks = [];

		// 缓存下当前主任务的所有子任务
		mdl.depthChildren[masterTaskId] = masterChildren;
		
		Ext.iterate(masterChildren, function(taskId) {
			var children = masterChildren[taskId];
			
			for (var i = 0; i < children.length; i++) {
				var child = children[i];

				// 如果当前子任务就是选取的主任务则标记主任务无效
				// 因为选取的主任务不允许出现在子任务中
				if (mdl.masterTaskMapping[child.taskId] != null) {
					mdl.masterTaskMapping[child.taskId].invalid = true;
				}
				
				if (Ext.isEmpty(child.masterTasks)) {
					child.masterTasks = [];
				}
			
				if (mdl.prevJobMapping[child.jobId] === true) {
					child.name = '<span style="color:red;">[有后置作业] </span>' + child.name;
					prevTasks.push(child);
				}
				
				var processedTask = mdl.processedTasks[child.taskId];
				if (processedTask == null) {
					tasks.push(child);
					mdl.processedTasks[child.taskId] = processedTask = child;
				}
				
				var exists = false;
				for (var j = 0, len = processedTask.masterTasks.length; j < len; j++) {
					if (masterTask.jobId === processedTask.masterTasks[j].jobId) {
						exists = true;
						break;
					}
				}
				
				if (!exists) {
					processedTask.masterTasks.push(masterTask);
				}
			}
		});

		return tasks;
	},
	
	/**
	 * 设置主任务
	 */
	setMasterTask: function() {
		// 设置批量选中的任务信息
		var mdl = this,
		
			taskIds = [],
			taskNames = [];
		
		Ext.iterate(mdl.masterTaskMapping, function(masterTaskId) {
			var masterTask = mdl.masterTaskMapping[masterTaskId];
			
			var taskName = [];
				
			if (masterTask.invalid === true) {
				taskName.push('\t[无效] ');
			}
			
			taskName.push(masterTask.jobId, ' - ');
			taskName.push(masterTask.jobName, ' [');
			taskName.push('任务日期: ', masterTask.taskDate);
			taskName.push(', 后置作业: ', mdl.prevJobMapping[masterTask.jobId] == true ? '有' : '无');
			taskName.push(']');

			taskIds.push(masterTask.taskId);
			taskNames.push(taskName.join(''));
		});
		
		var form = mdl.northPnl.form;
		form.findField('jobName').setValue(taskNames.join('\n'));
	},
	
	onModuleRender: function(mdl) {
		mdl.setDefaultAction(mdl.doAction, mdl);
		
		com.sw.bi.scheduler.task.TaskRedoSupplyBatchModule.superclass.onModuleRender.call(this);
	},
	
	onRowDblClick: function(grid, row, e) {
		var mdl = this,
			isRedo = mdl.actionType == 'redo';
		
		Ext.Msg.confirm('提示', '是否取消该作业及其子作业的' + (isRedo ? '重跑' : '补数据') + '?', function(btn) {
			if (btn != 'yes') return;
			
			var store = grid.store,
				record = store.getAt(row);
				
				taskId = record.get('taskId'),
				childrenTaskIds = {},
				depthChildren = null;
				
			var allMasterTaskIds = record.get('allMasterTaskIds'),
			
				// 获得需要删除任务的每个主任务对应的所有子任务集合
				masterTaskAndDeptChildren = {};
				
			for (var i = 0, len = allMasterTaskIds.length; i < len; i++) {
				var masterTaskId = allMasterTaskIds[i];
				masterTaskAndDeptChildren[masterTaskId] = mdl.depthChildren[masterTaskId];
			}
			
			var	getChildrenTasks = function(taskId, depthChildren) {
					childrenTaskIds[taskId] = true;
					
					var children = depthChildren[taskId];
					if (children != null && children.length > 0) {
						Ext.iterate(children, function(child) {
							getChildrenTasks(child.taskId, depthChildren);
						});
					}
				};
				
			Ext.iterate(masterTaskAndDeptChildren, function(masterTaskId) {
				var depthChildren = masterTaskAndDeptChildren[masterTaskId];
				
				// 在指定的主作业的子任务集合中查出需要删除任务的所有子任务
				getChildrenTasks(taskId, depthChildren);
			});

			var removeCount = 0;
			for (var i = store.getCount() - 1; i >= 0; i--) {
				var record = store.getAt(i);
				
				if (Ext.isEmpty(record)) {
					continue;
				}
				
				if (childrenTaskIds[record.get('taskId')] !== true) {
					continue;
				}
				
				// 累计删除的数量
				removeCount += 1;
				
				var newMasterTasks = [],
					masterTasks = record.get('masterTasks');
				
				for (var j = 0, len = masterTasks.length; j < len; j++) {
					var masterTaskId = masterTasks[j].taskId;
					
					if (masterTaskAndDeptChildren[masterTaskId] == null) {
						newMasterTasks.push(masterTasks[j]);
					}
				}
				
				if (newMasterTasks.length == 0) {
					store.remove(record);
				} else {
					record.set('masterTasks', newMasterTasks);
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
			
			form = this.northPnl.form,
			
			params = {
				operateBy: USER_ID,
				breakpoint: allowBreakpoint === true,
				isSerialSupply: isSerialSupply === true,
				isCascadeValidateParentTask: isCascadeValidateParentTask === true
			};
			
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

		var store = mdl.centerPnl.store,
			
			masterTaskIds = [],
			childrenTaskIds = [],
			masterAndChildrenTaskIds = {};
			
		// 获得有效的主作业
		Ext.iterate(mdl.masterTaskMapping, function(masterTaskId) {
			var masterTask = mdl.masterTaskMapping[masterTaskId];
			if (masterTask.invalid !== true) {
				masterAndChildrenTaskIds[masterTask.taskId] = [];
			}
		});
			
		if (store.getCount() > 0) {
			store.each(function(record) {
				var masterTasks = record.get('masterTasks');
				for (var i = 0, len = masterTasks.length; i < len; i++) {
					masterAndChildrenTaskIds[masterTasks[i].taskId].push(record.get('taskId'));
				}
			});
		}
		params['masterAndChildrenTaskId'] = Ext.encode(masterAndChildrenTaskIds);
		
		/*alert(Ext.encode(masterAndChildrenTaskIds));
		return;*/
		
		var loadMask = mdl.loadMask;
		loadMask.msg = '正在' + (isRedo ? '重跑作业' : '补作业数据') + ',请耐心等候...';
		loadMask.show();
		
		Ext.Ajax.request({
			timeout: 1200000, // 20分钟
			url: 'task/' + (isRedo ? 'batchRedo' : 'batchSupply'),
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