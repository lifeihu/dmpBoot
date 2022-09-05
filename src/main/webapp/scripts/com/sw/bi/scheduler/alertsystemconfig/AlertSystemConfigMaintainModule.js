_package('com.sw.bi.scheduler.alertsystemconfig');

_import([
	'framework.modules.MaintainModule',
	'framework.widgets.form.MultiCombo'
]);

com.sw.bi.scheduler.alertsystemconfig.AlertSystemConfigMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	// buttonSaveHidden: false,
	// alertSystemConfigId: 1,
	
	/**
	 * 是否复制
	 * @type Boolean
	 */
	isCopy: false,
	
	initModule: function() {
		com.sw.bi.scheduler.alertsystemconfig.AlertSystemConfigMaintainModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'alertSystemConfig',
			
			items: [{
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'userGroupId',
				fieldLabel: '用户组',
				labelWidth: 120,
				anchor: '80%',
				store: S.create('userGroups'),
				listeners: {
					select: this.onUserGroupSelect,
					scope: this
				}
			}, {
				columnWidth: .809,
				name: 'beginWorkTime',
				fieldLabel: '开始工作时间',
				labelWidth: 120
			}, {
				columnWidth: .191,
				xtype: 'label',
				text: 'HH:mm'
			}, {
				columnWidth: .809,
				name: 'endWorkTime',
				fieldLabel: '结束工作时间',
				labelWidth: 120
			}, {
				columnWidth: .191,
				xtype: 'label',
				text: 'HH:mm'
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'dutyMan',
				fieldLabel: '值周人',
				labelWidth: 120,
				anchor: '80%',
				store: S.create('usersByUserGroup', 0)
			}, {
				columnWidth: 1,
				xtype: 'multicombo',
				hiddenName: 'observeMan',
				fieldLabel: '观察人',
				labelWidth: 120,
				anchor: '80%',
				store: S.create('usersByUserGroup', 0)
			}, {
				columnWidth: .809,
				name: 'scanCycle',
				fieldLabel: '告警扫描周期(分钟)',
				labelWidth: 120
			}, {
				columnWidth: .191,
				xtype: 'label',
				text: '0-9,3;9-18,1;18-24,0'
			}, {
				columnWidth: .809,
				name: 'alertJobRange',
				fieldLabel: '告警的作业范围',
				labelWidth: 120
			}, {
				columnWidth: .191,
				xtype: 'label',
				text: '0-9,3;9-18,1;18-24,0'
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'alertWay',
				fieldLabel: '告警方式',
				labelWidth: 120,
				anchor: '80%',
				allowBlank: false,
				store: S.create('alertWays') /*new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[0, '工作时间以邮件告警，非工作时间以短信告警'],
						[1, '全天以邮件告警'],
						[2, '全天以短信告警']
					]
				})*/
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'alertTarget',
				fieldLabel: '短信告警目标对象',
				labelWidth: 120,
				anchor: '80%',
				allowBlank: false,
				store: S.create('alertTargets') /*new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[0, '告警给作业责任人'],
						[1, '告警给值周人'],
						[2, '同时告警给作业责任人和值周人']
					]
				})*/
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'smsContent',
				fieldLabel: '短信内容组成方式',
				labelWidth: 120,
				anchor: '80%',
				allowBlank: false,
				store: S.create('smsContentWays')/*new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[0, '3个作业信息组成1条短信'],
						[1, '4个作业信息组成1条短信'],
						[2, '5个作业信息组成1条短信'],
						[3, '以70个字为单位组成一条短信，如果超过70个字，则分为2条短信发送']
					]
				})*/
			}, {
				columnWidth: 1,
				xtype: 'datefield',
				name: 'precomputeForWhichdate',
				fieldLabel: '预算日期',
				labelWidth: 120,
				anchor: '80%'
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				vtype: 'mutilemail',
				name: 'alertMaillist',
				fieldLabel: '邮件告警接收邮箱',
				anchor: '80%',
				height: 130,
				labelWidth: 120
			}]
		};
	},
	
	onUserGroupSelect: function(combo, record) {
		
		var mdl = this,
			userGroupId = combo.getValue(),
			
			fldDuty = mdl.findField('dutyMan'),
			fldObserve = mdl.findField('observeMan');
		
		// userGroupId不能为空
		if (userGroupId == null) {
			userGroupId = 0
		}
		
		fldDuty.store.load({params: {
			'userGroupId': userGroupId
		}});
		fldDuty.setValue(null);
		
		fldObserve.store.load({params: {
			'userGroupId': userGroupId
		}});
		fldObserve.setValue(null);
	},
	
	onLoadDataComplete: function(mdl, data) {
		var alertSystemConfig = data['alertSystemConfig'];
		
		if (mdl.isCopy === true) {
			mdl.findField('alertSystemConfigId').setValue(null);			
			alertSystemConfig.alertSystemConfigId = null;
		}
		
		var userGroup = mdl.findField('userGroupId');
		
		// alertSystemConfig 不能为空
		if (alertSystemConfig) {
		    userGroup.setValue(alertSystemConfig.userGroupId);
		    mdl.findField('dutyMan').setValue(alertSystemConfig.dutyMan);
		    mdl.findField('observeMan').setValue(alertSystemConfig.observeMan);
		}
		
		mdl.onUserGroupSelect(userGroup);

		if (mdl.action == 'updateOnly' && mdl.isCopy === false) {
			userGroup.setReadOnly(true);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var alertSystemConfig = data['alertSystemConfig'];
		alertSystemConfig.alertRate = 0;
		alertSystemConfig.alertPolicy = 0;
		
		if (Ext.isEmpty(alertSystemConfig.monitorBeginTime, false)) {
			alertSystemConfig.monitorBeginTime = 0;
		}
		
		if (Ext.isEmpty(alertSystemConfig.monitorEndTime, false)) {
			alertSystemConfig.monitorEndTime = 0;
		}
	}
});