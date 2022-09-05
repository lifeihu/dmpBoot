_package('com.sw.bi.scheduler.serveruser');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.serveruser.ServerUserModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'serverUser',
	
	maintain: {
		title: '帐号维护',
		width: 400,
		height: 220,
		
		module: {
			module: 'com.sw.bi.scheduler.serveruser.ServerUserMaintainModule'
		}
	},
	
	searcher: function() {
		return {
			items: [{
				name: 'username',
				fieldLabel: '用户'
			}]
		};
	},
	
	detailer: function() {
		return {
			actions: [{
				iconCls: 'reset-password',
				tooltip: '重置密码',
				width: 40,
				handler: this.changePassword,
				scope: this
			}],
			
			columns: [{
				header: '用户',
				dataIndex: 'username',
				width: 250
			}, {
				header: '显示名',
				dataIndex: 'description',
				width: 300
			}]
		};
	},
	
	changePassword: function(record) {
		framework.createWindow({
			title: '修改 [' + record.get('username') + '] 密码',
			
			width: 300,
			height: 120,
			minHeight: 120,
			
			module: {
				module: 'com.sw.bi.scheduler.serveruser.ServerUserPasswordMaintainModule',
				serverUserId: record.get('serverUserId')
			}
		}, 'framework.widgets.window.MaintainWindow').create();
	}
	
});