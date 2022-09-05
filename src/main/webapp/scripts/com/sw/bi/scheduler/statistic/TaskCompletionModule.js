_package('com.sw.bi.scheduler.statistic');

_import([
	'com.sw.bi.scheduler.task.TaskModule'
]);

com.sw.bi.scheduler.statistic.TaskCompletionModule = Ext.extend(com.sw.bi.scheduler.task.TaskModule, {
	autoLoadData: false,
	disableTaskDate: true,
	
	center: function() {
		var taskDateStart = new Date().add(Date.DAY, -7),
			taskDateEnd = new Date().add(Date.DAY, -1);
		
		return {
			xtype: 'grid',
			height: 220,
			
			tbar: [{
				xtype: 'label',
				text: '快捷日期: '
			}, {
				xtype: 'combo',
				allowBlank: false,
				value: 6,
				store: S.create('quickDateRanges'),
				/*store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[0, '昨天'],
						[6, '最近七天'],
						[9, '最近十天'],
						[14, '最近十五天'],
						[29, '最近三十天'],
						[182, '最近半年'],
						[364, '最近一年']
					]
				}),*/
				listeners: {
					select: this.onQuickDatesSelect,
					scope: this
				}
			}, {
				xtype: 'label',
				text: '日期: '
			}, {
				id: 'statDateStart',
				xtype: 'datefield',
				value: taskDateStart
			}, {
				xtype: 'label',
				text: '至'
			}, {
				id: 'statDateEnd',
				xtype: 'datefield',
				value: taskDateEnd
			}, {
				xtype: 'button',
				iconCls: 'search',
				tooltip: '查询',
				handler: this.loadData,
				scope: this
			}],
			
			columns: [new Ext.grid.RowNumberer(), {
				header: '任务日期',
				dataIndex: 'taskDate'
			}, {
				xtype: 'customcolumn',
				header: '完成情况',
				dataIndex: 'uncomplete',
				width: 200,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					cc.setCellLink(row, col, value != 0);
					
					return value == 0 ? '全部任务已经完成' : '还有 ' + value + ' 个任务未完成';
				},
				handler: this.onUncompleteClick,
				scope: this
			}, {
				xtype: 'customcolumn',
				dateTimeColumn: true,
				header: '任务最后完成时间',
				dataIndex: 'lastCompleteTime'
			}],
			
			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'statistic/statisticTaskCompletions',
				fields: ['taskDate', 'uncomplete', 'lastCompleteTime'],
				baseParams: {
					'startTaskDate': taskDateStart.format('Y-m-d'),
					'endTaskDate': taskDateEnd.format('Y-m-d')
				}
			})
		};
	},
	
	loadData: function() {
		var store = this.centerPnl.store;
		delete store.baseParams;

		store.load({params: {
			startTaskDate: Ext.getCmp('statDateStart').getValue().format('Y-m-d'),
			endTaskDate: Ext.getCmp('statDateEnd').getValue().format('Y-m-d')
		}});
	},
	
	onQuickDatesSelect: function(combo) {
		var days = combo.getValue(),
			endDate = new Date().add(Date.DAY, -1),
			startDate = endDate.add(Date.DAY, days * -1);
		
		Ext.getCmp('statDateStart').setValue(startDate);
		Ext.getCmp('statDateEnd').setValue(endDate);
		
		this.loadData();
	},
	
	onUncompleteClick: function(record) {
		this.loadTaskData({
			taskStatus: '0,1,2',
			taskDateStart: record.get('taskDate'),
			taskDateEnd: record.get('taskDate')
		});
	}
});