_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.ReportQuotaMonitorMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	minHeight: 800,
	jobType: 91,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.ReportQuotaMonitorMaintainModule.superclass.master.call(this),
			items = master.items;
			
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
			title: '报表质量监控作业配置',
			anchor: '99%',
			
			labelWidth: 120,
			defaultType: 'textfield',
			
			items: [{
				name: 'tablename',
				fieldLabel: '要监控的Hive表名',
				allowBlank: false,
				anchor: '50%'
			}, {
				xtype: 'label',
				html: '<span style="color:red">(注: Hive表名前必须显式加上库名,如tools.dual)</span>'
			}, {
				name: 'ptName',
				fieldLabel: '要监控的分区名',
				allowBlank: false,
				anchor: '50%'
			}, {
				xtype: 'combo',
				name: 'ptType',
				fieldLabel: '要监控的分区类型',
				allowBlank: false,
				anchor: '50%',
				store: S.create('partitionType')
			}, {
				xtype: 'combo',
				hiddenName: 'monitorTotalNumber',
				fieldLabel: '监控总记录数',
				allowBlank: false,
				anchor: '20%',
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[true, '监控'],
						[false, '不监控']
					]
				}),
				listeners: {
					select: this.onMonitorTotalNumberSelect,
					scope: this
				}
			}, {
				xtype: 'numberfield',
				name: 'upAndDown',
				fieldLabel: '总记录数波动百分比',
				allowBlank: false,
				anchor: '50%',
				minValue: 0,
				maxValue: 1
			}, {
				xtype: 'combo',
				hiddenName: 'monitorQuata',
				fieldLabel: '监控报表指标',
				allowBlank: false,
				anchor: '20%',
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[true, '监控'],
						[false, '不监控']
					]
				}),
				listeners: {
					select: this.onMonitorQuataSelect,
					scope: this
				}
			}, {
				name: 'weidu',
				allowBlank: false,
				fieldLabel: '维度清单(逗号分隔)',
				anchor: '50%',
				vtype: 'weidu'
			}, {
				name: 'quata',
				allowBlank: false,
				fieldLabel: '监控的指标及波动值',
				anchor: '50%',
				vtype: 'quata'
			}, {
				xtype: 'combo',
				hiddenName: 'alertWay',
				fieldLabel: '告警方式',
				value: 2,
				allowBlank: false,
				anchor: '20%',
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[0, '邮件'],
						[1, '短信'],
						[2, '两者']
					]
				}),
				listeners: {
					select: this.onAlertWaySelect,
					scope: this
				}
			}, {
				name: 'email',
				fieldLabel: '告警信箱(逗号分隔)',
				allowBlank: false,
				vtype: 'mutilemail'
			}, {
				name: 'mobilePhone',
				fieldLabel: '告警手机(逗号分隔)',
				allowBlank: false,
				vtype: 'mutilmobile'
			}]
		});
		
		return master;
	},
	
	/////////////////////////////////////////////////////////////////////////////////
	
	onMonitorTotalNumberSelect: function(combo) {
		var mdl = this,
			fldUpAndDown = mdl.findField('upAndDown');
		
		if (combo.getValue()) {
			fldUpAndDown.allowBlank = false;
			fldUpAndDown.show();
		} else {
			fldUpAndDown.allowBlank = true;
			fldUpAndDown.hide();
		}
	},
	
	onMonitorQuataSelect: function(combo) {
		var mdl = this,
			fldWeidu = mdl.findField('weidu'),
			fldQuata = mdl.findField('quata');
			
		if (combo.getValue()) {
			fldWeidu.allowBlank = false;
			fldQuata.allowBlank = false;
			
			fldWeidu.show();
			fldQuata.show();
			
		} else {
			fldWeidu.allowBlank = true;
			fldQuata.allowBlank = true;
			
			fldWeidu.hide();
			fldQuata.hide();
		}
	},
	
	onAlertWaySelect: function(combo) {
		var mdl = this,
		
			alertWay = combo.getValue(),
			fldEmail = mdl.findField('email'),
			fldMobilePhone = mdl.findField('mobilePhone');
			
		if (alertWay == 0) {
			fldEmail.allowBlank = false;
			fldMobilePhone.allowBlank = true;
			
			fldEmail.show();
			fldMobilePhone.hide();
		} else if (alertWay == 1) {
			fldEmail.allowBlank = true;
			fldMobilePhone.allowBlank = false;
			
			fldEmail.hide();
			fldMobilePhone.show();
		} else if (alertWay == 2) {
			fldEmail.allowBlank = false;
			fldMobilePhone.allowBlank = false;
			
			fldEmail.show();
			fldMobilePhone.show();
		}
	},
	
	onLoadDataComplete: function(mdl, data) {
		com.sw.bi.scheduler.job.ReportQuotaMonitorMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
		var job = data['job'],
			config = data['reportsQualityMonitorConfig'],
			
			fldMonitorTotalNumber = mdl.findField('monitorTotalNumber'),
			fldMonitorQuata = mdl.findField('monitorQuata'),
			fldAlertWay = mdl.findField('alertWay');
			
		if (!Ext.isEmpty(config)) {
			fldMonitorTotalNumber.setValue(config.monitorTotalNumber);
			mdl.onMonitorTotalNumberSelect(fldMonitorTotalNumber);
			
			fldMonitorQuata.setValue(config.monitorQuata);
			mdl.onMonitorQuataSelect(fldMonitorQuata);
			
			fldAlertWay.setValue(config.alertWay);
			mdl.onAlertWaySelect(fldAlertWay);
		
			delete config.jobId;
			mdl.masterPnl.form.setValues(config);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
		
			monitorTotalNumber = eval(job.monitorTotalNumber),
			monitorQuata = eval(job.monitorQuata),
			alertWay = eval(job.alertWay);
		
		if (monitorTotalNumber === false) {
			job.upAndDown = null;
		}
		
		if (monitorQuata === false) {
			job.weidu = null;
			job.quata = null;
		}
		
		if (alertWay == 0) {
			job.mobilePhone = null;
		} else if (alertWay == 1) {
			job.email = null;
		}

		return com.sw.bi.scheduler.job.ReportQuotaMonitorMaintainModule.superclass.onBeforeSave.apply(this, arguments);
	}
});

Ext.apply(Ext.form.VTypes, {
	'weidu': function(v) {
		var reg = /^\w+(\,\w+)*$/g;
		return reg.test(v);
	},	
	'weiduText': '该输入项必须以逗号分隔',
	
	'quata': function(v) {
		var reg = /^\w+={1}((0\.){1}(0)*(1|2|3|4|5|6|7|8|9)+|0|1)$/g;
		return reg.test(v);
	},
	'quataText': '该输入项的正确格式为：M=0.3，这个格式中的数字必须在0-1这个范围内'
});