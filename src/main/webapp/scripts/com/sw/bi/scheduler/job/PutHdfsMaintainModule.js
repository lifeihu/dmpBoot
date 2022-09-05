_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.PutHdfsMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	jobType: 9,
	minHeight: 900,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.PutHdfsMaintainModule.superclass.master.call(this),
			items = master.items;
		
		// 执行网关机
		items[3].store = S.create('gatewaysByJobType', 9);
		
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
			title: 'HDFS作业配置',
			anchor: '99%',
			labelWidth: 110,
			
			defaultType: 'textfield',
			
			items: [{
				xtype: 'textfield',
				name: 'fileUniquePattern',
				fieldLabel: '文件唯一标识',
				anchor: '40%'
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
						// allowBlank: false,
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
						// allowBlank: false,
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
						// allowBlank: false,
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
				xtype: 'numberfield',
				fieldLabel: '文件数量',
				name: 'fileNumber',
				anchor: '40%',
				minValue: -1
			}, {
				name: 'linuxTmpDir',
				fieldLabel: 'Linux本地临时目录',
				allowBlank: false,
				anchor: '100%'
			}, {
				xtype: 'combo',
				hiddenName: 'appendDate',
				fieldLabel: '是否追加时间字段',
				anchor: '30%',
				allowBlank: false,
				value: false,
				store: S.create('yesNo'),
				listeners: {
					select: this.onAppendDateSelect,
					scope: this
				}
			}, {
				hidden: true,
				name: 'hiveTableName',
				fieldLabel: 'Hive表名',
				anchor: '100%'
			}, {
				hidden: true,
				name: 'hiveFields',
				fieldLabel: 'Hive字段列表',
				anchor: '100%'
			}, {
				hidden: true,
				xtype: 'textarea',
				name: 'createTableSql',
				fieldLabel: '建表语句',
				anchor: '100%',
				height: 100
			}, {
				name: 'hdfsPath',
				fieldLabel: 'HDFS路径',
				allowBlank: false,
				anchor: '100%'
			}, {
				xtype: 'textfield',
				name: 'referDbName',
				fieldLabel: '关联数据库',
				anchor: '30%'
			}, {
				xtype: 'textfield',
				name: 'referTableName',
				fieldLabel: '关联表名',
				anchor: '30%'
			}, {
				xtype: 'textfield',
				name: 'referPartName',
				fieldLabel: '关联分区名',
				anchor: '30%'
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
		com.sw.bi.scheduler.job.PutHdfsMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
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
			
			if (data['job'].jobId != null) {
				var fldAppendDate = mdl.findField('appendDate');
				if (Ext.isEmpty(config.hdfsPath, false)) {
					fldAppendDate.setValue(true);
					// fldAppendDate.show();
				} else {
					fldAppendDate.setValue(false);
					// fldAppendDate.hide();
				}
			}

			mdl.masterPnl.form.setValues(config);
		}

		mdl.onAppendDateSelect(mdl.findField('appendDate'));
		
		// 作业查看时“来源数据源”字段宽度会变的很窄，暂时只能通过调整窗口大小来解决这个问题
		var action = mdl.moduleWindow ? mdl.moduleWindow.action : null;
		if (action == 'viewOnly') {
			mdl.moduleWindow.setWidth(mdl.moduleWindow.getWidth() - 5);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
		
			jobType = job.jobType,
			judgeWay = job.judgeWay;
		
		job.linuxTmpDir = mdl.trimSeparator(job.linuxTmpDir);
		job.hdfsPath = mdl.trimSeparator(job.hdfsPath);
		
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
		
		if (job.appendDate == 'false') {
			job.hiveTableName = null;
			job.hiveFields = null;
			job.createTableSql = null;
		} else {
			job.hdfsPath = null;
		}
		
		/*if (job.jobType == 5) {
			job.dateTimePositions = null;
			job.fileNumber = null;
			
		} else if (job.jobType == 6 || job.jobType == 7) {
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
		
		if (job.jobType != 6) {
			job.appendDate = false;
			job.hiveTableName = null;
			job.hiveFields = null;
			job.createTableSql = null;
		} else {
			if (job.appendDate == 'true') {
				job.hdfsPath = null;
			}
		}*/

		return com.sw.bi.scheduler.job.PutHdfsMaintainModule.superclass.onBeforeSave.apply(this, arguments);
	},
	
	/**
	 * @deprecated
	 */
	onAppendDateSelect: function(combo) {
		var fldHdfsDir = this.findField('hdfsPath'),
			fldHiveTableName = this.findField('hiveTableName'),
			fldHiveFields = this.findField('hiveFields'),
			fldCreateTableSql = this.findField('createTableSql');
			
			
		if (combo.getValue() === true) {
			fldHiveTableName.show();
			fldHiveFields.show();
			fldCreateTableSql.show();
			
			fldHdfsDir.hide();
			
			fldHdfsDir.allowBlank = true;
			fldHiveTableName.allowBlank = false;
			fldHiveFields.allowBlank = false;
			fldCreateTableSql.allowBlank = false;
			
		} else {
			fldHiveTableName.hide();
			fldHiveFields.hide();
			fldCreateTableSql.hide();
			
			fldHdfsDir.show();
			
			fldHdfsDir.allowBlank = false;
			fldHiveTableName.allowBlank = true;
			fldHiveFields.allowBlank = true;
			fldCreateTableSql.allowBlank = true;
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