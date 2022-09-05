_package('com.sw.bi.scheduler.alertsystemlog');

_import([
	'framework.modules.SearchGridModule',
	'com.sw.bi.scheduler.alertsystemlog.AlertSystemLogListModule'
]);

com.sw.bi.scheduler.alertsystemlog.AlertSystemLogModule = Ext.extend(framework.modules.SearchGridModule, {

	searcher: function() {
		return {
			items: [{
				xtype: 'hidden', name: 'userGroupId', value: USER_GROUP_ID
			}, {
				xtype: 'combo',
				store: S.create('quickDateRanges'),
				fieldLabel: '快捷日期',
				listeners: {
					select: this.onQuickDatesSelect,
					scope: this
				}
			}, {
				xtype: 'datefield',
				name: 'alertDate-ge',
				fieldLabel: '告警时间',
				value: new Date().add(Date.DAY, -7)
			}, {
				xtype: 'datefield',
				name: 'alertDate-le',
				fieldLabel: '至',
				value: new Date()
			}, {
				name: 'jobId-in',
				fieldLabel: '作业ID',
				vtype: 'mutilnum'
			}, {
				name: 'jobName',
				fieldLabel: '作业名称'
			}/*, {
				xtype: 'combo',
				hiddenName: 'jobStatus-eq',
				fieldLabel: '作业状态',
				store: S.create('taskStatus')
			}*/, {
				// xtype: 'combo',
				name: 'dutyOfficer',
				fieldLabel: '责任人'/*,
				store: S.create('users')*/
			}, {
				xtype: 'combo',
				hiddenName: 'alertType-eq',
				fieldLabel: '告警类型',
				store: S.create('monitorAlertType')
			}, {
				xtype: 'combo',
				hiddenName: 'alertWay-eq',
				fieldLabel: '告警方式',
				store: S.create('monitorAlertWay')
			}]
		};
	},
	
	detailer: function() {
		return new com.sw.bi.scheduler.alertsystemlog.AlertSystemLogListModule();
	},
	
	onQuickDatesSelect: function(combo) {
		var days = combo.getValue(),
			endDate = new Date().add(Date.DAY, -1),
			startDate = endDate.add(Date.DAY, days * -1),
			
			frmSearch = this.northPnl.form;
		
		frmSearch.findField('alertDate-ge').setValue(startDate);
		frmSearch.findField('alertDate-le').setValue(endDate);
		
		this.loadData();
	}
	
});