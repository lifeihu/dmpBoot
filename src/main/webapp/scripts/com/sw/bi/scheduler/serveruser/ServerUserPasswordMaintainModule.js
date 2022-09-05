_package('com.sw.bi.scheduler.serveruser');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.serveruser.ServerUserPasswordMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	saveUrl: 'serverUser/password',
	
	/**
	 * 需要修改密码的用户
	 * @type 
	 */
	serverUserId: null,
	
	initModule: function() {
		var mdl = this;
		
		com.sw.bi.scheduler.serveruser.ServerUserPasswordMaintainModule.superclass.initModule.call(this);
		
		this.on({
			beforesave: this.onBeforeSave,
			savecomplete: function(mdl, result, data) {
				Ext.Msg.alert('提示', '密码修改成功.');
			},
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'serverUser',
			
			items: [{
				columnWidth: 1,
				inputType:'password',
				name: 'newPassword',
				fieldLabel: '新密码',
				anchor: '99%',
				allowBlank: false
			}, {
				columnWidth: 1,
				inputType:'password',
				name: 'rePassword',
				fieldLabel: '确认密码',
				anchor: '99%',
				allowBlank: false
			}]
		};
	},
	
	onBeforeSave: function(mdl, params) {
		var serverUser = params['serverUser'],
			
			newPassword = serverUser.newPassword,
			rePassword = serverUser.rePassword;
			
		if (!Ext.isEmpty(newPassword, false)) {
			if (newPassword != rePassword) {
				var plg = mdl.getModuleValidatePlugin();
				plg.addError('密码不一致,请重新输入!');
				plg.showErrors();
				return false;
			}
		}

		delete params['serverUser'];
		params.serverUserId = mdl.serverUserId;
		params.password = newPassword;
	}
});