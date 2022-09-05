_package('com.sw.bi.scheduler.role');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.role.RoleModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'role',
	
	maintain: {
		title: '角色维护',
		width: 300,
		height: 120,
		
		module: {
			module: 'com.sw.bi.scheduler.role.RoleMaintainModule'
		}
	},
	
	initModule: function() {
		com.sw.bi.scheduler.role.RoleModule.superclass.initModule.call(this);
		
		Ext.apply(this.actionUpdateConfig, {
			renderer: function(value, oldValue, options, record, row, col, store, grid, cc) {
				if (record.get('isAdmin') !== false) {
					cc.setCellLink(row, col, false);
				}
		
				return value;
			}
		});		
	},
	
	searcher: function() {
		return {
			items: [{
				name: 'roleName',
				fieldLabel: '名称'
			}]
		};
	},
	
	detailer: function() {
		return {
			actions: [{
				text: '分配菜单',
				
				handler: this.grant,
				scope: this,
				
				renderer: function(value, oldValue, options, record, rowIndex, colIndex, store, grid, cc) {
					if (record.get('isAdmin') === true) {
						cc.setCellLink(rowIndex, colIndex, false);
						value = null;
					}
					
					return value;
				}
			}],
			
			columns: [{
				header: '名称',
				dataIndex: 'roleName',
				width: 200
				
			}],
			
			store: {
				fields: ['isAdmin']
			}
		};
	},
	
	grant: function(record) {
		this.createWindow({
			title: '分配菜单',
			width: 350,
			height: 500,
			
			// relayEvent: ['granted'],
			
			module: 'com.sw.bi.scheduler.permission.PermissionModule'/*,
			
			listeners: {
				granted: function(mdl) {
					mdl.moduleWindow.close();
				}
			}*/
		}).open({
			roleId: record.get('roleId')
		});
	}
});