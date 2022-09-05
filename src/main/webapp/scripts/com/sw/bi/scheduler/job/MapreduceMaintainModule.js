_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.MapreduceMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	jobType: 21,
	
	// add by zhuzhongji 2015年9月11日17:31:21
	minHeight: 750,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.MapreduceMaintainModule.superclass.master.call(this),
			items = master.items;

		items[4].fieldLabel = '执行命令';

		return master;
	}
});