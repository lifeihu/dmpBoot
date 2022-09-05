_package('com.sw.bi.scheduler.task');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.task.TaskMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 900,
	saveUrl: 'task/update',
	
	initModule: function() {
		com.sw.bi.scheduler.task.TaskMaintainModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		var items = [{
			columnWidth: 1,
			xtype: 'fieldset',
			title: '作业信息',
			anchor: '100%',
			
			items: {
				xtype: 'panel',
				layout: 'column',
				border: false,
				
				items: [{
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textfield',
						// name: 'job.jobId',
						name: 'jobId',
						anchor: '49%',
						fieldLabel: '作业ID',
						readOnly: true
					}
				}/*, {
					columnWidth: .25,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textfield',
						name: 'taskId',
						fieldLabel: '任务ID',
						anchor: '96%',
						readOnly: true
					}
				}*/, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'combo',
						hiddenName: 'job.jobType',
						fieldLabel: '作业类型',
						anchor: '49%',
						readOnly: true,
						store: S.create('jobType')
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textfield',
						name: 'name',
						fieldLabel: '作业名称',
						anchor: '49%',
						readOnly: true
					}
				}, {
					columnWidth: .24,
					layout: 'form',
					border: false,
					items: {
						xtype: 'combo',
						hiddenName: 'cycleType',
						fieldLabel: '作业周期',
						store: S.create('jobCycleType'),
						anchor: '98%',
						readOnly: true
					}
				}, {
					columnWidth: .25,
					layout: 'form',
					border: false,
					labelWidth: 70,
					items: {
						xtype: 'textfield',
						name: 'job.dayN',
						fieldLabel: '每月(日)',
						readOnly: true,
						anchor: '99%'
					}
				}, {
					columnWidth: .25,
					layout: 'form',
					border: false,
					labelWidth: 35,
					items: {
						xtype: 'textfield',
						name: 'job.jobTime',
						fieldLabel: '时间',
						readOnly: true,
						anchor: '100%'
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textfield',
						name: 'jobBusinessGroup',
						fieldLabel: '业务组',
						readOnly: true,
						anchor: '49%'
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'combo',
						hiddenName: 'job.dutyOfficer',
						fieldLabel: '责任人',
						store: S.create('users'),
						anchor: '49%',
						readOnly: true
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'combo',
						hiddenName: 'jobLevel',
						fieldLabel: '优先级',
						store: S.create('jobLevel'),
						anchor: '49%',
						readOnly: true
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'combo',
						hiddenName: 'alert',
						fieldLabel: '警告',
						store: S.create('alertType'),
						anchor: '49%',
						readOnly: true
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textarea',
						name: 'preTasks',
						fieldLabel: '前置任务(由前置作业生成)',
						anchor: '100%',
						readOnly: true
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textarea',
						name: 'preTasksFromOperate',
						fieldLabel: '前置任务(由补数据操作生成)',
						anchor: '100%'
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textarea',
						name: 'parentJobs',
						fieldLabel: '父作业',
						anchor: '100%',
						readOnly: true
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textarea',
						name: 'job.jobDesc',
						fieldLabel: '描述',
						anchor: '100%',
						readOnly: true
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textfield',
						name: 'job.programPath',
						fieldLabel: '程序路径',
						readOnly: true,
						anchor: '100%'
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textfield',
						hidden: true,
						name: 'job.parameters',
						fieldLabel: '程序参数',
						readOnly: true,
						anchor: '100%'
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					border: false,
					items: {
						xtype: 'textarea',
						name: 'programCode',
						fieldLabel: '程序代码',
						anchor: '100%',
						readOnly: true,
						height: 185
					}
				}]
			}
		}];
		
		items.push({
			columnWidth: 1,
			xtype: 'fieldset',
			title: '调整作业优先级',
			anchor: '100%',
			
			items: {
				xtype: 'panel',
				layout: 'column',
				border: false,
				
				items: [{
					columnWidth: .25,
					layout: 'form',
					border: false,
					labelWidth: 80,
					items: {
						xtype: 'combo',
						name: 'jobLevel',
						fieldLabel: '当前优先级',
						store: S.create('jobLevel'),
						readOnly: true,
						anchor: '99%'
					}
				}, {
					columnWidth: .25,
					layout: 'form',
					border: false,
					labelWidth: 80,
					items: {
						xtype: 'combo',
						hiddenName: 'newJobLevel',
						fieldLabel: '新的优先级',
						store: S.create('jobLevel'),
						anchor: '99%',
						allowBlank: false
					}
				}]
			}
		}, {
			columnWidth: 1,
			xtype: 'fieldset',
			title: '调整作业状态',
			anchor: '100%',
			
			items: {
				xtype: 'panel',
				layout: 'column',
				border: false,
				
				items: [{
					columnWidth: .25,
					layout: 'form',
					border: false,
					items: {
						xtype: 'combo',
						name: 'taskStatus',
						fieldLabel: '当前状态',
						store: S.create('taskBackgroundStatus'),
						readOnly: true,
						anchor: '99%'
					}
				}, {
					columnWidth: .25,
					layout: 'form',
					border: false,
					items: {
						xtype: 'combo',
						hiddenName: 'newTaskStatus',
						fieldLabel: '新的状态',
						store: S.create('taskBackgroundStatus'),
						anchor: '99%',
						allowBlank: false
					}
				}]
			}
		});
		
		return {
			model: 'task',
			
			items: items
		};
	},
	
	/////////////////////////////////////////////////////////////////////////////
	
	onLoadDataComplete: function(mdl, data) {
		var task = data['task'],
			job = data['task.job'],
			parentJobs = data['parentJobs'],
			preTasks = data['preTasks'],
			preTasksFromOperate = data['preTasksFromOperate'],
		
			fldJobType = mdl.findField('job.jobType');
			fldDayN = mdl.findField('job.dayN'),
			fldJobTime = mdl.findField('job.jobTime'),
			
			fldProgramPath = mdl.findField('job.programPath'),
			fldParameters = mdl.findField('job.parameters'),
			fldProgramCode = mdl.findField('programCode'),
			fldDutyOfficer = mdl.findField('job.dutyOfficer'),
			
			fldNewJobLevel = mdl.findField('newJobLevel'),
			fldNewTaskStatus = mdl.findField('newTaskStatus'),
			
			fldParentJob = mdl.findField('parentJobs'),			
			fldPreTasks = mdl.findField('preTasks'),
			fldPreTasksFromOperate = mdl.findField('preTasksFromOperate');
		
		// 设置作业信息
		fldJobType.setValue(job.jobType);
		fldDayN.setValue(job.dayN);
		fldJobTime.setValue(job.jobTime);
		fldProgramPath.setValue(job.programPath);
		fldParameters.setValue(job.parameters);
		fldDutyOfficer.setValue(job.dutyOfficer);
		fldParentJob.setValue(parentJobs);
		fldPreTasks.setValue(preTasks);
		fldPreTasksFromOperate.setValue(preTasksFromOperate);
			
		switch (task.cycleType) {
			case JOB_CYCLE_TYPE.MONTH:
				fldDayN.show();
				fldDayN.setFieldLabel('每月(日)');
				
				fldJobTime.show();
				fldJobTime.setFieldLabel('时间');
			break;
			case JOB_CYCLE_TYPE.WEEK:
				fldDayN.show();
				fldDayN.setFieldLabel('每周(星期)');
				
				fldJobTime.show();
				fldJobTime.setFieldLabel('时间');
			break;
			case JOB_CYCLE_TYPE.DAY:
				fldDayN.hide();
				
				fldJobTime.show();
				fldJobTime.setFieldLabel('时间');
			break;
			case JOB_CYCLE_TYPE.HOUR:
				fldDayN.hide();
				
				fldJobTime.show();
				fldJobTime.setFieldLabel('分钟');
			break;
			case JOB_CYCLE_TYPE.MINUTE:
				fldDayN.show();
				fldDayN.setFieldLabel('每隔(分钟)');
				
				fldJobTime.hide();
			break;
		}
		
		if (Ext.isEmpty(job.programPath, false)) {
			fldProgramPath.hide();
			fldProgramCode.hide();
		} else {
			fldProgramPath.show();
			fldProgramCode.hide();
			
			var jobType = job.jobType;
			if (jobType == 20 || jobType == 40) {
				fldProgramCode.show();
				
				fldProgramCode.setValue('正在加载程序代码...');
				Ext.Ajax.request({
					url: 'task/programCode',
					params: {taskId: task.taskId},
					success: function(response) {
						var data = null;
						try {
							data = Ext.decode(response.responseText);
						} catch (e) {
							data = null;
						}
						
						fldProgramCode.setValue(data);
					}
				});
			}
		}
		
		// 只有存储过程作业才有程序参数字段
		if (job.jobType == 42) {
			fldParameters.show();
		} else {
			fldParameters.hide();
		}
		
		if (fldNewJobLevel) {
			// 设置新的优先级
			fldNewJobLevel.setValue(task.jobLevel);
		}
		
		if (fldNewTaskStatus) {
			// 设置新的状态
			fldNewTaskStatus.setValue(task.taskStatus);
		}
		
		if (!S.IS_AUTHORIZED_USER_GROUP(task.userGroup.userGroupId) && !USER_IS_ADMINISTRTOR) {
			// 调整作业优先级
			mdl.masterPnl.items.itemAt(0).items.itemAt(4).hide();
			// 调整作业状态
			mdl.masterPnl.items.itemAt(0).items.itemAt(5).hide();
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var task = data['task'],
		
			jobLevel = task.jobLevel,
			newJobLevel = task.newJobLevel,
			
			taskStatus = task.taskStatus,
			newTaskStatus = task.newTaskStatus;

		data.taskId = task.taskId;
		
		if (jobLevel != newJobLevel) {
			data.jobLevel = newJobLevel;
		}
		
		if (taskStatus != newTaskStatus) {
			data.taskStatus = newTaskStatus;
		}
		
		data.preTasksFromOperate = task.preTasksFromOperate;
		
		delete data['task'];
	}
});