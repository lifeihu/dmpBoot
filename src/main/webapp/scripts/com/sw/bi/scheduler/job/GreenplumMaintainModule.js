_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.GreenplumMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	jobType: 8, // GP函数
	
	master: function() {
		var master = com.sw.bi.scheduler.job.GreenplumMaintainModule.superclass.master.call(this),
			items = master.items;
		
		items[1].columnWidth = .5;
		items[1].anchor = '98%';
		
		items.splice(2, 0, {
			columnWidth: .5,
			xtype: 'combo',
			hiddenName: 'datasourceId',
			fieldLabel: 'GP数据源',
			allowBlank: false,
			labelWidth: 70,
			anchor: '40%',
			store: S.create('datasources', DATASOURCE_TYPE.GREENPLUM)
		});

		items[5].fieldLabel = '函数名称';
		
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
			fieldLabel: '函数参数',
			labelWidth: 70,
			anchor: '70%'
		});
		
		return master;
	},
	
	onLoadDataComplete: function(mdl, data) {
		var job = data['job'],
			action = mdl.moduleWindow ? mdl.moduleWindow.action : null;
			
		if (action != null && action != 'create') {
			var programPath = job.programPath,
				tmp = programPath.split(';');

			mdl.findField('datasourceId').setValue(parseInt(tmp[0], 10));
			mdl.findField('programPath').setValue(tmp[1]);
		}
		
		com.sw.bi.scheduler.job.GreenplumMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
		
			datasourceId = job['datasourceId'],
			programPath = job.programPath;
			
		job.programPath = datasourceId + ';' + programPath;
		
		return com.sw.bi.scheduler.job.GreenplumMaintainModule.superclass.onBeforeSave.apply(this, arguments);
	}
});