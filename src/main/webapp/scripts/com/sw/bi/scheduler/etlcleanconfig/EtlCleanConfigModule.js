_package('com.sw.bi.scheduler.etlcleanconfig');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.etlcleanconfig.EtlCleanConfigModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'etlCleanConfig',
	
	maintain: {
		title: '生命周期维护',
		width: 400,
		height: 200,
		
		module: {
			module: 'com.sw.bi.scheduler.etlcleanconfig.EtlCleanConfigMaintainModule'
		}
	},
	
	searcher: function() {
		return {
			items: [{
				name: 'tableName',
				fieldLabel: '表名'
			}, {
				name: 'partitionName',
				fieldLabel: '分区名'
			}, {
				xtype: 'combo',
				name: 'partitionType',
				fieldLabel: '分区类型',
				store: S.create('partitionType')
			}, {
				xtype: 'combo',
				hiddenName: 'applyMan-eq',
				fieldLabel: '申请人',
				store: S.create('users')
			}]
		};
	},
	
	detailer: function() {
		return {
			columns: [{
				header: '表名',
				dataIndex: 'tableName',
				width: 300
			}, {
				header: '分区名',
				dataIndex: 'partitionName'
			}, {
				header: '分区类型',
				dataIndex: 'partitionType'
			}, {
				xtype: 'customcolumn',
				header: '申请人',
				dataIndex: 'applyMan',
				store: S.create('users')
			}, {
				header: '保留天数',
				dataIndex: 'keepDays',
				width: 60
			}]
		};
	}
});