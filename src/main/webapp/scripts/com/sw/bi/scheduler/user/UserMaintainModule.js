_package('com.sw.bi.scheduler.user');

_import([
	'framework.modules.MaintainModule',
	
	'framework.widgets.AssignPanel'
]);

com.sw.bi.scheduler.user.UserMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	
	initModule: function() {
		// 未分配的角色
		this.notAssignStore = S.create({
			url: 'role/notAssignRoles',
			fields: ['roleId', 'roleName'],
			autoLoad: false,
			sortInfo: {
				field: 'roleName',
				direction: 'asc'
			}
		});
		
		// 已分配的角色
		this.assignStore = S.create({
			url: 'role/assignRoles',
        	fields: ['roleId', 'roleName'],
        	autoLoad: false
		});
		
		com.sw.bi.scheduler.user.UserMaintainModule.superclass.initModule.call(this);
		
		this.on({
			beforeloaddata: this.onBeforeLoadData,
			beforesave: this.onBeforeSave,
			savecomplete: this.onSaveComplete,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'user',
			
			items: [{
				xtype: 'hidden', name: 'isAdmin', value: false
			}, {
				xtype: 'hidden', name: 'passwd'
			}, {
				xtype: 'hidden', name: 'status'
			}, {
				columnWidth: .5,
				name: 'userName',
				fieldLabel: '登录名',
				allowBlank: false,
				labelWidth: 55,
				maxLength: 50
			}, {
				columnWidth: .5,
				name: 'realName',
				fieldLabel: '姓名',
				allowBlank: false,
				labelWidth: 35,
				maxLength: 50,
				anchor: '100%',
				maxLength: 35
			}, {
				columnWidth: 1,
				name: 'email',
				fieldLabel: '电子邮件',
				allowBlank: false,
				labelWidth: 55,
				vtype: 'email',
				anchor: '100%'
			}, {
				columnWidth: 1,
				name: 'mobilePhone',
				fieldLabel: '手机',
				labelWidth: 55,
				vtype: 'mutilmobile',
				allowBlank: false,
				maxLength: 35,
				anchor: '100%'
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'userType',
				fieldLabel: '用户类型',
				allowBlank: false,
				store: S.create('userType'),
				labelWidth: 55,
				anchor: '100%'
			}/*, {
				columnWidth: .5,
				xtype: 'combo',
				hiddenName: 'status',
				fieldLabel: '状态',
				allowBlank: false,
				store: S.create('userStatus'),
				labelWidth: 60
			}*/, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'comments',
				fieldLabel: '备注',
				labelWidth: 55,
				anchor: '100%'
			}]
		};
	},
	
	detailer: function() {
		var mdl = this;
		
		return [{
			xtype: 'assignpanel',
			
			title: '分配角色',
			model: 'userRoles',
			childrenDataRoot: 'roles',
			
			leftConfig: {
				store: mdl.notAssignStore
			},
			
			rightConfig: {
				store: mdl.assignStore
			}
		}]
	},
	
	////////////////////////////////////////////////////////////////////
	
	onBeforeLoadData: function(mdl, data) {
		var loadMask = new Ext.LoadMask(mdl.centerPnl.body, {
			msg: '正在加载角色信息,请耐心等候...'
		});
		
		loadMask.show();
		
		(function() {
			var userId = null;
			if (mdl.moduleWindow.action != 'create') {
				userId = data['user'].userId;
				
				mdl.assignStore.load({params: {userId: userId}});
			}
			
			mdl.notAssignStore.load({params: {userId: userId}});
			
			loadMask.hide.defer(500, loadMask);
		}).defer(1);
	},
	
	onBeforeSave: function(mdl, data) {
		var masterData = data[mdl.masterPnl.model],
			roleIds = data['userRoles'].assignIds,
			roles = [];

		if (!Ext.isEmpty(roleIds, false)) {
			Ext.iterate(roleIds.split(','), function(roleId) {
				roles.push({
					roleId: roleId
				});
			});
		}
		data['userRoles'] = roles;

		if (Ext.isEmpty(masterData.userId, false)) {
			masterData.passwd = '123456';
		}
	},
	
	onSaveComplete: function(mdl, result, data) {
		var userId = mdl.findField('userId').getValue();
		
		if (Ext.isEmpty(userId, false)) {
			Ext.Msg.alert('提示', '新建用户的初始密码为: <span style="color:red;font-weight:bold;font-size:14px;">123456</span>,<br> 请尽快登录后重新修改密码!');
		}
	}
	
});