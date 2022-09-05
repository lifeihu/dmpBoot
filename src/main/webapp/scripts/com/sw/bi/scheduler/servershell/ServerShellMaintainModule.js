_package('com.sw.bi.scheduler.servershell');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.servershell.ServerShellMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	initModule: function() {
		com.sw.bi.scheduler.servershell.ServerShellMaintainModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'serverShell',
			
			items: [{
				xtype: 'hidden', name: 'createdBy'
			}, {
				xtype: 'hidden', name: 'updatedBy'
			}, {
				columnWidth: 1,
				name: 'name',
				fieldLabel: '名称',
				allowBlank: false
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'shellType',
				fieldLabel: '脚本类型',
				allowBlank: false,
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[2, '脚本命令'],
						[1, '脚本文件']
					]
				}),
				
				listeners: {
					select: this.onShellTypeSelect,
					scope: this
				}
			}, {
				columnWidth: 1,
				hidden: true,
				xtype: 'textarea',
				name: 'path',
				fieldLabel: '全路径',
				height: 100
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'command',
				fieldLabel: '执行命令',
				height: 100
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'description',
				fieldLabel: '备注'
			}]
		};
	},
	
	onLoadDataComplete: function(mdl, data) {
		var serverShell = data['serverShell'],
			fldShellType = mdl.findField('shellType');

		if (mdl.moduleWindow.action != 'create') {	
			fldShellType.setValue(Ext.isEmpty(serverShell.path, false) ? 2 : 1);
			mdl.onShellTypeSelect(fldShellType);
		}
	},
	
	onShellTypeSelect: function(combo, record) {
		var shellType = combo.getValue(),
			fldPath = this.findField('path'),
			fldCommand = this.findField('command');
		
		if (shellType == 1) {
			fldPath.show();
			fldCommand.hide();
		} else {
			fldPath.hide();
			fldCommand.show();
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var serverShell = data['serverShell'];
		
		if (serverShell.shellType == 1) {
			serverShell.command = null;
		} else {
			serverShell.path = null;
		}
		
		if (Ext.isEmpty(serverShell.serverShellId, false)) {
			serverShell.createdBy = USER_ID;
			serverShell.updatedBy = null;
		} else {
			serverShell.updatedBy = USER_ID;
		}
	}
	
});