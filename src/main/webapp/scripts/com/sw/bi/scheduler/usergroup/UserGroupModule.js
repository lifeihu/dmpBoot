_package('com.sw.bi.scheduler.usergroup');

_import([
	'com.sw.bi.scheduler.usergroup.UserGroupTreePanel'
]);

com.sw.bi.scheduler.usergroup.UserGroupModule = Ext.extend(framework.core.Module, {
	readOnly: false,
	
	center: function() {
		return new com.sw.bi.scheduler.usergroup.UserGroupTreePanel({
			allowMaintain: true,
			isOnlyActive: false
		});
	}
	
});