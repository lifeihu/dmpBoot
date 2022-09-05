_package('com.sw.bi.scheduler.action');

com.sw.bi.scheduler.action.ActionLogModule = Ext.extend(framework.core.Module, {
	
	actionId: null,
	
	initModule: function() {
		this.buttons = [{
			text: '刷新',
			iconCls: 'refresh',
			handler: this.loadData,
			scope: this
		}];
		
		com.sw.bi.scheduler.action.ActionLogModule.superclass.initModule.call(this);
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
			url: 'action/log',
			params: {actionId: mdl.actionId},
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