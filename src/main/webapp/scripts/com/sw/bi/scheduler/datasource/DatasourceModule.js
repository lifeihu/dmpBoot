_package('com.sw.bi.scheduler.datasource');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.datasource.DatasourceModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'datasource',
	
	maintain: {
		title: '数据源维护',
		width: 700,
		height: 430,
		
		module: {
			module: 'com.sw.bi.scheduler.datasource.DatasourceMaintainModule'
		}
	},
	
	initModule: function() {
		this.actionUpdateConfig = {
			iconCls: 'edit',
			tooltip: '修改',
			renderer: function(value, metaData, record, row, col, store, grid, cc) {
				if (record.get('active') === false) {
					cc.setCellLink(row, col, false);
					return;
				}
			},
			handler: 'updateOnly'
		};
		
		com.sw.bi.scheduler.datasource.DatasourceModule.superclass.initModule.call(this, this);	
	},
	
	searcher: function() {
		return {
			items: [{
				xtype: 'hidden', name: 'userId', value: USER_ID
			}, {
				xtype: 'hidden', name: 'userGroupId', value: USER_GROUP_ID
			}, {
				name: 'name',
				fieldLabel: '名称'
			}, {
				xtype: 'combo',
				hiddenName: 'type',
				fieldLabel: '类型',
				store: S.create('datasourceType')
			}]
		};
	},
	
	detailer: function() {
		return {
			removeUrl: 'datasource/logicRemove',
			
			actions: [{
				xtype: 'customcolumn',
				iconCls: 'reset-password',
				tooltip: '修改数据源密码',
				renderer: function(newValue, value, metaData, record, row, col, store, grid, cc) {
					if (record.get('active') === false) {
						cc.setCellLink(row, col, false);
						return;
					}
					
					return newValue;
				},
				handler: this.changePassword,
				scope: this
			}, {
				xtype: 'customcolumn',
				iconCls: 'children-job',
				tooltip: '查看依赖当前数据源的所有作业',
				handler: this.viewJobs,
				scope: this
			}, {
				xtype: 'customcolumn',
				iconCls: 'recovery',
				tooltip: '恢复数据源',
				renderer: function(newValue, value, metaData, record, row, col, store, grid, cc) {
					if (record.get('active') !== false) {
						cc.setCellLink(row, col, false);
						return;
					}
					
					return newValue;
				},
				handler: this.recovery,
				scope: this
			}],
			
			columns: [{
				hidden: true,
				dataIndex: 'active'
			}, {
				xtype: 'customcolumn',
				header: '名称',
				dataIndex: 'name',
				width: 300
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'type',
				width: 80,
				store: S.create('datasourceType')
			}, {
				xtype: 'customcolumn',
				header: '服务器',
				dataIndex: 'ip',
				width: 150
			}, {
				xtype: 'customcolumn',
				header: '端口',
				dataIndex: 'port',
				width: 50
			}, {
				xtype: 'customcolumn',
				header: '数据库名',
				dataIndex: 'databaseName',
				width: 130
			}, {
				xtype: 'customcolumn',
				header: '字符集',
				dataIndex: 'charset',
				width: 50
			}, {
				xtype: 'customcolumn',
				header: '用户名',
				dataIndex: 'username',
				width: 100
			}, {
				xtype: 'customcolumn',
				header: '创建人',
				dataIndex: 'createBy',
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '用户组',
				dataIndex: 'userGroup.name'
			}/*, {
				header: '连接串',
				dataIndex: 'connectionString',
				width: 300,
				tooltip: true
			}*/]
		};
	},
	
	viewJobs: function(record) {
		this.createWindow({
			title: '数据源[' + record.get('name') + ']相关作业', 
			
			module: {
				module: 'com.sw.bi.scheduler.job.JobByDataSourceModule',
				dataSourceId: record.get('datasourceId')
			}
		}, 'framework.widgets.window.ModuleWindow').open();
	},
	
	changePassword: function(record) {
		framework.createWindow({
			title: '修改数据源密码',
			
			width: 300,
			height: 120,
			minHeight: 120,
			
			module: {
				module: 'com.sw.bi.scheduler.datasource.DatasourcePasswordMaintainModule',
				datasourceId: record.get('datasourceId'),
				datasourceName: record.get('name'),
				userName: record.get('username')
			}
		}, 'framework.widgets.window.MaintainWindow').create();
	},
	
	recovery: function(record) {
		var mdl = this;
		
		Ext.Ajax.request({
			url: 'datasource/recovery',
			params: {
				id: record.get('datasourceId')
			},
			
			success: function() {
				mdl.clearStoreData(['datasource']);
				mdl.loadData();
			}
		});
	},
	
	////////////////////////////////////////////////////////////////////////////////
	
	onModuleRender: function(mdl) {
		com.sw.bi.scheduler.datasource.DatasourceModule.superclass.onModuleRender.call(this, mdl);
		
		if (USER_IS_ADMINISTRTOR) {
			mdl.BUTTON_REMOVE.setText('禁用');
			mdl.BUTTON_REMOVE.show();
		} else {
			mdl.BUTTON_REMOVE.hide();
		}
	}
});