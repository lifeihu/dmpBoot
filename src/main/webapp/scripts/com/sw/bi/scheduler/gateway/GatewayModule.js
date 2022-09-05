_package('com.sw.bi.scheduler.gateway.gateway');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.gateway.GatewayModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'gateway',
	
	maintain: {
		title: '网关机维护',
		width: 600,
		height: 640,
		
		module: {
			module: 'com.sw.bi.scheduler.gateway.GatewayMaintainModule'
		}
	},
	
	searcher: function() {
		return {
			items: [{
				name: 'name',
				fieldLabel: '名称'
			}, {
				name: 'ip',
				fieldLabel: 'IP'
			}]
		};
	},
	
	detailer: function() {
		return {
			tbuttons: [{
				text: '修改调度方式',
				menu: {
					items: [{
						text: '并行方式',
						way: 0
					}, {
						text: '串行方式',
						way: 1
					}],
					
					listeners: {
						itemclick: this.onMenuItemClick,
						scope: this
					}
				}
			}, {
				text: '修改轮循方式',
				menu: {
					items: [{
						text: '参考点',
						way: 0
					}, {
						text: '模拟',
						way: 1
					}],
					
					listeners: {
						itemclick: this.onChangeRoundWay,
						scope: this
					}
				}
			}],
			
			columns: [{
				xtype: 'customcolumn',
				header: '名称',
				dataIndex: 'name',
				width: 160
			}, {
				xtype: 'customcolumn',
				header: 'IP',
				dataIndex: 'ip',
				width: 150,
				renderer: function(newValue, value, meta, record) {
					return newValue + ':' + record.get('port');
				}
			}, {
				xtype: 'customcolumn',
				header: '主网关',
				dataIndex: 'master',
				store: S.create('yesNo'),
				width: 50
			}, {
				xtype: 'customcolumn',
				header: '登录用户',
				dataIndex: 'userName',
				width: 100
			}, {
				xtype: 'customcolumn',
				header: '状态',
				dataIndex: 'status',
				width: 40,
				store: S.create('gatewayStatus')
			}, {
				xtype: 'customcolumn',
				header: '执行尾号',
				dataIndex: 'tailNumber',
				width: 200
			}, {
				xtype: 'customcolumn',
				header: '最大任务数',
				dataIndex: 'taskRunningMax',
				width: 100
			}, {
				xtype: 'customcolumn',
				header: '启用白名单',
				dataIndex: 'useWhiteList',
				width: 75
			}, {
				xtype: 'customcolumn',
				header: '调度方式',
				dataIndex: 'schedulerWay',
				width: 80,
				store: S.create('schedulerWay')
			}, {
				xtype: 'customcolumn',
				header: '轮循方式',
				dataIndex: 'roundWay',
				width: 80,
				store: S.create('roundWays')
			}, {
				xtype: 'customcolumn',
				header: '启用HiveJDBC连接',
				dataIndex: 'hiveJdbc',
				width: 120,
				renderer: function(value) {
					return Ext.isEmpty(value, false) ? '否' : '是';
				}
			}],
			
			store: {
				fields: ['port']
			}
		};
	},
	
	onMenuItemClick: function(item, e) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '确定需要将所有网关机都修改为 "' + (item.way == 0 ? '并行' : '串行') + '方式" 执行?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'gateway/updateSchedulerWay',
				params: {
					schedulerWay: item.way
				},
				
				success: function() {
					mdl.loadData();
				}
			})
		});
	},
	
	onChangeRoundWay: function(item, e) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '确定需要将所有网关机都修改为 "' + (item.way == 0 ? '参考点' : '模拟') + '方式" 轮循?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url: 'gateway/updateRoundWay',
				params: {
					roundWay: item.way
				},
				
				success: function() {
					mdl.loadData();
				}
			})
		});
	}
});