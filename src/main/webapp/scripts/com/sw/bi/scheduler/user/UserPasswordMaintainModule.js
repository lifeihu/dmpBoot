_package('com.sw.bi.scheduler.user');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.user.UserPasswordMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	saveUrl: 'user/password',
	
	userId: null,
	
	userName: null,
	
	initModule: function() {
		var mdl = this;
		
		this.buttons = [{
			iconCls: 'save',
			text: '随机生成',
			handler: function() {
				var fldNewPassword = mdl.findField('newPassword'),
					fldRePassword = mdl.findField('rePassword');
				
				fldNewPassword.allowBlank = true;
				fldRePassword.allowBlank = true;
				
				mdl.save(function() {
					mdl.moduleWindow.close();
				});
			}
		}];
		
		com.sw.bi.scheduler.user.UserPasswordMaintainModule.superclass.initModule.call(this);
		
		this.on({
			beforesave: this.onBeforeSave,
			savecomplete: function(mdl, data) {
				Ext.Msg.alert('提示', '"' + (mdl.userName || USER_NAME) + '" 的新密码  <span style="color:blue;">' + data + '</span> 请牢记!');
			},
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'user',
			
			items: [/*{
				xtype: 'hidden', name: 'userId', value: USER_ID	
			}, */{
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
		var user = params['user'],
			
			newPassword = user.newPassword,
			rePassword = user.rePassword;
			
		if (newPassword != rePassword) {
			var plg = mdl.getModuleValidatePlugin();
			plg.addError('密码不一致,请重新输入!');
			plg.showErrors();
			return false;
		}

		delete params['user'];
		params.userId = mdl.userId || USER_ID;
		params.password = newPassword;
	}
});