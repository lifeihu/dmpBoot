_package('com.sw.bi.scheduler.action');

com.sw.bi.scheduler.action.ActionPIDModule = Ext.extend(framework.core.Module, {
	
	actionId: null,
	
	initModule: function() {
		this.buttons = [{
			text: '刷新',
			iconCls: 'refresh',
			handler: this.loadData,
			scope: this
		}];
		
		com.sw.bi.scheduler.action.ActionPIDModule.superclass.initModule.call(this);
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
			url: 'action/viewPID',
			params: {actionId: mdl.actionId},
			waitMsg: '正在加载进程信息,请耐心等候...',
			
			success: function(response) {
				if (!Ext.isEmpty(response.responseText, false)) {
					mdl.centerPnl.body.dom.innerHTML = '<pre>' + Ext.decode(response.responseText) + '</pre>';
				}
				
				Ext.Msg.hide();
			}
		});
	}
});