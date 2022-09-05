_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.MailMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	minHeight: 830,
	jobType: 90,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.MailMaintainModule.superclass.master.call(this),
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
			title: '邮件作业配置',
			anchor: '99%',
			
			items: [{
				xtype: 'panel',
				border: false,
				layout: 'column',
				
				items: [{
					columnWidth: .85,
					layout: 'form',
					labelWidth: 65,
					border: false,
					
					items: {
						xtype: 'textfield',
						name: 'mailReceivers',
						fieldLabel: '邮件接收人',
						anchor: '99%',
						allowBlank: false,
						vtype: 'mutilemail'
					}
				}, {
					columnWidth: .15,
					border: false,
					
					items: {
						xtype: 'label',
						text: '多个接收人用逗号分隔'
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					labelWidth: 65,
					border: false,
					items: {
						xtype: 'textfield',
						name: 'mailTitle',
						fieldLabel: '邮件标题',
						anchor: '100%',
						allowBlank: false
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					labelWidth: 65,
					border: false,
					
					items: {
						xtype: 'combo',
						hiddenName: 'mailContentFrom',
						fieldLabel: '内容来源',
						anchor: '50%',
						allowBlank: false,
						store: new Ext.data.SimpleStore({
							fields: ['value', 'name'],
							data: [
								[0, '固定内容'],
								[1, '来自SQL查询']
							]
						}),
						
						listeners: {
							select: this.onContentFromSelect,
							scope: this
						}
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					labelWidth: 65,
					border: false,
					
					items: {
						xtype: 'combo',
						hidden: true,
						hiddenName: 'datasourceId',
						fieldLabel: '数据源',
						store: S.create('databaseDatasources'),
						anchor: '50%'
					}
				}, {
					columnWidth: 1,
					layout: 'form',
					labelWidth: 65,
					border: false,
					items: {
						xtype: 'textarea',
						name: 'mailContent',
						fieldLabel: '邮件内容',
						anchor: '100%',
						allowBlank: false,
						height: 210
					}
				}]
			}]
		});
			
		return master;
	},
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	onContentFromSelect: function(combo, record) {
		var mdl = this,
		
			fldDatasource = mdl.findField('datasourceId'),
			fldMailContent = mdl.findField('mailContent');
			
		if (combo.getValue() == 0) {
			fldDatasource.allowBlank = true;
			fldDatasource.hide();
		} else {
			fldDatasource.allowBlank = false;
			fldDatasource.show();
		}
		
		fldMailContent.setValue(null);
	},
	
	onLoadDataComplete: function(mdl, data) {
		com.sw.bi.scheduler.job.MailMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
		var job = data['job'],		
			config = data['mailJobConfig'],
			action = mdl.moduleWindow ? mdl.moduleWindow.action : null;
		
		if (!Ext.isEmpty(config)) {
			var fldContentFrom = mdl.findField('mailContentFrom');
			if (!Ext.isEmpty(config.datasourceId, false)) {
				fldContentFrom.setValue(1);
				mdl.onContentFromSelect(fldContentFrom);
			} else {
				fldContentFrom.setValue(0);
			}
			
			delete config.jobId;
			mdl.masterPnl.form.setValues(config);
		}
		
		// 作业查看时“来源数据源”字段宽度会变的很窄，暂时只能通过调整窗口大小来解决这个问题
		if (action == 'viewOnly') {
			mdl.moduleWindow.setWidth(mdl.moduleWindow.getWidth() - 3);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
		
			fldMailContent = mdl.findField('mailContent'),
		
			mailContentFrom = job.mailContentFrom,
			mailContent = job.mailContent.toLowerCase(),
			
			plg = mdl.getModuleValidatePlugin();

		if (mailContentFrom == 1) {
			if (USER_IS_ADMINISTRTOR !== true) {
				if (mailContent.indexOf('delete') > -1 || mailContent.indexOf('insert') > -1 || mailContent.indexOf('drop') > -1 || mailContent.indexOf('update') > -1) {
					plg.addError({
						msg: '中不允许出现 <span class="fieldLabel">"INSERT"、"DELETE"、"DROP"、"UPDATE"</span> 关键字!',
						field: fldMailContent
					});
					plg.showErrors();
					
					return false
				}
				
				if (mailContent.indexOf("select") == -1 /*|| mailContent.indexOf("where") == -1*/) {
					plg.addError({
						msg: '中必须出现 <span class="fieldLabel">"SELECT"</span> 关键字!',
						field: fldMailContent
					});
					plg.showErrors();
					
					return false
				}
			}
			
		} else {
			// 固定内容时数据源字段置空
			job.datasourceId = null;
		}
		
		return com.sw.bi.scheduler.job.MailMaintainModule.superclass.onBeforeSave.apply(this, arguments);
	}
});