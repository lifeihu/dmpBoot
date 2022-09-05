_package('com.sw.bi.scheduler.role');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.role.RoleMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	master: function() {
		return {
			model: 'role',
			
			items: [{
				xtype: 'hidden', name: 'isAdmin'
			}, {
				columnWidth: 1,
				name: 'roleName',
				fieldLabel: '角色名称',
				allowBlank: false
			}]
		};
	}
	
});