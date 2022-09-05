_package('com.sw.bi.scheduler.etlcleanconfig');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.etlcleanconfig.EtlCleanConfigMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,

	master: function() {
		return {
			model: 'etlCleanConfig',
			
			items: [{
				columnWidth: 1,
				name: 'tableName',
				fieldLabel: '表名',
				allowBlank: false
			}, {
				columnWidth: 1,
				name: 'partitionName',
				fieldLabel: '分区名',
				allowBlank: false
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'partitionType',
				fieldLabel: '分区类型',
				allowBlank: false,
				store: S.create('partitionType')
			}, {
				columnWidth: 1,
				xtype: 'numberfield',
				name: 'keepDays',
				fieldLabel: '保留天数',
				allowBlank: false,
				minValue: 3,
				maxValue: 40
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'applyMan',
				fieldLabel: '申请人',
				store: S.create('users'),
				allowBlank: false,
				value: USER_ID
			}]
		}
	}
	
});