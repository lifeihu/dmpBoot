_package('com.sw.bi.scheduler.serveroperate');

com.sw.bi.scheduler.serveroperate.ServerOperateResultModule = Ext.extend(framework.core.Module, {

	executeResult: null,
	
	center: function() {
		var mdl = this,
			items = [];
			
		Ext.iterate(mdl.executeResult, function(serverIp) {
			var log = mdl.executeResult[serverIp];
			
			items.push({
				xtype: 'panel',
				title: serverIp,
				autoScroll: true,
				html: Ext.isEmpty(log) ? '&nbsp;' : log.replace(/\n/g, '<br>')
			});
		});
		
		return {
			xtype: 'tabpanel',
			activeTab: 0,
			
			items: items
		}
	}
	
});