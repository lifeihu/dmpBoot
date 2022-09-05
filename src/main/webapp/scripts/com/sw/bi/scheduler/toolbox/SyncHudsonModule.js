_package('com.sw.bi.scheduler.toolbox');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.toolbox.SyncHudsonModule = Ext.extend(framework.core.Module, {
	
	center: function() {
		return {
			xtype: 'grid',
			
			actions: [{
				text: '更新',
				width: 40,
				handler: this.sync,
				scope: this
			}],
			
			columns: [{
				header: '项目',
				dataIndex: 'name',
				width: 150
			}, {
				header: '状态',
				dataIndex: 'state',
				width: 320
			}],
			
			store: S.create('hudsonProjects')
		};
	},
	
	sync: function(record) {
		Ext.Ajax.request({
			timeout: 300000000,
			url: 'toolbox/syncHudson',
			waitMsg: '正在更新,请耐心等候...',
			params: {project: record.get('value')},
			success: function(response) {
				if (Ext.isEmpty(response.responseText, false)) {
					record.set('state', '更新失败.')
				} else {
					var result = Ext.decode(response.responseText);
					record.set('state', '更新成功, 最新版本: ' + result);
				}
			}
		});
	}
});