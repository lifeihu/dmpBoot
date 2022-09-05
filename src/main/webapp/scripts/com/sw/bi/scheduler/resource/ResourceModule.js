_package('com.sw.bi.scheduler.resource');

_import([
	'framework.widgets.container.ModuleContainer',
	'com.sw.bi.scheduler.resource.ResourceListModule',
	
	'com.sw.bi.scheduler.resource.ResourceTreePanel'
]);

com.sw.bi.scheduler.resource.ResourceModule = Ext.extend(framework.core.Module, {
	
	west: function() {
		return {
			xtype: 'resourcetree',
			id: 'treeResource',
			width: 350,
			
			listeners: {
				click: this.onNodeClick,
				scope: this
			}
		};
	},
	
	center: function() {
		return {
			xtype: 'resourcelistmodule'
		};
	},
	
	onNodeClick: function(node) {
		var mdl = this.centerPnl;
		mdl.setCondition({'resourceId': node.attributes.resourceId, 'userId': USER_ID});
		mdl.loadData();
	}
});