_package('com.sw.bi.scheduler.task');

_import([
	'com.sw.bi.scheduler.task.TaskModule'
]);

com.sw.bi.scheduler.task.HourTaskModule = Ext.extend(com.sw.bi.scheduler.task.TaskModule, {
	autoLoadData: false,
	
	/**
	 * @property task
	 * @type 
	 */
	task: null,
	
	center: function() {
		return {
			xtype: 'panel',
			border: false,
			height: 0
		};
	},
	
	south: function() {
		var mdl = this;
		return {
			height: 450,
			minHeight: 290,
			maxHeight: 450,
			split: true,
			collapseMode: 'mini',
			
			items: {
				xtype: 'multimodule',
				
				moduler: function() {
					return [{
						xtype: 'tasklistmodule',
						pageSize: 30,
						title: '任务',
						module: mdl,
						
						onModuleRender: mdl.onTaskListModuleRender.createDelegate(mdl),
						scope: mdl
					}, {
						xtype: 'actionmodule',
						title: '任务明细'
					}];
				},
				
				listeners: {
					tabchange: mdl.onTaskChange,
					scope: mdl
				}
			}
		};
	},
	
	onModuleRender: function(mdl) {
		com.sw.bi.scheduler.task.HourTaskModule.superclass.onModuleRender.call(this, mdl);
		
		this.southPnl.getComponent(0).getActiveTab().centerPnl.store.baseParams.sort = 'settingTime';
		mdl.loadTaskData({
			'jobId': this.task.jobId,
			'taskDateStart': this.task.taskDate,
			'taskDateEnd': this.task.taskDate
		});
	}
	
});