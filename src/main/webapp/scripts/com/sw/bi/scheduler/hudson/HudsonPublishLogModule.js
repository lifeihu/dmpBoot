_package('com.sw.bi.scheduler.hudson');

com.sw.bi.scheduler.hudson.HudsonPublishLogModule = Ext.extend(framework.core.Module, {
	
	logFile: null,
	
	initModule: function() {
		this.buttons = [{
			text: '刷新',
			iconCls: 'refresh',
			handler: this.loadData,
			scope: this
		}];
		
		com.sw.bi.scheduler.hudson.HudsonPublishLogModule.superclass.initModule.call(this);
	},
	
	center: function() {
		return {
			xtype: 'panel',
			border: false,
			autoScroll: true	
		};
	},
	
	loadData: function() {
		var mdl = this;
		
		Ext.Ajax.request({
			url: 'hudsonProject/log',
			params: {logFile: mdl.logFile},
			waitMsg: '正在加载日志信息,请耐心等候...',
			
			success: function(response) {
				if (!Ext.isEmpty(response.responseText, false)) {
					mdl.centerPnl.body.dom.innerHTML = '<pre>' + Ext.decode(response.responseText) + '</pre>';
				}
				
				Ext.Msg.hide();
			}
		});
	}
});