_package('com.sw.bi.scheduler.log');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.log.SchedulerExecutionLogModule = Ext.extend(framework.modules.SearchGridModule, {

	searcher: function() {
		return {
			items: [{
				xtype: 'combo',
				store: S.create('sevenDays'),
				fieldLabel: '最近七天',
				listeners: {
					select: this.onSevenDaysSelect,
					scope: this
				}
			}, {
				xtype: 'combo',
				hiddenName: 'gateway',
				fieldLabel: '网关机',
				allowBlank: false,
				store: S.create('activeGateways')
			}, {
				xtype: 'datefield',
				name: 'date',
				fieldLabel: '日期',
				allowBlank: false,
				value: new Date()
			}, {
				xtype: 'combo',
				hiddenName: 'hour',
				fieldLabel: '小时',
				allowBlank: false,
				store: S.create('hours')
			}, {
				columnWidth: .15,
				xtype: 'numberfield',
				name: 'tailNumber',
				fieldLabel: '返回行数'
			}]
		};
	},
	
	detailer: function() {
		return {
			xtype: 'panel',
			border: false,
			/*autoScroll: true,
			bodyStyle: 'padding:5px;',*/
			items: {
				id: 'logContent',
				xtype: 'textarea'
			},
			loadData: this.viewLog.createDelegate(this)
		};
	},
	
	viewLog: function(params) {
		var mdl = this,
			
			loadMask = new Ext.LoadMask(this.centerPnl.body, {
				msg: '正在加载日志,请耐心等候...'
			});
			
		loadMask.show();

		Ext.Ajax.request({
			url: 'toolbox/viewSchedulerLog',
			params: {
				gateway: params.gateway,
				date: params.date + ' ' + params.hour + ':00:00',
				tailNumber: params.tailNumber
			},
			
			success: function(response) {
				var content = response.responseText;
				
				if (!Ext.isEmpty(content, false)) {
					Ext.getCmp('logContent').setValue(Ext.decode(content));
					Ext.getCmp('logContent').el.scroll('b', 100000, false);
				}
				
				loadMask.hide();
			}
		});
	},
	
	doModuleLayout: function(mdl) {
		com.sw.bi.scheduler.log.SchedulerExecutionLogModule.superclass.doModuleLayout.call(this, mdl);
		
		Ext.getCmp('logContent').setSize(mdl.centerPnl.body.getSize(true));
	},
	
	onModuleRender: function() {
		com.sw.bi.scheduler.log.SchedulerExecutionLogModule.superclass.onModuleRender.apply(this, arguments);
		
		var hour = new Date().format('H');
		this.findField('hour').setValue(hour);
	},
	
	onSevenDaysSelect: function(combo) {
		var date = combo.getValue();
		if (date == null) return;
		
		this.northPnl.form.findField('date').setValue(date);
		this.loadData();
	}
	
});