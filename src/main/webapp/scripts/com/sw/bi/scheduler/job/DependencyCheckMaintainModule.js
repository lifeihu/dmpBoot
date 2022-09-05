_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.DependencyCheckMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	jobType: 92,
	
	master: function() {
		var master = com.sw.bi.scheduler.job.DependencyCheckMaintainModule.superclass.master.call(this),
			items = master.items;
			
		var jobType = items[1];
		jobType.readOnly = false;
		jobType.value = 92;
		jobType.store = S.create('jobTypeForDependency');
		jobType.listeners = {
			select: this.onJobTypeSelect,
			scope: this
		};

		// 程序路径
		items[4].xtype = 'hidden';
		items[4].allowBlank = true;
		
		// 作业周期
		items[5].value = 3;
		items[5].readOnly = true;
		
		// 说明标签
		items[10].hidden = true;
		
		// 父作业
		items[18].condition = {'cycleType-eq': JOB_CYCLE_TYPE.HOUR};
		
		return master;
	},
	
	onJobTypeSelect: function(combo) {
		var mdl = this,
			jobType = combo.getValue(),
			
			fldCycle = mdl.findField('cycleType'),
			fldChooser = Ext.getCmp('pnlJob');
		
		if (jobType == 92) {
			fldCycle.setValue(JOB_CYCLE_TYPE.DAY);
			fldChooser.condition = {'cycleType-eq': JOB_CYCLE_TYPE.HOUR};
		} else {
			fldCycle.setValue(JOB_CYCLE_TYPE.MONTH);
			fldChooser.condition = {'cycleType-eq': JOB_CYCLE_TYPE.DAY};
		}
		
		mdl.onJobCycleSelect(fldCycle);
		
		var fldGateway = mdl.findField('gateway'),
			executeGateway = fldGateway.getValue();
			
		fldGateway.setValue(null);
		
		// 如果执行网关机之前有值，则在根据类型加载完后需要将值设回去
		if (!Ext.isEmpty(executeGateway, false)) {
			fldGateway.store.on({
				load: function() {
					fldGateway.setValue(executeGateway);
				}
			});
		}
		
		fldGateway.store.load({params: {
			jobType: jobType
		}});
	},
	
	onLoadDataComplete: function(mdl, data) {
		com.sw.bi.scheduler.job.DependencyCheckMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
		mdl.onJobTypeSelect(mdl.findField('jobType'));
	}
});