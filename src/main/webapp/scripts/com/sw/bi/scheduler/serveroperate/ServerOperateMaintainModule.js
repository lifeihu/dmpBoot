_package('com.sw.bi.scheduler.serveroperate');

_import([
	'framework.modules.MaintainModule',
	'framework.widgets.form.MultiCombo'
]);

com.sw.bi.scheduler.serveroperate.ServerOperateMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	initModule: function() {
		com.sw.bi.scheduler.serveroperate.ServerOperateMaintainModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'serverOperate',
			
			items: [{
				xtype: 'hidden', name: 'createdBy'
			}, {
				xtype: 'hidden', name: 'updatedBy'
			}, {
				xtype: 'hidden', name: 'serverName'
			}, {
				xtype: 'hidden', name: 'status'
			}, {
				columnWidth: 1,
				name: 'name',
				fieldLabel: '名称',
				allowBlank: false
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'serverShellId',
				fieldLabel: '脚本',
				allowBlank: false,
				store: S.create('serverShells')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'serverUserId',
				fieldLabel: '帐号',
				allowBlank: false,
				store: S.create('serverUsers')
			}, {
				columnWidth: 1,
				xtype: 'multicombo',
				hiddenName: 'serverId',
				fieldLabel: '服务器',
				allowBlank: false,
				store: S.create('servers'),
				listeners: {
					select: this.onServerSelect,
					scope: this
				}
			}, {
				columnWidth: 1,
				name: 'businessGroup',
				fieldLabel: '业务组'
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'showResult',
				fieldLabel: '显示结果',
				store: S.create('yesNo'),
				allowBlank: false,
				value: false
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'description',
				fieldLabel: '备注'
			}]
		};
	},
	
	onServerSelect: function(combo, record) {
		this.findField('serverName').setValue(combo.getRawValue());
	},
	
	onLoadDataComplete: function(mdl, data) {
		if (mdl.moduleWindow.action == 'create') {
			mdl.findField('serverUserId').setValue(null);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var serverOperate = data['serverOperate'];
		if (Ext.isEmpty(serverOperate.serverOperateId, false)) {
			serverOperate.createdBy = USER_ID;
			serverOperate.updatedBy = null;
		} else {
			serverOperate.updatedBy = USER_ID;
		}
	}
	
});