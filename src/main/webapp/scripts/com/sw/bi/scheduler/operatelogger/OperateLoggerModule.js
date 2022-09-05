_package('com.sw.bi.scheduler.operatelogger');

_import([
	'framework.modules.SearchGridModule',
	'framework.widgets.form.DateTimeField'
]);

com.sw.bi.scheduler.operatelogger.OperateLoggerModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'operateLogger',
	allowDefaultButtons: false,
	actionViewConfig: false,
	actionUpdateConfig: false,
	actionRecoveryConfig: false,
	
	searcher: function() {
		return {
			items: [{
				columnWidth: .15,
				xtype: 'combo',
				hiddenName: 'userId-eq',
				fieldLabel: '用户',
				store: S.create('users')
			}/*, {
				columnWidth: .15,
				xtype: 'combo',
				hiddenName: 'operateMobile-eq',
				fieldLabel: '用户手机',
				store: S.create('mobileUsers')
			}*/, {
				columnWidth: .12,
				xtype: 'combo',
				name: 'operateAction-eq',
				fieldLabel: '动作',
				store: S.create('operateActions')
			}, {
				xtype: 'datetimefield',
				name: 'createTime-ge',
				format: 'Y-n-j 00:00:00',
				fieldLabel: '操作日期'
			}, {
				columnWidth: .15,
				xtype: 'datetimefield',
				name: 'createTime-le',
				format: 'Y-n-j 23:59:59',
				fieldLabel: '至'
			}]
		};
	},
	
	detailer: function() {
		return {
			columns: [{
				header: '操作用户',
				dataIndex: 'userName',
				width: 150
			}/*, {
				xtype: 'customcolumn',
				header: '操作手机',
				dataIndex: 'operateMobile',
				width: 150,
				store: S.create('mobileUsers')
			}*/, {
				xtype: 'customcolumn',
				header: '操作时间',
				dataIndex: 'createTime',
				dateTimeColumn: true
			}, {
				header: '操作IP',
				dataIndex: 'operateIp',
				width: 150
			}, {
				xtype: 'customcolumn',
				header: '操作动作',
				dataIndex: 'operateAction',
				width: 100/*,
				store: S.create('operateActions')*/
			}, {
				header: '操作内容',
				dataIndex: 'operateContent',
				width: 700,
				align: 'left',
				tooltip: true
			}],
			
			store: {
				sortInfo: {
					field: 'operateLoggerId',
					direction: 'desc'
				}
			}
		};
	}
});