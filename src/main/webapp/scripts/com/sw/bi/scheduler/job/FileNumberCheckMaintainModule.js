_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.FileNumberCheckMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	minHeight: 900,
	jobType: 10,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.FileNumberCheckMaintainModule.superclass.master.call(this),
			items = master.items;
		
		// 执行网关机
		items[3].store = S.create('gatewaysByJobType', 10);
		
		// 程序路径
		items[4].hidden = true;
		items[4].allowBlank = true;
		
		// 责任人
		items[12].columnWidth = .25;
		items[12].anchor = '98%';
		
		// 优先级
		items[13].columnWidth = .25;
		items[13].anchor = '97%';
		items[13].labelWidth = 40;
		
		// 告警
		items[14].columnWidth = .5;
		items[14].anchor = '49%';
		items[14].labelWidth = 30;
		
		// 描述
		items[items.length - 1].height = 80;
		
		items.push({
			columnWidth: 1,
			xtype: 'fieldset',
			title: '作业配置',
			anchor: '99%',
			labelWidth: 110,
			
			defaultType: 'textfield',
			
			items: [{
				xtype: 'numberfield',
				name: 'checkSeconds',
				fieldLabel: '检查间隔(秒)',
				anchor: '40%'
			}, {
				xtype: 'numberfield',
				name: 'timeoutMinutes',
				fieldLabel: '超时分钟数',
				anchor: '40%'
			}, {
				xtype: 'textfield',
				name: 'fileUniquePattern',
				fieldLabel: '文件唯一标识',
				anchor: '40%'
			}, {
				xtype: 'combo',
				name: 'hasSuccessFlag',
				fieldLabel: '需要完成标记',
				anchor: '40%',
				allowBlank: false,
				store: S.create('yesNo'),
				value: false,
				listeners: {
					select: this.onSuccessFlagSelect,
					scope: this
				}
			}, {
				name: 'successFlag',
				hidden: true,
				fieldLabel: '完成标记',
				anchor: '40%'
			}, {
				xtype: 'numberfield',
				fieldLabel: '文件数量',
				name: 'fileNumber',
				anchor: '40%',
				allowBlank: false,
				minValue: 1
			}, {
				xtype: 'panel',
				layout: 'column',
				border: false,
				
				items: [{
					columnWidth: .09,
					border: false,
					items: {
						xtype: 'label',
						id: 'fileNamePosition',
						text: '文件名中日期定位: '
					}
				}, {
					columnWidth: .08,
					layout: 'form',
					border: false,
					labelWidth: 14,
					items: {
						xtype: 'textfield',
						name: 'posYear',
						fieldLabel: '年',
						anchor: '99%',
						allowBlank: false,
						vtype: 'position'
					}
				}, {
					columnWidth: .08,
					layout: 'form',
					border: false,
					labelWidth: 14,
					items: {
						xtype: 'textfield',
						name: 'posMonth',
						fieldLabel: '月',
						anchor: '99%',
						allowBlank: false,
						vtype: 'position'
					}
				}, {
					columnWidth: .08,
					layout: 'form',
					border: false,
					labelWidth: 14,
					items: {
						xtype: 'textfield',
						name: 'posDate',
						fieldLabel: '日',
						anchor: '99%',
						allowBlank: false,
						vtype: 'position'
					}
				}, {
					columnWidth: .08,
					layout: 'form',
					border: false,
					labelWidth: 14,
					items: {
						xtype: 'textfield',
						name: 'posHour',
						fieldLabel: '时',
						anchor: '99%',
						vtype: 'position'
					}
				}, {
					columnWidth: .08,
					layout: 'form',
					border: false,
					labelWidth: 14,
					items: {
						xtype: 'textfield',
						name: 'posMinute',
						fieldLabel: '分',
						anchor: '99%',
						vtype: 'position'
					}
				}, {
					columnWidth: .2,
					layout: 'form',
					border: false,
					labelWidth: 14,
					items: {
						id: 'sample',
						xtype: 'label',
						text: '(示例: 2012-02-02.txt中,年的定位是0,4)'
					}
				}]
			}, {
				name: 'linuxTmpDir',
				fieldLabel: 'Linux本地目录',
				allowBlank: false,
				anchor: '100%'
			}]
		});	
		
		return master;
	},
	
	/**
	 * 删除指定路径中最后一个'/'
	 * @param {} path
	 */
	trimSeparator: function(path) {
		if (!Ext.isEmpty(path, false) && path.charAt(path.length - 1) == '/') {
			return path.substring(0, path.length - 1);
		}
		
		return path;
	},
	
	onLoadDataComplete: function(mdl, data) {
		com.sw.bi.scheduler.job.FileNumberCheckMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
		var config = data['jobDatasyncConfig'];
		if (!Ext.isEmpty(config)) {
			delete config.jobId;
			delete config.jobType;
			
			var dateTimePosition = config.dateTimePosition;
			if (!Ext.isEmpty(dateTimePosition)) {
				var positions = dateTimePosition.split('|');
				for (var i = 0; i < positions.length; i++) {
					var pos = positions[i];
					if (Ext.isEmpty(pos, false)) {
						continue;
					}
					
					if (i == 0) {
						config.posYear = pos;
					} else if (i == 1) {
						config.posMonth = pos;
					} else if (i == 2) {
						config.posDate = pos;
					} else if (i == 3) {
						config.posHour = pos;
					} else if (i == 4) {
						config.posMinute = pos;
					}
				}
			}

			mdl.masterPnl.form.setValues(config);
			
			var fldHasSuccess = mdl.findField('hasSuccessFlag');
			fldHasSuccess.setValue(!Ext.isEmpty(config.successFlag, false));
			mdl.onSuccessFlagSelect(fldHasSuccess);
		}
		
		// 作业查看时“来源数据源”字段宽度会变的很窄，暂时只能通过调整窗口大小来解决这个问题
		var action = mdl.moduleWindow ? mdl.moduleWindow.action : null;
		if (action == 'viewOnly') {
			mdl.moduleWindow.setWidth(mdl.moduleWindow.getWidth() - 5);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'];
		
		job.linuxTmpDir = mdl.trimSeparator(job.linuxTmpDir);
		
		if (!Ext.isEmpty(job.successFlag, false)) {
			job.dateTimePositions = null;
			job.fileNumber = null;
			
		} else {
			job.successFlag = null;
			
			var positions = [];
			positions.push(job.posYear || '');
			positions.push(job.posMonth || '');
			positions.push(job.posDate || '');
			positions.push(job.posHour || '');
			positions.push(job.posMinute || '');
			job.dateTimePosition = positions.join('|');
			
			delete job.posYear;
			delete job.posMonth;
			delete job.posDate;
			delete job.posHour;
			delete job.posMinute;
		}

		return com.sw.bi.scheduler.job.FileNumberCheckMaintainModule.superclass.onBeforeSave.apply(this, arguments);
	},
	
	onSuccessFlagSelect: function(combo) {
		var hasSuccessFlag = combo.getValue(),
		
			fldSuccess = this.findField('successFlag'),
			fldCheck = this.findField('checkSeconds'),
			fldTimeout = this.findField('timeoutMinutes'),
			
			lblFileNamePosition = Ext.getCmp('fileNamePosition'),
			lblSample = Ext.getCmp('sample');
			fldPosYear = this.findField('posYear');
			fldPosMonth = this.findField('posMonth');
			fldPosDate = this.findField('posDate');
			fldPosHour = this.findField('posHour');
			fldPosMinute = this.findField('posMinute');
			
			fldFileNumber = this.findField('fileNumber');
		
		if (hasSuccessFlag) {
			fldSuccess.show();
			fldCheck.show();
			fldTimeout.show();
			
			lblFileNamePosition.hide();
			fldPosYear.hide();
			fldPosMonth.hide();
			fldPosDate.hide();
			fldPosHour.hide();
			fldPosMinute.hide();
			lblSample.hide();
			fldFileNumber.hide();
			
			fldSuccess.allowBlank = false;
			fldCheck.allowBlank = false;
			fldTimeout.allowBlank = false;
			fldPosYear.allowBlank = true;
			fldPosMonth.allowBlank = true;
			fldPosDate.allowBlank = true;
			fldPosHour.allowBlank = true;
			fldPosMinute.allowBlank = true;
			fldFileNumber.allowBlank = true;
			
		} else {
			fldSuccess.hide();
			
			fldCheck.show();
			fldTimeout.show();
			lblFileNamePosition.show();
			fldPosYear.show();
			fldPosMonth.show();
			fldPosDate.show();
			fldPosHour.show();
			fldPosMinute.show();
			lblSample.show();
			fldFileNumber.show();
			
			fldSuccess.allowBlank = true;
			fldCheck.allowBlank = false;
			fldTimeout.allowBlank = false;
			fldPosYear.allowBlank = false;
			fldPosMonth.allowBlank = false;
			fldPosDate.allowBlank = false;
			fldPosHour.allowBlank = true;
			fldPosMinute.allowBlank = true;
			fldFileNumber.allowBlank = false;
		}
	}
});

Ext.apply(Ext.form.VTypes, {
	'position': function(v) {
		var reg = /^\d+(\,\d+)+$/g;
		return reg.test(v);
	},
	'positionText': '位置的正确格式为: <b>开始位置,结束位置</b>, 如: 1,2'
});