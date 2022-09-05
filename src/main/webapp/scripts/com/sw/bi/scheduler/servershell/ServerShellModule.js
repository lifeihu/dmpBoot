_package('com.sw.bi.scheduler.servershell');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.servershell.ServerShellModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'serverShell',
	
	maintain: {
		title: '脚本维护',
		width: 500,
		height: 300,
		
		module: {
			module: 'com.sw.bi.scheduler.servershell.ServerShellMaintainModule'
		}
	},
	
	searcher: function() {
		return {
			items: [{
				name: 'name',
				fieldLabel: '名称'
			}, {
				name: 'path',
				fieldLabel: '路径'
			}, {
				name: 'command',
				fieldLabel: '执行命令'
			}]
		};
	},
	
	detailer: function() {
		return {
			columns: [{
				header: '名称',
				dataIndex: 'name',
				width: 150
			}, {
				header: '全路径',
				dataIndex: 'path',
				width: 200,
				tooltip: true
			}, {
				header: '执行命令',
				dataIndex: 'command',
				width: 300,
				tooltip: true
			}, {
				header: '备注',
				dataIndex: 'description',
				width: 300
			}]
		};
	}
	
});