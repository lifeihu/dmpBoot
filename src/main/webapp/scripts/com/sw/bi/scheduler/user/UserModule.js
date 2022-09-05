_package('com.sw.bi.scheduler.user');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.user.UserModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'user',
	
	buttonRemoveDisabled: true,
	
	maintain: {
		title: '用户维护',
		width: 550,
		height: 600,
		
		module: 'com.sw.bi.scheduler.user.UserMaintainModule'
	},
	
	initModule: function() {
		com.sw.bi.scheduler.user.UserModule.superclass.initModule.call(this);
		
		Ext.apply(this.actionUpdateConfig, {
			renderer: function(value, oldValue, options, record, row, col, store, grid, cc) {
				if (record.get('isAdmin') !== false || record.get('status') == 1) {
					cc.setCellLink(row, col, false);
				}
		
				return value;
			}
		});
	},
	
	searcher: function() {
		return {
			items: [{
				name: 'userName',
				fieldLabel: '登录名',
				labelWidth: 40
			}, {
				name: 'realName',
				fieldLabel: '姓名',
				labelWidth: 40
			}]
		};
	},
	
	detailer: function() {
		return [{
			checkboxable: false,
			
			actions: [{
				iconCls: 'remove',
				tooltip: '删除用户',
				width: 40,
				renderer: function(value, oldValue, options, record, row, col, store, grid, cc) {
					if (record.get('isAdmin') || record.get('status') == 1) {
						cc.setCellLink(row, col, false);
					}
					
					return value;
				},
				handler: this.removed,
				scope: this
			}, {
				iconCls: 'reset-password',
				tooltip: '重置密码',
				width: 40,
				renderer: function(value, oldValue, options, record, row, col, store, grid, cc) {
					if (record.get('isAdmin') || record.get('status') == 1) {
						cc.setCellLink(row, col, false);
					}
					
					return value;
				},
				handler: this.resetPassword,
				scope: this
			}],
			
			columns: [{
				header: '登录名',
				dataIndex: 'userName',
				width: 100,
				renderer: function(value, options, record) {
					if (record.get('status') == 1) {
						options.style += 'color:red;text-decoration:line-through;';
					}
					
					return value;
				}
			}, {
				header: '姓名',
				dataIndex: 'realName',
				width: 100
			}, {
				header: '电子邮件',
				dataIndex: 'email',
				width: 150
			}, {
				header: '手机',
				dataIndex: 'mobilePhone',
				width: 80
			}, {
				xtype: 'customcolumn',
				header: '用户类型',
				dataIndex: 'userType',
				store: S.create('userType')
			}, {
				header: '所属角色',
				dataIndex: 'roleNames',
				width: 200
			}, {
				header: '用户组',
				dataIndex: 'userGroup.name'
			}, {
				xtype: 'customcolumn',
				header: '状态',
				dataIndex: 'status',
				width: 60,
				store: S.create('userStatus')
			}],
			
			store: {
				fields: ['isAdmin', 'userType', 'status'],
				remoteSort: true,
				sortInfo: {
					field: 'status',
					direction: 'ASC'
				}
			}/*,
			
			listeners: {
				rowclick: function(grid, row, e) {
					var record = grid.getSelectionModel().getSelected();
					this.BUTTON_REMOVE.setDisabled(record.get('isAdmin'));
				},
				scope: this
			}*/
		}];
	},
	
	/**
	 * 重置密码
	 */
	resetPassword: function(record) {
		framework.createWindow({
			title: '修改登录密码',
			
			width: 300,
			height: 120,
			minHeight: 120,
			
			module: {
				module: 'com.sw.bi.scheduler.user.UserPasswordMaintainModule',
				userId: record.get('userId'),
				userName: record.get('userName')
			}
		}, 'framework.widgets.window.MaintainWindow').create();
	}
});