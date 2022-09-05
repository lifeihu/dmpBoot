_package('com.sw.bi.scheduler.statistic');

com.sw.bi.scheduler.statistic.TaskRunTimeChartModule = Ext.extend(com.sw.bi.scheduler.task.TaskModule, {
	autoLoadData: false,
	
	startTaskDate: null,
	endTaskDate: null,
	settingTime: null,
	jobId: null,

	center: function() {
		return {
			height: 300,
			
			tbar: [{
				xtype: 'label',
				text: '快捷日期: '
			}, {
				xtype: 'combo',
				value: this.startTaskDate != null ? null : 6,
				store: S.create('quickDateRanges'),
				listeners: {
					select: this.onQuickDatesSelect,
					scope: this
				}
			}, {
				xtype: 'label',
				text: '任务日期：'
			}, {
				id: 'statDateStart',
				xtype: 'datefield',
				width: 150,
				allowBlank: false,
				value: this.startTaskDate == null ? new Date().add(Date.DAY, -7) : this.startTaskDate
			}, {
				xtype: 'label',
				text: '至'
			}, {
				id: 'statDateEnd',
				xtype: 'datefield',
				width: 150,
				allowBlank: false,
				value: this.endTaskDate == null ? new Date().add(Date.DAY, -1) : this.endTaskDate
			}, '-', {
				xtype: 'label',
				text: '时间：'
			}, {
				xtype: 'combo',
				width: 50,
				store: S.create('hours'),
				value: this.settingTime == null ? null : this.settingTime.format('H')
			}, {
				xtype: 'combo',
				width: 50,
				store: S.create('minutes'),
				value: this.settingTime == null ? null : this.settingTime.format('i')
			}, '-', {
				xtype: 'label',
				text: '作业ID：'
			}, {
				xtype: 'textfield',
				vtype: 'mutilnum',
				width: 200,
				allowBlank: false,
				value: this.jobId == null ? null : this.jobId
			}, {
				xtype: 'button',
				iconCls: 'search',
				tooltip: '查询',
				handler: this.loadData,
				scope: this
			}],
			
			items: {
				xtype: 'linechart',
            	url: framework.getUrl('/resources/ext/charts.swf'),
	            xField: 'taskDate',
	            yField: 'runTime',
	            
	            tipRenderer : function(chart, record){
	                return ['任务日期：', record.get('taskDate'), '\n任务名称：', record.get('name'), '\n运行时长: ', record.get('runTime'), ' 分钟'].join('');
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
	                displayName: '运行时长(分钟)',
	                yField: 'runTime'
	            }],
	            
	            listeners: {
					itemclick: this.onPointClick,
					scope: this
				},
	            
	            store: new Ext.data.JsonStore({
	            	url: 'statistic/statisticTaskRunTime4Chart',
	            	fields: ['taskId', 'taskDate', 'name', 'runTime'],
					
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
			
			fldJobId = tbar.getComponent(12),
			fldStartDate = tbar.getComponent(3),
			fldEndDate = tbar.getComponent(5),
			
			fldHour = tbar.getComponent(8),
			fldMinute = tbar.getComponent(9);
		
		if (!fldJobId.validate() || !fldStartDate.validate() || !fldEndDate.validate()) {
			return;
		}
			
		var job = framework.syncRequest({
			url: 'job',
			params: {id: fldJobId.getValue()},
			decode: true,
			root: 'job'
		});
		
		if (job.cycleType == JOB_CYCLE_TYPE.HOUR || job.cycleType == JOB_CYCLE_TYPE.MINUTE) {
			fldHour.allowBlank = false;
			fldMinute.allowBlank = false;
		} else {
			fldHour.allowBlank = true;
			fldMinute.allowBlank = true;
		}
		
		if (!fldHour.validate() || !fldMinute.validate()) {
			return;
		}
		
		var hour = fldHour.getValue(),
			minute = fldMinute.getValue();
		
		store.load({params: {
			jobId: fldJobId.getValue(),
			startDate: fldStartDate.getValue().format('Y-m-d'),
			endDate: fldEndDate.getValue().format('Y-m-d'),
			time: !Ext.isEmpty(hour, false) && !Ext.isEmpty(minute, false) ? hour + ':' + minute : null
		}});
	},
	
	onModuleRender: function() {
		com.sw.bi.scheduler.statistic.TaskRunTimeChartModule.superclass.onModuleRender.apply(this, arguments);
		
		if (!Ext.isEmpty(this.jobId, false) && this.startTaskDate != null) {
			this.loadData();
		}
	},
	
	onQuickDatesSelect: function(combo) {
		var days = combo.getValue(),
			endDate = new Date().add(Date.DAY, -1),
			startDate = endDate.add(Date.DAY, days * -1);
		
		Ext.getCmp('statDateStart').setValue(startDate);
		Ext.getCmp('statDateEnd').setValue(endDate);
		
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
		
		if (records.length == 0) {
			Ext.Msg.alert('提示', '指定日期范围内未找到指定作业ID的运行时长记录.');
		}
	},
	
	onPointClick: function(o) {
		this.loadTaskData({
			taskId: o.item.taskId,
			taskDateStart: o.item.taskDate,
			taskDateEnd: o.item.taskDate
		});
	}
	
});