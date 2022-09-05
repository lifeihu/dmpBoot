_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.VirtualMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	jobType: 100,
	
	// add by zhuzhongji 2015年9月11日17:37:02
	minHeight: 750,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.VirtualMaintainModule.superclass.master.call(this),
			items = master.items;
			
		// 程序路径
		items[4].xtype = 'hidden';
		items[4].allowBlank = true;
		
		// 作业周期
		items[5].value = 3;
		items[5].store.loadData([
			[JOB_CYCLE_TYPE.DAY, '天'],
			[JOB_CYCLE_TYPE.HOUR, '小时']
		]);
		
		// 小时
		items[8].hidden = true;
		
		// 分钟
		items[9].hidden = true;
		
		// 说明标签
		items[10].hidden = true;
		
		// 优先级
		items[13].hidden = true;
		
		// 是否告警
		items[14].value = 2;
		items[14].hidden = true;
		
		// 父作业
		items[18].onlyVirtual = true;
		
		return master;
	},
	
	onLoadDataComplete: function(mdl, data) {
		com.sw.bi.scheduler.job.VirtualMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
		mdl.findField('hourN').hide();
		mdl.findField('minuteN').hide();
	},
	
	beforeDestroy: function() {
		// 因为之前更改了周期数据源的值,所以在注销页面后需要将初始值恢复,否则打开别的页面时周期值也会变
		S.create('jobCycleType').loadData(S.CONFIG['jobCycleType'].data);
		
		com.sw.bi.scheduler.job.VirtualMaintainModule.superclass.beforeDestroy.call(this);
	}
});