_package('com.sw.bi.scheduler.alertsystemconfig');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.alertsystemconfig.AlertSystemConfigModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'alertSystemConfig',
	actionViewConfig: false,
	actionUpdateConfig: false,
	
	maintain: {
		title: '监控告警系统配置',
		module: 'com.sw.bi.scheduler.alertsystemconfig.AlertSystemConfigMaintainModule'
	},
	
	detailer: function() {
		return {
			columns: [{
				header: '用户组ID',
				dataIndex: 'userGroupId',
				width: 70
			}, {
				xtype: 'customcolumn',
				header: '用户组',
				dataIndex: 'userGroupId',
				store: S.create('userGroups')
			}, {
				xtype: 'customcolumn',
				iconCls: 'edit',
				tooltip: '修改',
				handler: 'updateOnly',
				scope: this,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) { 
					return null;
				}
			}, {
				xtype: 'customcolumn',
				iconCls: 'copy',
				tooltip: '复制',
				handler: this.copy,
				scope: this,
				renderer: function() { return null; }
			}, {
				header: '开始工作时间',
				dataIndex: 'beginWorkTime'
			}, {
				header: '结束工作时间',
				dataIndex: 'endWorkTime'
			}, {
				xtype: 'customcolumn',
				header: '值周人',
				dataIndex: 'dutyMan',
				store: S.create('users')
			}/*, {
				header: '观察人',
				dataIndex: 'observeMan',
				renderer: function(value) {
					var storeUser = S.create('users'),
						observeManIds = value.split(','),
						observeManNames = [];

					Ext.iterate(observeManIds, function(observeManId) {
						var user = storeUser.queryUnique('userId', observeManId);
						if (user != null) {
							observeManNames.push(user.get('realName'));
						}
					});
					
					return value;
				}
			}*/, {
				header: '告警扫描周期(分钟)',
				dataIndex: 'scanCycle',
				width: 180
			}, {
				header: '告警的作业范围',
				dataIndex: 'alertJobRange',
				width: 150
			}, {
				xtype: 'customcolumn',
				header: '告警方式',
				dataIndex: 'alertWay',
				tooltip: true,
				store: S.create('alertWays'),
				width: 200
			}, {
				xtype: 'customcolumn',
				header: '短信告警目标',
				dataIndex: 'alertTarget',
				tooltip: true,
				store: S.create('alertTargets'),
				width: 150
			}, {
				xtype: 'customcolumn',
				header: '短信内容组成方式',
				dataIndex: 'smsContent',
				tooltip: true,
				store: S.create('smsContentWays'),
				width: 200
			}, {
				header: '预算日期',
				dataIndex: 'precomputeForWhichdate',
				dateColumn: true
			}, {
				header: '邮件告警邮箱',
				dataIndex: 'alertMaillist',
				width: 300
			}]
		};
	},
	
	/**
	 * 复制作业
	 */
	copy: function(record) {
		this.updateOnly({
			alertSystemConfigId: record.get('alertSystemConfigId'),
			isCopy: true
		});
	}
});