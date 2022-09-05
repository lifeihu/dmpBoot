_package('com.sw.bi.scheduler.statistic');

_import([
	'com.sw.bi.scheduler.task.TaskModule'
]);

com.sw.bi.scheduler.statistic.CycleTypeTaskNumbersStatisticModule = Ext.extend(com.sw.bi.scheduler.task.TaskModule, {
	autoLoadData: false,
	disableTaskDate: true,
	
	center: function() {
		return {
			xtype: 'grid',
			height: 220,
			
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
				handler: this.statistics,
				scope: this
			}],
			
			columns: [{
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				store: S.create('jobCycleType'),
				renderer: function(newValue, value) {
					return Ext.isEmpty(value, false) ? '总计' : newValue;
				}
			}, {
				xtype: 'customcolumn',
				header: '未运行',
				dataIndex: 'notRunning',
				renderer: this.countRenderer,
				handler: this.onCountClick,
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '正在运行',
				dataIndex: 'running',
				renderer: this.countRenderer,
				handler: this.onCountClick,
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '运行成功',
				dataIndex: 'runSuccess',
				renderer: this.countRenderer,
				handler: this.onCountClick,
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '运行失败',
				dataIndex: 'runFailure',
				renderer: this.countRenderer,
				handler: this.onCountClick,
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '小计',
				dataIndex: 'total',
				renderer: this.countRenderer,
				handler: this.onCountClick,
				scope: this
			}],
			
			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'statistic/statisticCycleTypeTaskNumbers',
				fields: ['cycleType', 'runSuccess', 'running', 'notRunning', 'runFailure', 'total'],
				baseParams: {
					taskDate: new Date().format('Y-m-d')
				}
			})
		};
	},
	
	countRenderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
		if (cc) {
			cc.setCellLink(row, col, (value != 0));
		}
		
		return newValue;
	},
	
	statistics: function() {
		var store = this.centerPnl.store;
		delete store.baseParams;
		
		store.load({params: {
			taskDate: Ext.getCmp('statDate').getValue().format('Y-m-d')
		}});
	},
	
	/////////////////////////////////////////////////////////////////////

	onSevenDaysSelect: function(combo) {
		var date = combo.getValue();
		if (date == null) return;
		
		Ext.getCmp('statDate').setValue(date);
		this.statistics();
	},
	
	onCountClick: function(record, cc, grid, row, col, e) {
		var cycleType = record.get('cycleType'),
			taskStatus = null;
			
		if (Ext.isEmpty(cycleType, false)) {
			cycleType = null;
		}
		
		if (col == 1) {
			// 未运行
			taskStatus = 0;
		} else if (col == 2) {
			// 运行中
			taskStatus = 1;
		} else if (col == 3) {
			// 运行成功
			taskStatus = 3;
		} else if (col == 4) {
			// 运行失败
			taskStatus = 2
		}
		
		var statDate = Ext.getCmp('statDate').getValue();
		this.loadTaskData({
			cycleType: cycleType,
			taskStatus: taskStatus,
			taskDateStart: statDate.format('Y-m-d'),
			taskDateEnd: statDate.format('Y-m-d')
		});
	}
});