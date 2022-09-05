_package('com.sw.bi.scheduler.job');

_import([
	'framework.modules.MaintainModule',
	
	'com.sw.bi.scheduler.job.JobChoosePanel'
]);

com.sw.bi.scheduler.job.JobMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	buttonSaveHidden: false,
	
	defaultHour: '00',
	defaultMinute: '05',
	
	/**
	 * 是否复制
	 * @type Boolean
	 */
	isCopy: false,
	
	/**
	 * 是否保存并同时上线
	 * @type Boolean
	 */
	saveOnline: false,
	
	/**
	 * 上线成功
	 * @type Boolean
	 */
	onlineSuccess: false,
	
	initModule: function() {
		var mdl = this;

		this.buttons = [{
			id: 'btnSaveAndOnline',
			iconCls: 'save',
			hidden: true,
			text: '保存并上线',
			handler: function() {
				mdl.saveOnline = true;
				mdl.onlineSuccess = false;
				mdl.save(function() {
					mdl.moduleWindow.close();
				});
			}
		}, {
			id: 'btnSaveAndOnlineSuccess',
			iconCls: 'save',
			hidden: true,
			text: '保存并上线成功',
			handler: function() {
				mdl.saveOnline = true;
				mdl.onlineSuccess = true;
				mdl.save(function() {
					mdl.moduleWindow.close();
				});
			}
		}];
		
		com.sw.bi.scheduler.job.JobMaintainModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			savecomplete: this.onSaveComplete,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'job',
			
			items: [{
				xtype: 'hidden', name: 'jobStatus', value: JOB_STATUS.UNLINE
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'jobType',
				fieldLabel: '类型',
				allowBlank: false,
				labelWidth: 70,
				store: S.create('jobType'),
				anchor: '49%',
				value: this.jobType,
				readOnly: true
			}, {
				columnWidth: .5,
				name: 'jobName',
				fieldLabel: '名称',
				labelWidth: 70,
				anchor: '98%',
				maxLength: 100,
				allowBlank: false
			}, {
				columnWidth: .2,
				xtype: 'combo',
				hiddenName: 'gateway',
				fieldLabel: '执行网关机',
				labelWidth: 70,
				anchor: '100%',
				readOnly: USER_IS_ADMINISTRTOR ? false : true, // 该字段只允许管理员操作
				store: S.create('gatewaysByJobType', this.jobType)
			}, {
				columnWidth: .7,
				name: 'programPath',
				fieldLabel: '程序路径',
				labelWidth: 70,
				anchor: '100%',
				allowBlank: false
			}, {
				columnWidth: .5,
				xtype: 'combo',
				hiddenName: 'cycleType',
				fieldLabel: '周期',
				allowBlank: false,
				store: S.create('jobCycleType'),
				labelWidth: 70,
				anchor: '98%',
				
				listeners: {
					select: this.onJobCycleSelect,
					scope: this
				}
			}, {
				columnWidth: .08,
				xtype: 'combo',
				hidden: true,
				hiddenName: 'dayN',
				fieldLabel: '天',
				labelWidth: 20,
				anchor: '98%',
				allowBlank: false,
				store: S.create('days')
			}, {
				columnWidth: .08,
				xtype: 'combo',
				hidden: true,
				hiddenName: 'weekN',
				fieldLabel: '周几',
				labelWidth: 30,
				anchor: '98%',
				allowBlank: false,
				store: S.create('weeks')
			}, {
				columnWidth: .08,
				xtype: 'combo',
				hiddenName: 'hourN',
				fieldLabel: '小时',
				labelWidth: 30,
				anchor: '98%',
				store: S.create('hours'),
				value: this.defaultHour
			}, {
				columnWidth: .08,
				xtype: 'combo',
				hiddenName: 'minuteN',
				fieldLabel: '分钟',
				labelWidth: 30,
				anchor: '98%',
				store: S.create('minutes'),
				value: this.defaultMinute
			}, {
				id: 'lblCycle',
				xtype: 'label',
				text: '(注: 分钟周期时该分钟表示为间隔分钟数)'
			}, {
				columnWidth: 1,
				name: 'jobBusinessGroup',
				fieldLabel: '业务组',
				labelWidth: 70,
				anchor: '49%',
				maxLength: 200
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'dutyOfficer',
				fieldLabel: '责任人',
				allowBlank: false,
				store: S.create('usersByUserGroup', USER_GROUP_ID),
				value: USER_ID,
				labelWidth: 70,
				anchor: '20%'
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'jobLevel',
				fieldLabel: '优先级',
				labelWidth: 70,
				store: S.create('jobLevel'),
				anchor: '20%',
				allowBlank: false,
				value: 3,
				listeners: {
					select: this.validateParentJobLevel,
					scope: this
				}
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'alert',
				fieldLabel: '警告',
				labelWidth: 70,
				store: S.create('alertType'),
				allowBlank: false,
				anchor: '20%'
			}, {
				columnWidth: .25,
				xtype: 'numberfield',
				name: 'failureRerunTimes',
				labelWidth: 70,
				fieldLabel: '重跑次数',
				anchor: '98%'
			}, {
				columnWidth: .25,
				xtype: 'numberfield',
				name: 'failureRerunInterval',
				fieldLabel: '重跑间隔(分钟)',
				labelWidth: 100,
				anchor: '97%'
			}, {
				columnWidth: .25,
				xtype: 'combo',
				hiddenName: 'endHourN',
				labelWidth: 70,
				fieldLabel: '结束时间',
				anchor: '98%',
				emptyText: '小时',
				store: S.create('hours')
			}, {
				columnWidth: .25,
				xtype: 'combo',
				hiddenName: 'endMinuteN',
				hideLabel: true,
				anchor: '97%',
				emptyText: '分钟',
				store: S.create('minutes')
			}, {
				columnWidth: 1,
				// vtype: 'mutilnum',
				name: 'prevJobs',
				fieldLabel: '前置作业',
				labelWidth: 70
			}, {
				columnWidth: 1,
				id: 'pnlJob',
				xtype: 'jobchoose',
				fieldLabel: '父作业',
				labelWidth: 70,
				anchor: '99%'
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'jobDesc',
				labelWidth: 70,
				height: 200,
				maxLength: 200,
				fieldLabel: '描述'
			}],
			
			setReadOnly: function(readonly) {
				framework.widgets.MaintainFormPanel.prototype.setReadOnly.call(this, readonly);
				
				Ext.getCmp('pnlJob').setReadOnly(readonly);
			}
		};
	},
	
	/**
	 * 检验父任务
	 */
	validateParentJob: function() {
		var plg = this.getModuleValidatePlugin(),
		
			jobId = this.findField('jobId').getValue(),
			cycleType = this.findField('cycleType').getValue(),
			parentJobs = Ext.getCmp('pnlJob').getValues();

		if (jobId != 1 && parentJobs.length == 0) {
			plg.addError({
				msg: '<span class="fieldLabel">"父作业"</span> 不允许为空'
			});
			
			return false;
		}
		
		for (var i = 0; i < parentJobs.length; i++) {
			var parent = parentJobs[i];
			
			// 虚拟作业是允许的
			if (parent.jobType == 100) {
				if (cycleType == JOB_CYCLE_TYPE.NONE && parent.cycleType == JOB_CYCLE_TYPE.HOUR) {
					plg.addError({
						msg: '待触发作业的 <span class="fieldLabel">"父作业"</span> 只允许为 "天周期的虚拟作业"'
					});
					return false;
				}
				
				continue;
			}
			
			// 当子作业为分钟作业时父作业必须是虚拟作业
			/*if (cycleType == JOB_CYCLE_TYPE.MINUTE || cycleType == JOB_CYCLE_TYPE.NONE) {
				plg.addError({
					msg: (cycleType == JOB_CYCLE_TYPE.MINUTE ? '分钟' : '待触发') + '作业的 <span class="fieldLabel">"父作业"</span> 只允许为 "虚拟作业"'
				});
				
				return false;
			}*/
			// 当子作业为待触发作业时父作业必须是虚拟作业
			if (cycleType == JOB_CYCLE_TYPE.NONE) {
				plg.addError({
					msg: '待触发作业的 <span class="fieldLabel">"父作业"</span> 只允许为 "虚拟作业"'
				});
				
				return false;
			}

			if (VALID_PARENT_CHILD_RELATIONS[parent.cycleType + '' + cycleType] !== true) {
				var cycleTypes;
				if (cycleType == JOB_CYCLE_TYPE.MONTH) {
					cycleTypes = '"月"、"周" 和 "天"';
				} else if (cycleType == JOB_CYCLE_TYPE.WEEK) {
					cycleTypes = '"周" 和 "天"';
				} else if (cycleType == JOB_CYCLE_TYPE.DAY) {
					cycleTypes = '"天" 和 "小时"';
				} else if (cycleType == JOB_CYCLE_TYPE.HOUR) {
					cycleTypes = '"小时" 和 "分钟"';
				} else if (cycleType == JOB_CYCLE_TYPE.MINUTE) {
					cycleTypes = '"分钟"';
				}
				
				plg.addError({
					msg: '<span class="fieldLabel">"父作业"</span> 只允许类型为 "虚拟作业" 或周期为 ' + cycleTypes + ' 的作业'
				});
				
				return false;
			}
		
			var dayN = parseInt(this.findField('minuteN').getValue(), 10);
			if (cycleType == JOB_CYCLE_TYPE.MINUTE && dayN != parseInt(parent.dayN, 10)) {
				plg.addError({
					msg: '"子作业"与"父作业(' + parent.jobId + ')"的间隔分钟不一致'
				});
				
				return false;
			}
		};
		
		return true;
	},
	
	/**
	 * 校验当前作业与父作业的优先级,并高亮显示(当前作业的优先级必须小于等于父作业优先级)
	 */
	validateParentJobLevel: function() {
		var jobLevel = this.findField('jobLevel').getValue();

		Ext.getCmp('pnlJob').getComponent(0).getComponent(0).store.each(function(parent) {
			parent.set('lower', jobLevel > parent.get('jobLevel'));
		});
	},
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	onLoadDataComplete: function(mdl, data) {
		var job = data['job'],
			action = mdl.moduleWindow ? mdl.moduleWindow.action : null;

		if (action != null && action != 'create') {
			var cmbCycleType = mdl.findField('cycleType'),
				cmbDay = mdl.findField('dayN'),
				cmbWeek = mdl.findField('weekN'),
				cmbHour = mdl.findField('hourN'),
				cmbMinute = mdl.findField('minuteN'),
				cmbEndHour = mdl.findField('endHourN'),
				cmbEndMinute = mdl.findField('endMinuteN'),
				
				jobTime = job.jobTime,
				endTime = job.endTime,
				dayN = job.dayN,
				cycleType = job.cycleType;
				
			// 当前是否是复制操作
			if (mdl.isCopy === true) {
				mdl.findField('jobId').setValue(null);
				mdl.findField('jobStatus').setValue(JOB_STATUS.UNLINE);
				
				job.jobId = null;
				job.jobStatus = JOB_STATUS.UNLINE;
			}
			
			mdl.onJobCycleSelect(cmbCycleType);
			
			if (cycleType != 1) {
				// 不是月作业时默认天为1
				cmbDay.setValue(1);
			} else if (cycleType != 2) {
				// 不是周作业时默认周几为周一
				cmbWeek.setValue(2);
			}
				
			if (cycleType == JOB_CYCLE_TYPE.WEEK) {
				cmbWeek.setValue(dayN);
			} else if (cycleType == JOB_CYCLE_TYPE.MINUTE) {
				cmbMinute.setValue(dayN);
			}
			
			if (!Ext.isEmpty(jobTime, false)) {
				if (jobTime.indexOf(':') == -1) {
					cmbMinute.setValue(jobTime);
				} else {
					var times = jobTime.split(':');
					cmbHour.setValue(times[0]);
					cmbMinute.setValue(times[1]);
				}
			}
			
			if (!Ext.isEmpty(endTime, false)) {
				var times = endTime.split(':');
				cmbEndHour.setValue(times[0]);
				cmbEndMinute.setValue(times[1] == '00' ? null : times[1]);
			}
			
			// 父作业
			var parentJobs = data.parentJobs;
			if (parentJobs && parentJobs.length > 0) {
				var pnlChooseJob = Ext.getCmp('pnlJob');
				pnlChooseJob.job = job;
				pnlChooseJob.setValues(parentJobs);
			}
			
			// 作业查看时“天”、“周几”这二个字段宽度会变的很窄，暂时只能通过调整窗口大小来解决这个问题
			if (action == 'viewOnly') {
				mdl.moduleWindow.setHeight(mdl.moduleWindow.getHeight() - 1);
				
			} else if (action == 'updateOnly') {
				// 修改作业时作业周期不允许被修改
				mdl.findField('cycleType').setReadOnly(true);
				mdl.findField('jobType').setReadOnly(true);
			}
			
			Ext.getCmp('btnSaveAndOnline').setVisible(action != 'viewOnly');
			Ext.getCmp('btnSaveAndOnlineSuccess').setVisible(action != 'viewOnly');
			mdl.validateParentJobLevel();
		}
	},
	
	onJobCycleSelect: function(combo, record, selectIndex) {
		var mdl = this,
			action = Ext.isEmpty(mdl.moduleWindow) ? 'create' : mdl.moduleWindow.action;
			
			cycleType = combo.getValue(),

			cmbAlert = mdl.findField('alert'),
			cmbDay = mdl.findField('dayN'),
			cmbWeek = mdl.findField('weekN'),
			cmbHour = mdl.findField('hourN'),
			cmbMinute = mdl.findField('minuteN'),
			cmbEndHour = mdl.findField('endHourN'),
			cmbEndMinute = mdl.findField('endMinuteN'),
			
			fldPrevJobs = mdl.findField('prevJobs'),
			lblCycle = Ext.getCmp('lblCycle'),
			pnlJob = Ext.getCmp('pnlJob');
			
		lblCycle.setText('(注: 分钟周期时该分钟表示为间隔分钟数)');
		
		if (action == 'create') {
			cmbAlert.setValue(0);
			cmbAlert.setReadOnly(false);
			
			cmbHour.setValue(mdl.defaultHour);
			cmbMinute.setValue(mdl.defaultMinute);
		}
		
		cmbEndHour.setVisible(cycleType == JOB_CYCLE_TYPE.MINUTE);
		cmbEndMinute.setVisible(cycleType == JOB_CYCLE_TYPE.MINUTE);
		fldPrevJobs.show();

		pnlJob.onlyVirtual = this.findField('jobType').getValue() == 100;

		if (cycleType == JOB_CYCLE_TYPE.MONTH) {
			cmbDay.show();
			cmbWeek.hide();
			
			cmbDay.allowBlank = false;
			cmbWeek.allowBlank = true;
			
		} else if (cycleType == JOB_CYCLE_TYPE.WEEK) {
			cmbDay.hide();
			cmbWeek.show();
			
			cmbDay.allowBlank = true;
			cmbWeek.allowBlank = false;
			
		} else {
			cmbDay.hide();
			cmbWeek.hide();
			
			cmbDay.allowBlank = true;
			cmbWeek.allowBlank = true;
		}
		
		if (cycleType == JOB_CYCLE_TYPE.HOUR || cycleType == JOB_CYCLE_TYPE.MINUTE) {
			cmbHour.hide();
			cmbMinute.show();
			
			cmbHour.allowBlank = true;
			cmbMinute.allowBlank = false;
			
		} else if (cycleType == JOB_CYCLE_TYPE.NONE) {
			cmbDay.hide();
			cmbWeek.hide();
			cmbHour.hide();
			cmbMinute.hide();
			fldPrevJobs.hide();
			
			cmbAlert.setValue(2);
			cmbAlert.setReadOnly(true);
			
			lblCycle.setText('(注: 待触发周期的作业不会被调度系统自动触发, 只会由分支作业触发执行)');
			
			pnlJob.onlyVirtual = true;
			
			cmbDay.allowBlank = true;
			cmbWeek.allowBlank = true;
			cmbHour.allowBlank = true;
			cmbMinute.allowBlank = true;
			
		} else {
			cmbHour.show();
			cmbMinute.show();
			
			cmbHour.allowBlank = false;
			cmbMinute.allowBlank = false;
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
			
			cycleType = job.cycleType,
			jobLevel = job.jobLevel,
			dayN = job.dayN,
			weekN = job.weekN,
			hourN = job.hourN,
			minuteN = job.minuteN,
			endHourN = job.endHourN,
			endMinuteN = job.endMinuteN,
			
			jobTime = '',
			endTime = null,
			
			plg = mdl.getModuleValidatePlugin(),
			parentJobs = Ext.getCmp('pnlJob').getValues(),
			storeLevel = S.create('jobLevel');

		if (mdl.validateParentJob()) {
			var ids = [],
				idx = 0;
			
			Ext.iterate(parentJobs, function(parent) {
				ids.push(parent.jobId);
			});
			data.parentJobs = ids.join(',');
			
		} else {
			plg.showErrors();
			return false;
		}
			
		if (Ext.isEmpty(hourN, false)) {
			hourN = this.defaultHour;
		}
		
		if (Ext.isEmpty(minuteN, false)) {
			minuteN = this.defaultMinute;
		}
		
		jobTime = hourN + ':' + minuteN;
		
		delete job['dayN'];
		delete job['weekN'];
		delete job['hourN'];
		delete job['minuteN'];
		delete job['jobTime'];
		
		if (cycleType == JOB_CYCLE_TYPE.MONTH) {
			dayN = dayN;
			
		} else if (cycleType == JOB_CYCLE_TYPE.WEEK) {
			dayN = weekN;
			
		} else if (cycleType == JOB_CYCLE_TYPE.HOUR) {
			jobTime = minuteN;
			
		} else if (cycleType == JOB_CYCLE_TYPE.MINUTE) {
			dayN = parseInt(minuteN, 10);
			jobTime = '';
			
			// 小时未选时不允许加上默认值,否则创建可以成功，但修改时会默认设置为0点
			// 这就导致了第二天任务生成时只生成了一条记录
			/*if (Ext.isEmpty(endHourN, false)) {
				endHourN = this.defaultHour;
			}*/
			
			if (Ext.isEmpty(endMinuteN, false)) {
				endMinuteN = '00';
			}
			
			if (!Ext.isEmpty(endHourN, false) && !Ext.isEmpty(endMinuteN, false)) {
				endTime = endHourN + ':' + endMinuteN;
			}
			
		} else if (cycleType == JOB_CYCLE_TYPE.NONE) {
			dayN = null;
			jobTime = '';
			
		} else {
			dayN = null;
		}

		job.dayN = dayN;
		job.jobTime = jobTime;
		job.endTime = endTime;
		
		job.updateBy = USER_ID;
		job.updateTime = new Date().format('Y-m-d H:i:s');
		
		data['saveOnline'] = mdl.saveOnline;
		data['onlineSuccess'] = mdl.onlineSuccess;
		mdl.saveOnline = false;
		mdl.onlineSuccess = false;
	},
	
	onSaveComplete: function(mdl, result, data) {
		if (Ext.isEmpty(mdl.moduleWindow)) {
			Ext.Msg.alert('提示', '作业已经成功保存!', function() {
				var appContainer = mdl.moduleContainer;
				if (appContainer) {
					appContainer.reload();
				}
			});
		}
	}
});

var VALID_PARENT_CHILD_RELATIONS = {
	'11': true, // 月-月
	'21': true, // 周-月
	'22': true,	// 周-周
	'31': true, // 天-月
	'32': true, // 天-周
	'33': true, // 天-天
	'43': true, // 小时-天
	'44': true, // 小时-小时
	'54': true, // 分钟-小时
	'55': true, // 分钟-分钟
	'63': true // 待触发-天
};