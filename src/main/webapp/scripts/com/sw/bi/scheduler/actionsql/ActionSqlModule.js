_package('com.sw.bi.scheduler.actionsql');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.actionsql.ActionSqlModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'actionSql',
	pageSize: 50,

	actionUpdateConfig: false,
	
	maintain: {
		title: '执行SQL查看',
		width: 700,
		height: 430,
		resizable: false,
		
		module: {
			module: 'com.sw.bi.scheduler.actionsql.ActionSqlMaintainModule'
		}
	},

	searcher: function() {
		return {
			items: [{
				xtype: 'datefield',
				name: 'taskDate-eq',
				fieldLabel: '任务日期',
				allowBlank: false,
				value: new Date().add(Date.DAY, -1)
			}, {
				xtype: 'combo',
				name: 'dutyMan-eq',
				fieldLabel: '责任人',
				store: S.create('users')
			}]
		};
	},
	
	detailer: function() {
		return {
			allowDefaultButtons: false,
			
			columns: [/*{
				header: '编号',
				dataIndex: 'actionSqlId',
				width: 80
			}, */{
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}, {
				header: '脚本名',
				dataIndex: 'hiveSqlPath',
				tooltip: true,
				align: 'left',
				width: 360
			}, {
				header: 'SQL字符串',
				dataIndex: 'sqlString',
				tooltip: true,
				width: 350,
				renderer: function(v) {
					return v.replace(/\n/g, '');
				}
			}, {
				xtype: 'customcolumn',
				header: '开始时间',
				dataIndex: 'beginTime',
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '结束时间',
				dataIndex: 'endTime',
				dateTimeColumn: true
			}, {
				header: '时长(分钟)',
				dataIndex: 'runTime',
				width: 120,
				renderer: function(value) {
					if (value > 0) {
						value = parseInt(value / 60, 10);
					}
					
					return value;
				}
			}, {
				header: '责任人',
				dataIndex: 'dutyMan',
				width: 80
			}],
			
			store: {
				baseParams: {limit: 50},
				sortInfo: {
					field: 'runTime',
    				direction: 'DESC'
				}
			}
		};
	}
});