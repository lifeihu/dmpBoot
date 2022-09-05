_package('com.sw.bi.scheduler.actionsql');

_import([
	'framework.modules.MaintainModule',
	'framework.widgets.form.DateTimeField'
]);

com.sw.bi.scheduler.actionsql.ActionSqlMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	master: function() {
		return {
			model: 'actionSql',

			items: [{
				columnWidth: .5,
				xtype: 'datefield',
				name: 'taskDate',
				fieldLabel: '任务日期',
				altFormats: 'Y-m-d|Y-m-d G:i:s',
				anchor: '99%'
			}, {
				columnWidth: .5,
				xtype: 'combo',
				name: 'dutyMan',
				fieldLabel: '责任人',
				store: S.create('users'),
				anchor: '100%'
			}, {
				columnWidth: .4,
				xtype: 'datetimefield',
				name: 'beginTime',
				fieldLabel: '开始时间',
				anchor: '99%'
			}, {
				columnWidth: .4,
				xtype: 'datetimefield',
				name: 'endTime',
				fieldLabel: '结束时间',
				anchor: '99%'
			}, {
				columnWidth: .2,
				name: 'runTime',
				fieldLabel: '时长',
				labelWidth: 40,
				anchor: '100%'
			}, {
				columnWidth: 1,
				name: 'hiveSqlPath',
				fieldLabel: '脚本名',
				anchor: '100%'
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'sqlString',
				fieldLabel: 'SQL字符',
				anchor: '100%',
				height: 284
			}]
		};
	}
});