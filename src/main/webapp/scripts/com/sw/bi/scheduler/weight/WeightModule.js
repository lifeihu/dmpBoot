_package('com.sw.bi.scheduler.weight');

_import([
	'framework.modules.SearchGridModule',
	'com.sw.bi.scheduler.weight.WeightListModule'
]);

com.sw.bi.scheduler.weight.WeightModule = Ext.extend(framework.modules.SearchGridModule, {

	searcher: function() {
		return {
			items: [{
				columnWidth: .3,
				name: 'jobId',
				fieldLabel: '作业ID',
				vtype: 'mutilnum'
			}]
		};
	},
	
	detailer: function() {
		return {
			xtype: 'weightlistmodule'
		}
	}
	
});