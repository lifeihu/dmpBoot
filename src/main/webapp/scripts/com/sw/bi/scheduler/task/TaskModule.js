_package('com.sw.bi.scheduler.task');

_import([
	'framework.modules.MultiModule',
	'framework.widgets.container.ModuleContainer',
	
	'com.sw.bi.scheduler.task.TaskRelationChartModule',
	'com.sw.bi.scheduler.task.TaskListModule',
	'com.sw.bi.scheduler.action.ActionModule'
]);

com.sw.bi.scheduler.task.TaskModule = Ext.extend(framework.core.Module, {
	
	/**
	 * 是否禁用任务日期的查询条件
	 * @type Boolean
	 */
	disableTaskDate: false,
	
	/**
	 * @property taskActionMenu
	 * @type Ext.menu.Menu
	 * @description 任务操作菜单
	 */
	taskActionMenu: null,
	
	initModule: function() {
		this.storeUser = S.create('users');
		this.storeUser.load();
		
		this.initTaskActionMenu();
		
		com.sw.bi.scheduler.task.TaskModule.superclass.initModule.call(this);
	},
	
	south: function() {
		var mdl = this;
		return {
			height: 450,
			minHeight: 290,
			split: true,
			collapseMode: 'mini',
			
			items: {
				xtype: 'multimodule',
				
				moduler: function() {
					return [{
						xtype: 'tasklistmodule',
						title: '任务',
						module: mdl,
						
						onModuleRender: mdl.onTaskListModuleRender.createDelegate(mdl),
						scope: mdl
					}, {
						xtype: 'actionmodule',
						title: '任务明细'
					}];
				},
				
				listeners: {
					tabchange: mdl.onTaskChange,
					scope: mdl
				}
			}
		};
	},
	
	center: function() {
		var mdl = this;
		return {
			items: {
				xtype: 'taskrelationchart',
				module: mdl
			}
		};
	},
	
	initTaskActionMenu: function() {
		var gateways = framework.syncRequest({
				url: 'gateway/list',
				params: {
					sort: 'master',
					dir: 'desc',
					condition: encodeURIComponent(Ext.encode({
						'status-eq': 1
					}))
//					condition: 'status-eq = 1'
				},
				decode: true
			}),
			gatewayItems = [];
		
		Ext.iterate(gateways, function(gateway) {
			if (gateway.master === true) {
				gatewayItems.push({
					text: gateway.name
				});
			}
		});
		
		this.taskActionMenu = new Ext.menu.Menu({
			items: [{
				name: 'redo',
				iconCls: 'redo',
				text: '重跑该作业'
			}, {
				name: 'redoChildren',
				iconCls: 'redo',
				text: '重跑该作业及其子作业'
			}, '-', {
				name: 'supply',
				iconCls: 'supply',
				text: '补该作业数据'
			}, {
				name: 'supplyChildren',
				iconCls: 'supply',
				text: '补该作业及其子作业数据'
			}, '-', {
				name: 'viewDetail',
				text: '查看执行明细',
				iconCls: 'maintain'
			}, {
				name: 'viewLog',
				text: '查看执行日志',
				iconCls: 'log'
			}, '-', {
				name: 'viewPID',
				text: '查询进程'
			}, {
				name: 'killPID',
				text: '删除进程'
			}, '-', {
				name: 'expandTasks',
				text: '展开任务'
			}, '-', {
				name: 'addReferPoint',
				text: '添加参考点'
			}, {
				name: 'simulateSchedule',
				text: '模拟后台',
				menu: {
					items: gatewayItems,
					listeners: {
						itemclick: function(item, e) {
							var parent = item.ownerCt.ownerCt;
							parent.gateway = item.text;
							
							this.onActionMenuItemClick(parent, e);
						},
						scope: this
					}
				}
			}, '-', {
				name: 'analyseUnrunningTask',
				text: '分析任务未运行原因'
			}, {
				name: 'runTimeChart',
				text: '作业运行时长统计图'
			}, {
				name: 'chtRefresh',
				text: '刷新任务',
				iconCls: 'refresh'
			}],
			
			listeners: {
				beforeshow: this.onActionMenuBeforeShow,
				itemclick: this.onActionMenuItemClick,
				scope: this
			}
		});
	},
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 是否允许查看明细或日志
	 * @param {Ext.data.Record/Object} task
	 */
	allowView: function(task) {
		if (task instanceof Ext.data.Record) {
			task = task.data;
		}
		
		if (!Ext.isEmpty(task.lastActionId, false)) {
			return true;
		}
		
		if (FOREGROUND_TASK_STATUS[task.taskStatus] == '正在运行') {
			return true;
		}
		
		return false;
	},
	
	/**
	 * 查看指定任务执行明细
	 * @param {Ext.data.Record/Object} task
	 */
	viewDetail: function(task) {
		if (task instanceof Ext.data.Record) {
			task = task.data;
		}
		
		var mdl = this,
			module = mdl.southPnl.getComponent(0),
			tabPnl = module.centerPnl,
			active = tabPnl.getActiveTab(),
			
			params = {
				taskId: task.taskId,
				taskDateStart: task.taskDate,
				taskDateEnd: task.taskDate,
				
				jobId: null,
				jobName: null,
				'actionStatus-eq': '',
				'flag-eq': '',
				operator: null
			};

		if (active instanceof com.sw.bi.scheduler.task.TaskListModule) {
			tabPnl.on({
				tabchange: function(tab, item) {
					item.setCondition(params);					
					module.loadData();
				},
				single: true
			});
			
			tabPnl.setActiveTab(1);
			
		} else {
			active.setCondition(params);
			module.loadData();
		}
	},
	
	/**
	 * 查看指定任务的最近一次日志
	 * @param {Ext.data.Record/Object} task
	 */
	viewLog: function(task) {
		if (task instanceof Ext.data.Record) {
			task = task.data;
		}
		
		framework.createWindow({
			title: '日志查看',
			iconCls: 'log',
			
			module: {
				module: 'com.sw.bi.scheduler.action.ActionLogModule',
				actionId: task.lastActionId
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
	},
	
	/**
	 * 给指定任务进行加权操作
	 * @param {Ext.data.Record/Object} task
	 */
	weighting: function(task) {
		if (task instanceof Ext.data.Record) {
			task = task.data;
		}
		
		var jobName = task.name || task.jobName;
		
		Ext.Msg.confirm('提示', '是否需要对 "' + jobName + '" 作业进行加权操作?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'weight/weighting',
				params: {taskId: task.taskId},
				waitMsg: '正在给作业加权,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '"' + (task.name || task.jobName) + '" 作业加权成功!');
				}
			});
		});
	},
	
	/**
	 * 查看进程信息
	 * @param {Ext.data.Record/Object} task
	 */
	viewPID: function(task) {
		if (task instanceof Ext.data.Record) {
			task = task.data;
		}
		
		framework.createWindow({
			title: '查看进程信息',
			
			module: {
				module: 'com.sw.bi.scheduler.action.ActionPIDModule',
				actionId: task.lastActionId
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
	},
	
	/**
	 * 删除进程
	 * @param {Ext.data.Record/Object} task
	 */
	killPID: function(task) {
		if (task instanceof Ext.data.Record) {
			task = task.data;
		}
		
		var mdl = this,
			jobName = task.name || task.jobName;
		
		Ext.Msg.confirm('提示', '是否删除 ' + jobName + ' 任务的执行进程?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'action/killPID',
				params: {actionId: task.lastActionId},
				waitMsg: '正在删除任务执行进程,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '"' + (task.name || task.jobName) + '" 任务进程删除成功!');
					mdl.southPnl.getComponent(0).getActiveTab().loadData();
				}
			});
		});
	},
	
	/**
	 * 删除指定网关机上所有正在运行的任务
	 * @param gateway
	 */
	killRunningTaskPID: function(gateway) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '该操作将删除 "' + (gateway == null ? '所有网关机' : '网关机(' + gateway + ')') + '" 上最近二天所有正在运行任务的进程,并更改状态为失败,是否继续?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'action/killGatewayPID',
				waitMsg: '正在删除正在运行任务的进程,进耐心等候...',
				
				params: {
					gateway: gateway
				},
				
				success: function(response) {
					mdl.loadTaskData({
						'useScanDate': true,
						'taskStatus': 1
					});
				}
			});
		});
	},
	
	/**
	 * 作业批量重跑
	 * 
	 * @param isRedo
	 * @param allowChildren 是否允许重跑子作业
	 */
	batchRedoOrSupply: function(isRedo, allowChildren) {
		var mdl = this,
			selections = mdl.southPnl.getComponent(0).getActiveTab().getSelections(),
			
			lastJobId = null,
			tasks = [];
			
		if (selections.length == 0) {
			Ext.Msg.alert('提示', '请选择需要被重跑的任务.');
			return;
		}
			
		for (var i = 0; i < selections.length; i++) {	
			var record = selections[i],
			
				jobId = record.get('jobId'),
				taskStatus = FOREGROUND_TASK_STATUS[record.get('taskStatus')];
				
			if (jobId == 1) {
				Ext.Msg.alert('提示', '根任务不允许重跑操作.');
				return;
			}
				
			if (taskStatus == '未运行' || taskStatus == '正在运行') {
				Ext.Msg.alert('提示', '你选择的任务中有 "未运行" 或 "正在运行" 的任务，不允许进行重跑操作.');
				return;
			}
			
			/**
			 * 2014-05-06
			 * 讨论决定可以将所选的任务在重跑逻辑中稍作变更，将所选主任务也改成重做初始化状态，
			 * 最终有调度自动去触发，这样就可以取消相同作业ID才能批量模拟的限制
			 */
			// 判断所选的任务是否在同一作业下
			/*if (lastJobId == null) {
				lastJobId = jobId;
				
			} else {
				if (lastJobId != jobId) {
					Ext.Msg.alert('提示', '只有相同作业下的任务才允许进行批量重跑操作.');
					return;
				}
			}*/
			
			tasks.push(record.data);
		}
		
		if (tasks.length == 0) {
			return;
		}
		
		if (isRedo && !allowChildren) {
			var masterAndChildrenTaskIds = {};
			for (var i = 0, len = tasks.length; i < len; i++) {
				masterAndChildrenTaskIds[tasks[i].taskId] = [];
			}
			
			Ext.Msg.confirm('提示', '是否批量重跑选定的 ' + tasks.length + ' 个任务?', function(btn) {
				if (btn !== 'yes') {
					return;
				}
				
				Ext.Ajax.request({
					url: 'task/batchRedo',
					params: {
						masterAndChildrenTaskId: Ext.encode(masterAndChildrenTaskIds),
						operateBy: USER_ID
					},
					waitMsg: '正在批量重跑作业,请耐心等候...',
					
					success: function() {
						mdl.southPnl.getComponent(0).loadData();
					}
				});
			});
			
		} else {
			var win = mdl.createWindow({
				title: isRedo ? '批量重跑' : '批量补数据',
				resizable: false,
				
				relayEvent: ['success'],
				
				module: {
					module: 'com.sw.bi.scheduler.task.TaskRedoSupplyBatchModule',
					
					actionType: isRedo ? 'redo' : 'supply',
					containChildren: allowChildren,
					tasks: tasks,
					
					taskStatusRenderer: mdl.taskStatusRenderer.createDelegate(mdl)
				}
				
			}, 'framework.widgets.window.ModuleWindow');
				
			win.on({
				success: function() {
					mdl.southPnl.getComponent(0).loadData();
				}
			});
				
			win.open();
		}
	},
	
	/**
	 * 模拟后台调度
	 * @param gateway
	 * @param selections
	 */
	simulateSchedule: function(gateway, selections) {
		if (Ext.isEmpty(gateway, false)) {
			Ext.Msg.alert('提示', '网关机不允许为空!');
			return;
		}
		
		var mdl = this,
			selections = selections || mdl.southPnl.getComponent(0).getActiveTab().getSelections();

		if (selections.length == 0) {
			Ext.Msg.alert('提示', '模拟后台调度必须先选择需要执行的任务!');
			return;
		}
			
		var ids = [],
			failureCount = 0;
			
		for (var i = 0, len = selections.length; i < len; i++) {
			var record = selections[i];
			if (record instanceof Ext.data.Record) {
				record = record.data;
			}
			
			var taskStatus = FOREGROUND_TASK_STATUS[record.taskStatus];
			if (taskStatus == '运行失败') {
				failureCount += 1;
			}
			
			if (record.cycleType/*.get('cycleType')*/ == JOB_CYCLE_TYPE.NONE) {
				Ext.Msg.alert('提示', '不允许选择 "待触发" 周期的作业进行模拟后台操作!');
				return;
				
			} else {
				ids.push(record.taskId/*get('taskId')*/);
			}
		}
		
		// 只有当所有选取任务都为失败状态时才允许批量模拟
		// 模拟后台的功能已经修改了，对于非管理员用户可以开放
		// 但是对于父子作业的批量模拟还是需要人工去控制
		// if (selections.length > 3 && !USER_IS_ADMINISTRTOR) {
			// if (failureCount != selections.length) {
				/**
				 * 暂时屏蔽批量模拟的功能
				 * 原因：批量模拟是按照页面上的顺序提交给后台执行的，可就有可能出现子作业在前父作业在后
				 * 当子作业校验父作业是否完成的动作时由于父作业排在后面所以该子作业是被改为“已触发”状态
				 * 而且是在所有作业更改完状态后一次性提交后台的，所以当子作业真正被执行起来时父作业已经
				 * 不是成功状态了，所以子作业就被改回了“未运行”状态
				 */
				// Ext.Msg.alert('提示', '非管理员批量模拟运行最多3个任务.');
				// return;
			// }
		// }

		Ext.Msg.confirm('提示', '是否需要模拟选中的 ' + selections.length + ' 个任务进行后台调度?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				timeout: 1000 * 60 * 30, // 半个小时
				url: 'task/simulateSchedule',
				params: {
					gateway: gateway,
					id: ids.join(',')
				},
				waitMsg: '正在执行后台调度,请耐心等候...',
				
				success: function(response) {
					var msg = Ext.decode(response.responseText);
					if (!Ext.isEmpty(msg, false)) {
						Ext.Msg.alert('提示', msg);
					}
					
					// 按原有条件重新查询
					var mdlTaskList = mdl.southPnl.getComponent(0).getActiveTab();
					mdlTaskList.loadData.defer(900, mdlTaskList);
					/*mdl.loadTaskData({
						taskId: ids.join(','),
						useScanDate: true
					});
					
					// 查询结束后需要把taskId条件置空,否则下次查询就查不到记录了
					(function() {
						mdl.southPnl.getComponent(0).getActiveTab().setCondition({
							taskId: ''
						});
					}).defer(300);*/
				}
			})
		});
	},
	
	/**
	 * 恢复指定日期的FTP备份目录下的文件
	 * @param {Date} taskDate
	 */
	restoreBackupFile: function(taskDate) {
		var mdl = this,
			selections = mdl.southPnl.getComponent(0).getActiveTab().getSelections();
			
		if (selections.length == 0) {
			Ext.Msg.alert('提示', '请先选择作业.');
			return;
		}
		
		var jobIds = new Ext.util.MixedCollection();
		for (var i = 0, len = selections.length; i < len; i++) {
			var record = selections[i],
			
				jobId = record.get('jobId'),
 				jobType = record.get('jobType');

			if (jobType != 6 && jobType != 7) {
				Ext.Msg.alert('提示', '作业[作业ID: ' + jobId + ', 作业名称: ' + record.get('jobName') + ']类型不支持.<br>恢复备份文件的操作只支持 "间隔n分钟" 和 "不需要成功标记" 的FTP作业.');
				return;
			}
			
			if (!jobIds.containsKey(jobId)) {
				jobIds.add(jobId, true);
			}
		};
		
		Ext.Msg.confirm('提示', '是否恢复 ' + taskDate + ' 的备份文件?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				timeout: 300000000,
				url: 'toolbox/restoreBackupFile',
				params: {
					taskDate: taskDate,
					jobId: jobIds.keys.join(',')
				},
				waitMsg: '正在恢复备份文件,请耐心等待...',
				
				success: function(response) {
					var result = response.responseText;
					if (!Ext.isEmpty(result, false)) {
						Ext.Msg.alert('提示', Ext.decode(result));
					}
				}
			});
		});
	},
	
	/**
	 * 按选取的任务添加参考点
	 */
	addReferPoints: function() {
		var mdl = this,
			selections = mdl.southPnl.getComponent(0).getActiveTab().getSelections()
			taskIds = [];
			
		if (selections.length == 0) {
			Ext.Msg.alert('提示', '请选择需要添加参考点的任务.');
			return;
		}
			
		for (var i = 0; i < selections.length; i++) {	
			var record = selections[i],
			
				taskStatus = FOREGROUND_TASK_STATUS[record.get('taskStatus')];

			taskIds.push(record.get('taskId'));
		}
		
		Ext.Msg.confirm('提示', '是否需要将选中的 ' + taskIds.length + ' 个任务的父任务加入参考点?', function(btn) {
			if (btn !== 'yes') return;
			
			Ext.Ajax.request({
				url: 'waitUpdateStatusTask/addParentTasks',
				params: {
					taskId: taskIds.join(',')
				},
				waitMsg: '正在处理已选取任务的参考点,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '成功将父作业加入参考点.');
				}
			});
		});
	},
	
	/**
	 * 按扫描日期添加参考点
	 */
	addReferPointsByScanDate: function() {
		Ext.Msg.confirm('提示', '是否需要将扫描日期为今天的任务的父任务加入参考点?', function(btn) {
			if (btn !== 'yes') return;
			
			Ext.Ajax.request({
				url: 'task/addReferPointsByScanDate',
				waitMsg: '正在处理扫描日期是今天的任务的参考点,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '成功将父作业加入参考点.');
				}
			});
		});
	},
	
	/**
	 * 分析指定任务未运行的原因
	 * @param unrunningTask
	 */
	analyseUnrunningTasks: function(btn, e, unrunningTask) {
		var mdl = this,
			selections = unrunningTask != null ? [unrunningTask] : mdl.southPnl.getComponent(0).getActiveTab().getSelections()
			taskIds = [];
			
		if (selections.length == 0) {
			Ext.Msg.alert('提示', '请选择需要分析未运行原因的任务.');
			return;
		}
			
		var runningNumber = 0;
		for (var i = 0; i < selections.length; i++) {
			var record = Ext.isDefined(selections[i].data) ? selections[i].data : selections[i],
			
				taskStatus = FOREGROUND_TASK_STATUS[record.taskStatus];

			if (taskStatus != '未运行') {
				runningNumber += 1;
				continue;
			}
				
			taskIds.push(record.taskId);
		}
		
		if (taskIds.length == 0) {
			Ext.Msg.alert('提示', '选取的任务都是已经运行的任务, 不需要再进行原因分析');
			return;
		}
		
		var win = mdl.createWindow({
			title: '任务未运行原因分析',
			
			module: {
				module: 'com.sw.bi.scheduler.task.UnrunningTaskAnalyseModule',
				
				taskIds: taskIds,
				
				taskStatusRenderer: mdl.taskStatusRenderer.createDelegate(mdl)
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
		
		/*var message = ['是否需要对选取的 '];
		message.push(selections.length, ' 个任务')
		if (runningNumber > 0) {
			message.push('(排除 ' + runningNumber + ' 个已运行的任务)');
		}
		message.push('进行未运行原因分析?');
		
		Ext.Msg.confirm('提示', message.join(''), function(btn) {
			if (btn !== 'yes') return;
			
			var win = mdl.createWindow({
				title: '任务未运行原因分析',
				
				module: {
					module: 'com.sw.bi.scheduler.task.UnrunningTaskAnalyseModule',
					
					taskIds: taskIds,
					
					taskStatusRenderer: mdl.taskStatusRenderer.createDelegate(mdl)
				}
				
			}, 'framework.widgets.window.ModuleWindow').open();
		});*/
		
	},
	
	/**
	 * 渲染任务状态
	 */
	taskStatusRenderer: function(value, meta, record) {
		var foregroundStatus = null,
			qtip = [];
			
		if (record.get('merge') === true) {
			foregroundStatus = FOREGROUND_TASK_STATUS_NATIVE[value];
			qtip.push('<div>前台状态: ', FOREGROUND_TASK_STATUS_NATIVE[value], '</div>');
		} else {
			foregroundStatus = FOREGROUND_TASK_STATUS[value];
			qtip.push('<div>前台状态: ', FOREGROUND_TASK_STATUS[value], '</div>');
			qtip.push('<div>后台状态: ', BACKGROUND_TASK_STATUS[value], '</div>');
		}
		
		meta.attr += ' ext:qtitle="作业状态" ext:qtip="' + qtip.join('') + '"';
		
		return foregroundStatus;
	},
	
	/**
	 * 加载任务数据
	 */
	loadTaskData: function(condition) {
		var multi = this.southPnl.getComponent(0);
		multi.setActiveTab(0);
		
		if (!Ext.isEmpty(condition)) {
			multi.getActiveTab().setCondition(Ext.apply({
				'taskId': '',
				'jobId': '',
				'jobName': '',
				'settingTime': '',
				'jobBusinessGroup': '',
				'dutyOfficer': '',
				'taskStatus': '',
				'cycleType': '',
				'taskDateStart': '',
				'taskDateEnd': '',
				'jobType': '',
				'useScanDate': false
			}, condition));
		}
		
		multi.getActiveTab().loadData(0);
	},
	
	loadActionData: function(condition) {
		var multi = this.southPnl.getComponent(0);
		multi.setActiveTab(1);
		
		if (!Ext.isEmpty(condition)) {
			multi.getActiveTab().setCondition(Ext.apply({
				'taskId': '',
				'scanDate': '',
				'startTime': '',
				'endTime': '',
				'jobId': '',
				'jobName': '',
				'settingTime': '',
				'operator': '',
				'actionStatus': '',
				'flag': '',
				'taskDateStart': '',
				'taskDateEnd': '',
				'gateway': '',
				'jobType': '',
				'useScanDate': false
			}, condition));
		}
		
		multi.getActiveTab().loadData(0);
	},
	
	onActionMenuBeforeShow: function(menu) {
		var parent = menu,
			task = null,
			from = null;

		// 获得当前选中的任务
		while (parent) {
			task = parent.currentTask;
			from = parent.from;
			
			if (!Ext.isEmpty(task)) {
				break;
			}
			
			parent = parent.parentMenu;
		}
		
		if (Ext.isEmpty(task)) {
			return;
		}

		var mdl = this,

			fromGrid = from instanceof Ext.grid.GridPanel,
		
			cycleType = task.cycleType,
			taskStatus = FOREGROUND_TASK_STATUS[task.taskStatus],
			
			mnuRedo = menu.getComponent(0/*'redo'*/),
			mnuRedoChildren = menu.getComponent(1/*'redoChildren'*/),
			mnuSep1 = menu.getComponent(2),
			
			mnuSupply = menu.getComponent(3/*'supply'*/),
			mnuSupplyChildren = menu.getComponent(4/*'supplyChildren'*/),
		
			mnuSep2 = menu.getComponent(5),
			mnuViewDetail = menu.getComponent(6/*'viewDetail'*/),
			mnuViewLog = menu.getComponent(7/*'viewLog'*/),
			
			mnuSep3 = menu.getComponent(8),
			mnuViewPID = menu.getComponent(9/*'viewPID'*/),
			mnuKillPID = menu.getComponent(10/*'killPID'*/),
			
			mnuSep4 = menu.getComponent(11),
			mnuExpand = menu.getComponent(12/*'expandTasks'*/),
			
			mnuSep5 = menu.getComponent(13),
			mnuAddReferPoint = menu.getComponent(14/*'addReferPoint'*/);
			mnuSimulateSchedule = menu.getComponent(15);
			
			mnuSep6 = menu.getComponent(16),
			mnuAnalyse = menu.getComponent(17/*analyseUnrunningTask*/),
			mnuChartRunTime = menu.getComponent(18/*runTimeChart*/),
			mnuRefreshTask = menu.getComponent(19/*'chtRefresh'*/);
			
		menu.items.each(function(item) {
			item.hide();
		});

		mnuAddReferPoint.show();
		mnuChartRunTime.show();
			
		if (taskStatus == '未运行') {
			mnuAnalyse.show();
		}
		
		if (fromGrid && (task.taskStatus == 0 || task.jobId == 1)) { // 初始化或根任务没有右键菜单
			return true;
		}

		if (task.merge !== true) {
			if (taskStatus == '运行成功' || taskStatus == '运行失败') {
				mnuRedo.show();
				mnuRedoChildren.show();
				mnuSep1.show();
				
				mnuSupply.show();
				mnuSupplyChildren.show();
				mnuSep2.show();
			}/* else {
				mnuRedo.hide();
				mnuRedoChildren.hide();
				mnuSep1.hide();
				
				mnuSupply.hide();
				mnuSupplyChildren.hide();
				mnuSep2.hide();
			}*/
	
			if (mdl.allowView(task)) {
				mnuViewDetail.show();
				mnuViewLog.show();
				
			}/* else {
				mnuSep2.hide();
				mnuViewDetail.hide();
				mnuViewLog.hide();
			}*/
			
			if (taskStatus == '正在运行') {
				mnuSep3.show();
				mnuViewPID.show();

				// 只允许DataX、MapReduce和HiveSQL作业能显示杀进程的菜单
				switch (task.jobType) {
					case 1:
					case 2:
					case 3:
					case 4:
					case 20:
					case 21:
					case 30:
					case 31:
					case 32:
					case 33:
					case 34:
					case 35:
					case 36:
					case 50:
					case 51:
					case 52:
					case 53:
					case 54:
					case 55:
					case 60:
					case 61:
					case 62:
					case 63:
					case 64:
					case 65:
					case 70:
					case 71:
					case 72:
					case 73:
					case 74:
					case 75:
					case 80:
					case 81:
					case 82:
					case 83:
					case 84:
					case 85:
					case 110:
					case 111:
					case 112:
					case 113:
					case 114:
					case 115:
					case 116:
					case 120:
					case 121:
					case 122:
					case 123:
					case 124:
					case 125:
					case 126:
						mnuKillPID.show();
					break;
				}
			}/* else {
				mnuSep3.hide();
				mnuViewPID.hide();
				mnuKillPID.hide();
			}*/
			
			if (!fromGrid) {
				mnuSep6.show();
				mnuRefreshTask.show();
				mnuSimulateSchedule.show();
			}

			/*if (fromGrid) {
				mnuSep6.hide();
				mnuRefreshTask.hide();
			} else {
				mnuSep6.show();
				mnuRefreshTask.show();
			}*/
		}/* else {
			mnuRedo.hide();
			mnuRedoChildren.hide();
			mnuSep1.hide();
			
			mnuSupply.hide();
			mnuSupplyChildren.hide();
		
			mnuSep2.hide();
			mnuViewDetail.hide();
			mnuViewLog.hide();
			
			mnuSep3.hide();
			mnuViewPID.hide();
			mnuKillPID.hide();
			
			mnuSep6.hide();
			mnuRefreshTask.hide();
		}*/
		
		if (!fromGrid && (cycleType == JOB_CYCLE_TYPE.HOUR || cycleType == JOB_CYCLE_TYPE.MINUTE)) {
			mnuSep4.show();
			mnuExpand.show();
			
			mnuExpand.setText('展开' + (cycleType == JOB_CYCLE_TYPE.HOUR ? '小时' : '分钟') + '任务');
		}/* else {
			mnuSep4.hide();
			mnuExpand.hide();
		}*/
	
		var hiddenItems = 0;
		menu.items.each(function(item) {
			if (item.hidden) {
				hiddenItems += 1;
			}
		});
		
		// TODO 暂时屏蔽杀进程功能,杀进程功能完善后删除下面这行
		// mnuKillPID.hide();
		
		return menu.items.getCount() != hiddenItems;
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
			mdlChart = this.centerPnl,
			itemId = item.name;
		
		if (itemId == 'viewDetail') {
			mdl.viewDetail(task);
			
		} else if (itemId == 'viewLog') {
			mdl.viewLog(task);
		
		} else if (itemId == 'viewPID') {
			mdl.viewPID(task);
			
		} else if (itemId == 'killPID') {
			mdl.killPID(task);
			
		} else if (itemId == 'chtRefresh') {
			// 刷新任务框
			var taskShape = CanvasMgr.getTask(task.taskId);
			if (taskShape) {
				taskShape.refresh();
			}
			
		} else if (itemId == 'runTimeChart') {
			var cycleType = task.cycleType,
				settingTime = Date.parseDate(task.settingTime, 'Y-n-j H:i:s'),
				endTaskDate = Date.parseDate(task.taskDate, 'Y-m-d'),
				startTaskDate = null;
			
			if (cycleType == JOB_CYCLE_TYPE.MONTH) {
				startTaskDate = endTaskDate.add(Date.MONTH, -12);
			} else if (cycleType == JOB_CYCLE_TYPE.WEEK) {
				startTaskDate = endTaskDate.add(Date.DAY, -12 * 7);
			} else {
				startTaskDate = endTaskDate.add(Date.DAY, -12);
			}

			mdl.createWindow({
				title: '作业运行时长统计图',
				
				module: {
					module: 'com.sw.bi.scheduler.statistic.TaskRunTimeChartModule',
					
					settingTime: settingTime,
					startTaskDate: startTaskDate,
					endTaskDate: endTaskDate,
					jobId: task.jobId
				}
				
			}, 'framework.widgets.window.ModuleWindow').open();
			
		} else if (itemId == 'expandTasks') {
			var cycleType = task.cycleType,
			
				win = mdl.createWindow({
					title: cycleType == JOB_CYCLE_TYPE.HOUR ? '小时任务' : '分钟任务',
					
					module: {
						module: cycleType == JOB_CYCLE_TYPE.HOUR ? 'com.sw.bi.scheduler.task.HourTaskModule' : 'com.sw.bi.scheduler.task.MinuteTaskModule',
						
						task: task,
						
						taskStatusRenderer: mdl.taskStatusRenderer.createDelegate(mdl)
					}
				}, 'framework.widgets.window.ModuleWindow');
				
			win.open();
			
		} else if (itemId == 'addReferPoint') { 
			Ext.Msg.confirm('提示', '是否将 "' + task.jobName + '" 作业的父作业加入参考点?', function(btn) {
				if (btn != 'yes') return;
				
				Ext.Ajax.request({
					url: 'waitUpdateStatusTask/addParentTasks',
					params: {
						taskId: task.taskId
					},
					
					success: function() {
						Ext.Msg.alert('提示', '成功将父作业加入参考点.');
					}
				});
			});
			
		} else if (itemId == 'simulateSchedule') {
			this.simulateSchedule(item.gateway, [task]);
			
		} else if (itemId == 'analyseUnrunningTask') {
			this.analyseUnrunningTasks(null, null, task);
			
		} else {
			var isRedo = itemId.indexOf('redo') > -1,
				containChildren = itemId.indexOf('Children') > -1,
				
				win = mdl.createWindow({
					title: isRedo ? '重跑作业' : '补作业数据',
					resizable: false,
					
					width: containChildren ? 1020 : 490,
					height: containChildren ? 600 : 200,
					
					relayEvent: ['success'],
					
					module: {
						module: 'com.sw.bi.scheduler.task.TaskRedoSupplyModule',
						
						actionType: isRedo ? 'redo' : 'supply',
						containChildren: containChildren,
						task: task,
						
						taskStatusRenderer: mdl.taskStatusRenderer.createDelegate(mdl)
					}
					
				}, 'framework.widgets.window.ModuleWindow');
				
			win.on({
				success: function() {
					mdl.southPnl.getComponent(0).loadData();
				}
			});
				
			win.open();
		}
	},
	
	doLayout: function() {
		com.sw.bi.scheduler.task.TaskModule.superclass.doLayout.apply(this, arguments);
		
		var mdl = this,
			height = mdl.getHeight(true),
			centerHeight = mdl.centerPnl.height;

		if (Ext.isNumber(centerHeight)) {
			mdl.southPnl.setHeight(height - centerHeight);
			
		} else {
			mdl.southPnl.setHeight(height - 9);
		}
	},
	
	onTaskListModuleRender: function(mdl) {
		com.sw.bi.scheduler.task.TaskListModule.superclass.onModuleRender.call(this, mdl);

		if (this.autoLoadData === false) {
			mdl.autoLoadData = null;
		}
	},
	
	onTaskChange: function(tp, tab) {
		if (this.disableTaskDate === true) {
			var searcher = tab.getComponent(0).form;
			if (!searcher) return;
			
			searcher.findField('taskDateStart').setReadOnly(true);
			searcher.findField('taskDateEnd').setReadOnly(true);
		}
	},
	
	beforeDestroy: function() {
		this.taskActionMenu.destroy();
		this.taskActionMenu = null;
		
		com.sw.bi.scheduler.task.TaskModule.superclass.beforeDestroy.apply(this, arguments);
	}
});

FOREGROUND_TASK_STATUS = {
	0: '未运行',
	1: '未运行',
	2: '未运行',
	3: '正在运行',
	4: '运行失败',
	5: '运行成功',
	6: '未运行',
	7: '未运行',
	8: '未运行',
	9: '正在运行',
	10: '运行失败',
	11: '运行成功'
};

FOREGROUND_TASK_STATUS_NATIVE = {
	0: '未运行',
	1: '正在运行',
	2: '运行失败',
	3: '运行成功'
};

BACKGROUND_TASK_STATUS = {
	0: '初始化状态',
	1: '未触发',
	2: '已触发',
	3: '运行中',
	4: '运行失败',
	5: '运行成功',
	6: '重做初始化',
	7: '重做未触发',
	8: '重做已触发',
	9: '重做运行中',
	10: '重做失败',
	11: '重做成功'
};