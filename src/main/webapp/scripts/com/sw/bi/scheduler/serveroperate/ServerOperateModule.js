_package('com.sw.bi.scheduler.serveroperate');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.serveroperate.ServerOperateModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'serverOperate',
	pageSize: 30,
	
	maintain: {
		title: '操作维护',
		width: 500,
		height: 300,
		
		module: {
			module: 'com.sw.bi.scheduler.serveroperate.ServerOperateMaintainModule'
		}
	},
	
	searcher: function() {
		return {
			items: [{
				columnWidth: .15,
				name: 'name',
				fieldLabel: '名称'
			}, {
				columnWidth: .15,
				name: 'businessGroup',
				fieldLabel: '业务组'
			}, {
				xtype: 'combo',
				name: 'serverName',
				fieldLabel: '服务器',
				store: S.create('servers')
			}, {
				columnWidth: .1,
				xtype: 'combo',
				hiddenName: 'serverUserId-eq',
				fieldLabel: '帐号',
				store: S.create('serverUsers')
			}, {
				xtype: 'combo',
				hiddenName: 'serverShellId-eq',
				fieldLabel: '脚本',
				store: S.create('serverShells')
			}]
		};
	},
	
	detailer: function() {
		return {			
			actions: [{
				width: 20,
				handler: function(record) {
					this[record.get('status') == SERVER_OPERATE_STATUS.ONLINE ? 'offline' : 'online'](record);
				},
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					var	status, action = '上线',
						operateStatus = record.get('status');
						
					if (operateStatus == SERVER_OPERATE_STATUS.UNLINE) {
						meta.css += ' unline';
						
						status = '未上线';
					} else if (operateStatus == SERVER_OPERATE_STATUS.ONLINE) {
						meta.css += ' online';
						
						status = '已上线';
						action = '下线';
					} else if (operateStatus == SERVER_OPERATE_STATUS.OFFLINE) {
						meta.css += ' offline';
						
						status = '已下线';
					}
					
					meta.attr += ' ext:qtip="状态: ' + status + '<br>点击后可将操作<b>' + action + '</b>"';
					
					return '';
				},
				scope: this
			}, {
				width: 20,
				iconCls: 'execute',
				tooltip: '执行操作',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('status') != SERVER_OPERATE_STATUS.ONLINE) {
						cc.setCellLink(row, col, false);
						return null;
					}
					
					return '';
				},
				handler: this.execute,
				scope: this
			}],
			
			columns: [{
				header: '名称',
				dataIndex: 'name',
				width: 300
			}, {
				xtype: 'customcolumn',
				header: '脚本',
				dataIndex: 'serverShellId',
				width: 150,
				store: S.create('serverShells')
			}, {
				xtype: 'customcolumn',
				header: '帐号',
				dataIndex: 'serverUserId',
				store: S.create('serverUsers')
			}, {
				header: '服务器',
				dataIndex: 'serverName',
				width: 300,
				tooltip: true
			}, {
				header: '业务组',
				dataIndex: 'businessGroup'
			}, {
				xtype: 'customcolumn',
				header: '显示结果',
				dataIndex: 'showResult',
				width: 60
			}/*, {
				header: '备注',
				dataIndex: 'description',
				width: 300
			}*/],
			
			store: {
				fields: ['status', 'showResult']
			}
		};
	},
	
	online: function(record) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '是否将 "' + record.get('name') + '" 操作上线?', function(btn) {
			if (btn !== 'yes') return;
			
			Ext.Ajax.request({
				url: 'serverOperate/online',
				params: {
					serverOperateId: record.get('serverOperateId')
				},
				
				success: function() {
					mdl.loadData();
				}
			})
		});
	},
	
	offline: function(record) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '是否将 "' + record.get('name') + '" 操作下线?', function(btn) {
			if (btn !== 'yes') return;
			
			Ext.Ajax.request({
				url: 'serverOperate/offline',
				params: {
					serverOperateId: record.get('serverOperateId')
				},
				
				success: function() {
					mdl.loadData();
				}
			})
		});
	},
	
	execute: function(record) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '是否执行 "' + record.get('name') + '" 操作?', function(btn) {
			if (btn !== 'yes') return;
			
			Ext.Ajax.request({
				timeout: 1000 * 60 * 20,
				url: 'serverOperate/execute',
				params: {
					serverOperateId: record.get('serverOperateId')
				},
				waitMsg: '正在执行服务器操作,请耐心等候...',
				
				success: function(response) {
					var result = Ext.decode(response.responseText);
					
					if (record.get('showResult')) {
						framework.createWindow({
							title: '"' + record.get('name') + '" 执行结果',
							
							module: {
								module: 'com.sw.bi.scheduler.serveroperate.ServerOperateResultModule',
								executeResult: result
							}
						}, 'framework.widgets.window.ModuleWindow').open();
						
					} else {
						var message = '';
						Ext.iterate(result, function(serverIp) {
							message += result[serverIp].replace(/\n/g, '<br>');
						});
						
						if (Ext.isEmpty(message, false)) {
							Ext.Msg.alert('提示', '成功执行操作.');
						} else {
							Ext.Msg.alert('提示', message);
						}
					}
				}
			});
		});
	}
	
});