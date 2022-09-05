_package('com.sw.bi.scheduler.statistic');

_import([
	'com.sw.bi.scheduler.task.TaskModule'
]);

com.sw.bi.scheduler.statistic.TaskRunTimeModule = Ext.extend(com.sw.bi.scheduler.task.TaskModule, {
	autoLoadData: false,
	disableTaskDate: true,
	
	/**
	 * 运行时行范围(用于传往后台)
	 * @type String
	 */
	runTimeRange: null,
	
	initModule: function() {
		com.sw.bi.scheduler.statistic.TaskRunTimeModule.superclass.initModule.call(this);
		
		var runTimeRange = [];
		S.create('runTimeRanges').each(function(record) {
			runTimeRange.push(record.get('value'));
		});
		this.runTimeRange = runTimeRange.join(';');
	},
	
	center: function() {
		return {
			xtype: 'grid',
			height: 200,
			
			tbar: [{
				xtype: 'label',
				text: '最近七天'
			}, {
				xtype: 'combo',
				store: S.create('sevenDays'),
				listeners: {
					select: this.onSevenDaysSelect,
					scope: this
				}
			}, {
				xtype: 'label',
				text: '日期: '
			}, {
				id: 'statDate',
				xtype: 'datefield',
				value: new Date()
			}, {
				xtype: 'button',
				iconCls: 'search',
				tooltip: '查询',
				handler: this.loadData,
				scope: this
			}],
			
			columns: [{
				xtype: 'customcolumn',
				header: '运行时长',
				dataIndex: 'runTimeRange',
				width: 200,
				store: S.create('runTimeRanges')
			}, {
				xtype: 'customcolumn',
				header: '任务数量',
				dataIndex: 'taskCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (value == 0) {
						cc.setCellLink(row, col, false);
					}
					
					return newValue;
				},
				handler: this.onTaskCountClick,
				scope: this
			}, {
				header: '总耗时(分钟)',
				dataIndex: 'sumRunTime'
			}],
			
			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'statistic/statisticTaskRunTimes',
				fields: ['runTimeRange', 'taskCount', 'sumRunTime'],
				baseParams: {
					'runTimeRange': this.runTimeRange,
					'taskDate': new Date().format('Y-m-d')
				}
			})
		};
	},
	
	loadData: function() {
		var store = this.centerPnl.store;
		delete store.baseParams;

		store.load({params: {
			'runTimeRange': this.runTimeRange,
			'taskDate': Ext.getCmp('statDate').getValue().format('Y-m-d')
		}});
	},
	
	onSevenDaysSelect: function(combo) {
		var date = combo.getValue();
		if (date == null) return;
		
		Ext.getCmp('statDate').setValue(date);
		
		this.loadData();
	},
	
	onTaskCountClick: function(record) {
		var taskDate = Ext.getCmp('statDate').getValue(),
			runTimeRanges = record.get('runTimeRange').split(',');

		this.loadTaskData({
			runTimeStart: runTimeRanges[0] == 0 ? null : runTimeRanges[0],
			runTimeEnd: runTimeRanges[1] == 0 ? null : runTimeRanges[1],
			taskDateStart: taskDate,
			taskDateEnd: taskDate
		});
	}
});