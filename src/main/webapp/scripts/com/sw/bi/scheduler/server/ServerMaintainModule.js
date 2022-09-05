_package('com.sw.bi.scheduler.server');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.server.ServerMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	initModule: function() {
		com.sw.bi.scheduler.server.ServerMaintainModule.superclass.initModule.call(this);
		
		this.on({
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'server',
			
			items: [{
				xtype: 'hidden', name: 'createdBy'
			}, {
				xtype: 'hidden', name: 'updatedBy'
			}, {
				columnWidth: 1,
				name: 'name',
				fieldLabel: '名称',
				allowBlank: false
			}, {
				columnWidth: 1,
				name: 'ip',
				fieldLabel: 'IP',
				allowBlank: false
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'description',
				fieldLabel: '备注'
			}]
		};
	},
	
	onBeforeSave: function(mdl, data) {
		var server = data['server'];
		if (Ext.isEmpty(server.serverId, false)) {
			server.createdBy = USER_ID;
			server.updatedBy = null;
		} else {
			server.updatedBy = USER_ID;
		}
	}
	
});