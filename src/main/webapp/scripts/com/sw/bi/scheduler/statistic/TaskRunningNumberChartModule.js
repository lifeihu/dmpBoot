_package('com.sw.bi.scheduler.statistic');

_import([
	'com.sw.bi.scheduler.task.TaskModule',
	'framework.widgets.form.DateTimeField',
	'framework.widgets.form.MultiCombo'
]);

com.sw.bi.scheduler.statistic.TaskRunningNumberChartModule = Ext.extend(com.sw.bi.scheduler.task.TaskModule, {
	autoLoadData: false,
	
	center: function() {
		var startTime = new Date().clearTime();
		// startTime.setYear(2012);
		
		var endTime = startTime.add(Date.HOUR, 23);
		endTime.setMinutes(59);
		endTime.setSeconds(59);
		
		return {
			height: 200,
			
			tbar: [{
				xtype: 'label',
				text: '最近半月'
			}, {
				xtype: 'combo',
				width: 120,
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[new Date(), new Date().format('Y-m-d')],
						[new Date().add(Date.DAY, -1), new Date().add(Date.DAY, -1).format('Y-m-d')],
						[new Date().add(Date.DAY, -2), new Date().add(Date.DAY, -2).format('Y-m-d')],
						[new Date().add(Date.DAY, -3), new Date().add(Date.DAY, -3).format('Y-m-d')],
						[new Date().add(Date.DAY, -4), new Date().add(Date.DAY, -4).format('Y-m-d')],
						[new Date().add(Date.DAY, -5), new Date().add(Date.DAY, -5).format('Y-m-d')],
						[new Date().add(Date.DAY, -6), new Date().add(Date.DAY, -6).format('Y-m-d')],
						[new Date().add(Date.DAY, -1), new Date().add(Date.DAY, -7).format('Y-m-d')],
						[new Date().add(Date.DAY, -2), new Date().add(Date.DAY, -8).format('Y-m-d')],
						[new Date().add(Date.DAY, -3), new Date().add(Date.DAY, -9).format('Y-m-d')],
						[new Date().add(Date.DAY, -4), new Date().add(Date.DAY, -10).format('Y-m-d')],
						[new Date().add(Date.DAY, -5), new Date().add(Date.DAY, -11).format('Y-m-d')],
						[new Date().add(Date.DAY, -6), new Date().add(Date.DAY, -12).format('Y-m-d')],
						[new Date().add(Date.DAY, -1), new Date().add(Date.DAY, -13).format('Y-m-d')],
						[new Date().add(Date.DAY, -2), new Date().add(Date.DAY, -14).format('Y-m-d')]
					]
				}),
				listeners: {
					select: this.onSevenDaysSelect,
					scope: this
				}
			}, {
				xtype: 'label',
				text: '日期：'
			}, {
				xtype: 'datefield',
				width: 120,
				value: startTime
			}, {
				xtype: 'label',
				text: '时间：'
			}, {
				xtype: 'combo',
				width: 50,
				store: S.create('hours'),
				value: startTime.format('H'),
				listeners: {
					select: this.onHourSelect.createDelegate(this, [true], true),
					scope: this
				}
			}, {
				xtype: 'combo',
				width: 50,
				store: S.create('minutes')
			}, {
				xtype: 'label',
				text: '至'
			}, {
				xtype: 'combo',
				width: 50,
				store: S.create('hours'),
				value: endTime.format('H'),
				listeners: {
					select: this.onHourSelect.createDelegate(this, [false], true),
					scope: this
				}
			}, {
				xtype: 'combo',
				width: 50,
				store: S.create('minutes'),
				value: '59'
			}, '-', {
				xtype: 'label',
				text: '间隔：'
			}, {
				xtype: 'combo',
				width: 80,
				allowBlank: false,
				value: 30,
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[1, '一分钟'],
						[5, '五分钟'],
						[10, '十分钟'],
						[20, '二十分钟'],
						[30, '三十分钟'],
						[60, '一小时']
					]
				})
			}, '-', {
				xtype: 'label',
				text: '作业类型：'
			}, {
				xtype: 'multicombo',
				store: S.create('jobType'),
				width: 300
			}, {
				xtype: 'button',
				iconCls: 'search',
				tooltip: '查询',
				handler: this.loadData,
				scope: this
			}, '-', {
				xtype: 'button',
				disabled: true,
				iconCls: 'x-tbar-page-prev',
				handler: this.scrollChart.createDelegate(this, [true]),
				scope: this
			}, {
				xtype: 'button',
				disabled: true,
				iconCls: 'x-tbar-page-next',
				handler: this.scrollChart.createDelegate(this, [false]),
				scope: this
			}],
			
			items: {
				xtype: 'linechart',
				url: framework.getUrl('/resources/ext/charts.swf'),
	            xField: 'time',
	            yField: 'counter',
	            
	            tipRenderer : function(chart, record){
	                return ['日期：', record.get('scanDate'), ' ', record.get('time'), '\n', '此刻正在运行的任务数: ', record.get('counter')].join('');
	            },
	            
	            extraStyle: {
	            	legend : {  
                        display : 'top'
                    },
		            xAxis: {
	                    labelRotation: 60
	                }
	            },
	            
	            series: [{
	                type: 'line',
	                displayName: '此刻正在运行的任务数',
	                yField: 'counter'
	            }],
	            
	            listeners: {
					itemclick: this.onPointClick,
					scope: this
				},
	            
	            store: new Ext.data.JsonStore({
	            	autoLoad: true,
	            	url: 'statistic/statisticTaskRunningNumber4Chart',
	            	fields: ['scanDate', 'time', 'counter', 'jobIds', 'actionIds'],
					baseParams: {
						interval: 30,
						startTime: startTime.format('Y-m-d H:i:00'),
						endTime: endTime.format('Y-m-d H:i:59')
					},
					
					listeners: {
						beforeload: this.onChartBeforeLoad,
						load: this.onChartLoad,
						scope: this
					}
	            })
			}
		};
	},
	
	loadData: function() {
		var center = this.centerPnl,
			tbar = center.getTopToolbar(),
			store = center.getComponent(0).store,
			
			scanDate = tbar.getComponent(3).getValue().format('Y-m-d'),
			shour = tbar.getComponent(5).getValue(),
			sminute = tbar.getComponent(6).getValue(),
			ehour = tbar.getComponent(8).getValue(),
			eminute = tbar.getComponent(9).getValue(),
			interval = tbar.getComponent(12).getValue(),
			jobType = tbar.getComponent(15).getValue();
			
		sminute = Ext.isEmpty(sminute) ? '00' : sminute;
		eminute = Ext.isEmpty(eminute) ? '00' : eminute;
		delete store.baseParams;
		
		var startTime = scanDate + ' ' + shour + ':' + sminute + ':00',
			endTime = scanDate + ' ' + ehour + ':' + eminute + ':59';

		store.load({params: {
			interval: interval,
			startTime: startTime,
			endTime: endTime,
			jobType: jobType
		}});
	},
	
	scrollChart: function(isPrev) {
		var center = this.centerPnl,
			tbar = center.getTopToolbar(),
			
			fldSHour = tbar.getComponent(5),
			fldEHour = tbar.getComponent(8),
			
			fldPrev = tbar.getComponent(18),
			fldNext = tbar.getComponent(19),
			
			shour = parseInt(fldSHour.getValue(), 10),
			ehour = parseInt(fldEHour.getValue(), 10),
			hours = ehour - shour,
			half = hours > 1 ? parseInt(hours / 2, 10) : ehour;
			
		if (isPrev) {
			nshour = Math.max(shour - half, 0),
			nehour = Math.min(nshour + hours, 23);
		} else {
			nshour = Math.min(shour + half, 23),
			nehour = Math.min(nshour + hours, 23);
		}
			
		fldSHour.setValue(String.leftPad(nshour, 2, '0'));
		fldEHour.setValue(String.leftPad(nehour, 2, '0'));
		
		fldPrev.setDisabled(nshour == 0);
		fldNext.setDisabled(nehour == 23);
		
		this.loadData();
	},
	
	onModuleRender: function() {
		com.sw.bi.scheduler.statistic.TaskRunningNumberChartModule.superclass.onModuleRender.call(this);
		
		var multi = this.southPnl.getComponent(0);
		multi.setActiveTab(1);
		
		var fldStartTaskDate = multi.getActiveTab().findField('taskDateStart'),
			fldEndTaskDate = multi.getActiveTab().findField('taskDateEnd');
			
		fldStartTaskDate.setValue(null);
		fldStartTaskDate.setReadOnly(true);
		fldStartTaskDate.allowBlank = true;
		
		fldEndTaskDate.setValue(null);
		fldEndTaskDate.setReadOnly(true);
		fldEndTaskDate.allowBlank = true;
	},
	
	onSevenDaysSelect: function(combo) {
		var date = combo.getValue();
		if (date == null) return;
		
		this.centerPnl.getTopToolbar().getComponent(3).setValue(date);
		
		this.loadData();
	},
	
	onChartBeforeLoad: function() {
		if (!this.chartLoadMask) {
			this.chartLoadMask = new Ext.LoadMask(this.centerPnl.body, {
				msg: '正在加载数据,请耐心等候...'
			});
		}
		
		this.chartLoadMask.show.defer(1, this.chartLoadMask);
	},
	
	onChartLoad: function(store, records) {
		if (this.chartLoadMask) {
			this.chartLoadMask.hide();
		}
	},
	
	onPointClick: function(o) {
		var multi = this.southPnl.getComponent(0);
		multi.setActiveTab(1);
		var	mdlAction = multi.getActiveTab();
		
		if (o.item.counter == 0) {
			mdlAction.centerPnl.store.removeAll();
			return;
		}
		
		/*var center = this.centerPnl,
			tbar = center.getTopToolbar(),
			store = center.getComponent(0).store,
			
			item = o.item,
			nextIndex = o.index + 1,
			startTime = item.scanDate + ' ' + item.time + ':00',
			endTime = null;
		
		if (nextIndex >= store.getCount()) {
			var scanDate = tbar.getComponent(3).getValue().format('Y-m-d'),
				ehour = tbar.getComponent(8).getValue(),
				eminute = tbar.getComponent(9).getValue();
			eminute = Ext.isEmpty(eminute) ? '00' : eminute;
			
			endTime = scanDate + ' ' + ehour + ':' + eminute + ':59';
		} else {
			var record = store.getAt(nextIndex);
			endTime = Date.parseDate(record.get('scanDate') + ' ' + record.get('time') + ':00', 'Y-n-j H:i:s');
			endTime = endTime.add(Date.SECOND, -1).format('Y-m-d H:i:s');
		}*/
		
		this.loadActionData({
			scanDate: o.item.scanDate,
			actionId: o.item.actionIds,
			useScanDate: true
			/*startTime: startTime,
			endTime: endTime*/
		});
	},
	
	onHourSelect: function(combo, record, index, isStartHour) {
		var center = this.centerPnl,
			tbar = center.getTopToolbar(),
			
			fldPrev = tbar.getComponent(18),
			fldNext = tbar.getComponent(19),
			
			hour = parseInt(combo.getValue(), 10);
	
		if (isStartHour) {
			fldPrev.setDisabled(hour == 0);
		} else {
			fldNext.setDisabled(hour == 23);
		}
	}
});