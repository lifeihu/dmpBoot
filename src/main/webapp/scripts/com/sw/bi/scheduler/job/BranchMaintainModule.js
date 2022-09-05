_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.BranchMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	jobType: 102,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.BranchMaintainModule.superclass.master.call(this),
			items = master.items;

		// 程序路径
		items[4].columnWidth = .7;
		items[4].fieldLabel = '主从作业';
		items[4].vtype = 'mutilnum';
		
		items.splice(5, 0, {
			xtype: 'label',
			width: 500,
			html: '(注: 各ID以逗号分隔, 第一个为主作业,第二个开始都为从作业. 主作业返回值以前缀为 "<span style="color:blue;font-weight:bold;">master_branch_return_value=</span>"的日志形式输出.)'
		});
		
		return master;
	},
	
	onBeforeSave: function(mdl, data) {
		com.sw.bi.scheduler.job.BranchMaintainModule.superclass.onBeforeSave.apply(this, arguments);
		
		var branchJobs = data['job'].programPath.split(',');
		if (branchJobs.length == 1) {
			mdl.getModuleValidatePlugin().addError({
				field: mdl.findField('programPath'),
				msg: '中只有主作业,必须再指定一个或一个以上的从作业"'
			});
			
			mdl.getModuleValidatePlugin().showErrors();
			
			return false;
		}
	}
});