_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.MySQLProcedureMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	jobType: 42,
	
	// add by zhuzhongji 2015年9月11日17:33:55
	minHeight: 750,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.MySQLProcedureMaintainModule.superclass.master.call(this),
			items = master.items;
		
		items[1].columnWidth = .5;
		items[1].anchor = '98%';
		
		items.splice(2, 0, {
			columnWidth: .5,
			xtype: 'combo',
			hiddenName: 'ftpDatasource.datasourceId',
			fieldLabel: '选择数据源',
			allowBlank: false,
			labelWidth: 77,
			anchor: '46.7%',
			store: S.create('datasources', DATASOURCE_TYPE.ORACLE)
		});

		items[5].fieldLabel = '存储过程名';
		
		// 责任人
		items[13].columnWidth = .25;
		items[13].anchor = '98%';
		
		// 优先级
		items[14].columnWidth = .25;
		items[14].anchor = '97%';
		items[14].labelWidth = 40;
		
		// 告警
		items[15].columnWidth = .5;
		items[15].anchor = '49%';
		items[15].labelWidth = 30;
		
		items.splice(6, 0, {
			columnWidth: 1,
			name: 'parameters',
			fieldLabel: '程序参数',
			labelWidth: 70,
			anchor: '73.3%'
		});
		
		return master;
	},
	
	onLoadDataComplete: function(mdl, data) {
		var job = data['job'],
			action = mdl.moduleWindow ? mdl.moduleWindow.action : null;
			
		if (action != null && action != 'create') {
			var programPath = job.programPath,
				tmp = programPath.split(';');

			mdl.findField('ftpDatasource.datasourceId').setValue(parseInt(tmp[0], 10));
			mdl.findField('programPath').setValue(tmp[1]);
		}
		
		com.sw.bi.scheduler.job.MySQLProcedureMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
		
			ftpDatasourceId = job['ftpDatasource.datasourceId'],
			programPath = job.programPath;
			
		job.programPath = ftpDatasourceId + ';' + programPath;
		
		return com.sw.bi.scheduler.job.MySQLProcedureMaintainModule.superclass.onBeforeSave.apply(this, arguments);
	}
});