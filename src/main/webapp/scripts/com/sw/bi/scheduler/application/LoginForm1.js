Ext.onReady(function() {
	var exception = SPRING_SECURITY_LAST_EXCEPTION_MESSAGE;
	if (!Ext.isEmpty(exception, false)) {
		var message = '';
		if (exception == 'UserDetailsService returned null, which is an interface contract violation') {
			message = '您指定的用户不存在!';
		} else if (exception == 'Bad credentials') {
			message = '密码错误!';
		}
		
		if (!Ext.isEmpty(message, false)) {
			Ext.Msg.alert('错误', message);
		}
	}
	
	var submit = function() {
			var form = frmLogin.getForm();

			if (form.isValid()) {
				form.el.dom.action = '/scheduler/j_spring_security_check';
				form.el.dom.submit();
			}
		},
			
		frmLogin = new Ext.FormPanel({
			frame: true,
			width: 300,
			height: 150,
			labelWidth: 60,
			renderTo: Ext.getBody(),
			
			title: '调度系统用户登录',
			
			items: [{
				xtype: 'textfield',
				name: 'j_username',
				fieldLabel: '登 录 名',
				allowBlank: false,
				anchor: '99%',
				value: ''
			}, {
				xtype: 'textfield',
				name: 'j_password',
				fieldLabel: '密　 码',
				inputType: 'password',
				allowBlank: false,
				anchor: '99%',
				value: ''
			}/*, {
				xtype: 'checkbox',
				name: '_spring_security_remember_me',
				hideLabel: true,
				boxLabel: '记住此用户',
				align: 'right'
			}*/],
			
			buttons: [{
				text: '登录',
				tooltip: '登录系统(Shift-Enter)',
				handler: submit
			}]
		});
	
	frmLogin.render();
	frmLogin.el.alignTo(Ext.getBody(), 'c-c?');
	
	// 添加Shift-Enter为默认登录键
	var keyMap = new Ext.KeyMap(Ext.getBody());
	keyMap.addBinding([{
		key: '\r',
		shift: true,
		fn: submit
	}, {
		key: '\r',
		fn: submit
	}]);
});