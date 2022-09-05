_package('com.sw.bi.scheduler.application');

com.sw.bi.scheduler.application.LoginForm = Ext.extend(Ext.form.FormPanel, {
	frame: true,
	width: 300,
	height: 170,
	labelWidth: 60,
	renderTo: Ext.getBody(),
	
	stateful: true,
	stateId: 'c_scheduler_login',
	
	//title: '调度系统-用户登录',
	
	initComponent: function() {
		this.items = [{
			xtype: 'textfield',
			cls: 'bg-username',
			name: 'j_username',
			fieldLabel: '登 录 名',
			allowBlank: false,
			anchor: '99%',
			listeners: {
				blur: this.getUserMobiles,
				scope: this
			}
		}, {
			xtype: 'textfield',
			name: 'mobile',
			cls: 'bg-mobile',
			fieldLabel: '手 机 号',
			allowBlank: false,
			readOnly: true,
			anchor: '99%'
		}, {
			xtype: 'textfield',
			name: 'j_password',
			cls: 'bg-password',
			fieldLabel: '密　 码',
			inputType: 'password',
			allowBlank: false,
			anchor: '99%'
		}, {


			layout : 'column',
			items : [{
				layout : 'form',
				items: [{
					xtype: 'textfield',
					name: 'mobileCode',
					cls: 'bg-mobileCode',
					fieldLabel: '验 证 码',
					allowBlank: false,
					anchor: '68%',
					maxLength: 6,

				}, {
					cls: 'yzm',
					xtype : 'button',
					text: '获取验证码',
					handler: this.getVertifyCode,
					scope: this
				}]

			}]

		}];
		
/*		this.buttons = [{
			text: '获取验证码',
			handler: this.getVertifyCode,
			scope: this
		}, {
			text: '登录',
			tooltip: '登录系统(Shift-Enter)',
			handler: this.login,
			scope: this
		}];*/

		this.buttons = [ {
			text: '登录',
			cls: 'btnLogin',
			tooltip: '登录系统(Shift-Enter)',
			handler: this.login,
			scope: this
		}];
		
		com.sw.bi.scheduler.application.LoginForm.superclass.initComponent.call(this);
	},
	
	/**
	 * 获得指定登录用户的所有手机
	 */
	getUserMobiles: function(callback) {
		var form = this.form,
			fldMobile = form.findField('mobile'),
			username = form.findField('j_username').getValue();
			
		if (Ext.isEmpty(username, false)) {
			return;
		}
		
		Ext.Ajax.request({
			url: 'login/mobile',
			params: {
				username: username
			},
			
			success: function(response) {
				fldMobile.setValue(Ext.decode(response.responseText));
				form.findField('j_password').focus();
			}
		});
	},
	
	/**
	 * 向指定手机发送验证码
	 */
	getVertifyCode: function() {
		var form = this.form,
			username = form.findField('j_username').getValue(),
			password = form.findField('j_password').getValue(),
			mobile = form.findField('mobile').getValue();
			
		if (Ext.isEmpty(username, false)) {
			Notice.warn('未指定登录用户.');
			return;
		}
		
		if (Ext.isEmpty(password, false)) {
			Notice.warn('未输入登录密码.');
			return;
		}
		
		if (Ext.isEmpty(mobile, false)) {
			Notice.warn('未指定手机号.');
			return;
		}
		
		Ext.Ajax.request({
			url: 'login/mobileCode',
			params: {
				username: username,
				password: password,
				mobile: mobile
			},
			
			success: function() {
				Notice.info('验证码已经发送至手机.');
				form.findField('mobileCode').focus();
			}
		});
	},
	
	/**
	 * 用户登录
	 */
	login: function() {
		var me = this,
			form = this.form;

		if (form.isValid()) {
			Ext.Ajax.request({
				url: CONTEXT_PATH + '/manage/login/validate',
				params: {
					username: form.findField('j_username').getValue(),
					mobile: form.findField('mobile').getValue(),
					password: form.findField('j_password').getValue(),
					vertifyCode: form.findField('mobileCode').getValue()
				},
				
				success: function() {
					form.el.dom.action = CONTEXT_PATH + '/j_spring_security_check';
					form.el.dom.submit();
					
					me.saveState();
				}
			});
		}
	},
	
	getState: function() {
		var form = this.form;
		return {
			'username': form.findField('j_username').getValue(),
			'mobile': form.findField('mobile').getValue()
		};
	},
	
	applyState: function(state) {
		var form = this.form,
		
			username = state['username'],
			mobile = state['mobile'];

		if (Ext.isEmpty(username, false)) {
			return;
		}
		
		form.findField('j_username').setValue(username);
		this.getUserMobiles(function() {
			form.findField('mobile').setValue(mobile);
		});
	},
	
	///////////////////////////////////////////////////////////////////////////////////
	
	afterRender: function() {
		com.sw.bi.scheduler.application.LoginForm.superclass.afterRender.apply(this, arguments);
		
		this.el.alignTo(Ext.getBody(), 'c-c?');
		this.form.findField('j_username').focus();
		
		var keyMap = new Ext.KeyMap(Ext.getBody());
		keyMap.addBinding([{
			key: '\r',
			shift: true,
			fn: this.login,
			scope: this
		}, {
			key: '\r',
			fn: this.login,
			scope: this
		}]);
	}

});

framework.readyModule('com.sw.bi.scheduler.application.LoginForm');