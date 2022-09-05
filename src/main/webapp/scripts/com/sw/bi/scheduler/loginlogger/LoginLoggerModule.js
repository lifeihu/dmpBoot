_package('com.sw.bi.scheduler.loginlogger');

_import([
	'framework.modules.SearchGridModule',
	'framework.widgets.form.DateTimeField'
]);

com.sw.bi.scheduler.loginlogger.LoginLoggerModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'loginLogger',
	allowDefaultButtons: false,
	actionViewConfig: false,
	actionUpdateConfig: false,
	actionRecoveryConfig: false,
	
	searcher: function() {
		return {
			items: [{
				xtype: 'combo',
				hiddenName: 'userId-eq',
				fieldLabel: '用户',
				store: S.create('users')
			}/*, {
				columnWidth: .15,
				xtype: 'combo',
				hiddenName: 'loginMobile-eq',
				fieldLabel: '用户手机',
				store: S.create('mobileUsers')
			}*/, {
				xtype: 'datetimefield',
				name: 'createTime-ge',
				format: 'Y-n-j 00:00:00',
				fieldLabel: '登录日期'
			}, {
				xtype: 'datefield',
				name: 'createTime-le',
				format: 'Y-n-j 23:59:59',
				fieldLabel: '至'
			}]
		};
	},
	
	detailer: function() {
		return {
			columns: [{
				header: '登录用户',
				dataIndex: 'loginName',
				width: 150
			}/*, {
				xtype: 'customcolumn',
				header: '登录手机',
				dataIndex: 'loginMobile',
				width: 150,
				store: S.create('mobileUsers')
			}*/, {
				header: '姓名',
				dataIndex: 'userName',
				width: 150
			}, {
				xtype: 'customcolumn',
				header: '登录时间',
				dataIndex: 'createTime',
				dateTimeColumn: true
			}, {
				header: '登录IP',
				dataIndex: 'loginIp',
				width: 150
			}],
			
			store: {
				sortInfo: {
					field: 'loginLoggerId',
					direction: 'DESC'
				}
			}
		};
	}
});