_package('com.sw.bi.scheduler.server');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.server.ServerModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'server',
	
	maintain: {
		title: '服务器维护',
		width: 400,
		height: 220,
		
		module: {
			module: 'com.sw.bi.scheduler.server.ServerMaintainModule'
		}
	},
	
	searcher: function() {
		return {
			items: [{
				name: 'name',
				fieldLabel: '名称'
			}, {
				name: 'ip',
				fieldLabel: 'IP'
			}]
		};
	},
	
	detailer: function() {
		return {
			columns: [{
				header: '名称',
				dataIndex: 'name',
				width: 250
			}, {
				header: 'IP',
				dataIndex: 'ip'
			}, {
				header: '备注',
				dataIndex: 'description',
				width: 300
			}]
		};
	}
	
});