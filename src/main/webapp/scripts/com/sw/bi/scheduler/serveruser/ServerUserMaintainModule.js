_package('com.sw.bi.scheduler.serveruser');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.serveruser.ServerUserMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	initModule: function() {
		com.sw.bi.scheduler.serveruser.ServerUserMaintainModule.superclass.initModule.call(this);
		
		this.on({
			beforeloaddata: this.onBeforeLoadData,
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'serverUser',
			
			items: [{
				xtype: 'hidden', name: 'createdBy'
			}, {
				xtype: 'hidden', name: 'updatedBy'
			}, {
				columnWidth: 1,
				name: 'username',
				fieldLabel: '用户',
				allowBlank: false
			}, {
				columnWidth: 1,
				inputType:'password',
				name: 'password',
				fieldLabel: '密码',
				allowBlank: false
			}, {
				columnWidth: 1,
				name: 'description',
				fieldLabel: '显示名',
				allowBlank: false
			}]
		};
	},
	
	onBeforeLoadData: function(mdl, data) {
		if (mdl.moduleWindow.action != 'create') {
			var fldPwd = mdl.findField('password');
			fldPwd.allowBlank = true;
			fldPwd.hide();
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var serverUser = data['serverUser'];
		if (Ext.isEmpty(serverUser.serverUserId, false)) {
			serverUser.createdBy = USER_ID;
			serverUser.updatedBy = null;
		} else {
			serverUser.updatedBy = USER_ID;
		}
	}
	
});