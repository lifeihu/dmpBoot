_package('com.sw.bi.scheduler.usergroup');

_import([
	'framework.modules.MaintainModule',
	'framework.widgets.AssignPanel'
]);

com.sw.bi.scheduler.usergroup.UserGroupAssignUserModule = Ext.extend(framework.modules.MaintainModule, {
	
	/**
	 * 用户组
	 * @type 
	 */
	userGroup: null,
	
	initModule: function() {
		this.tbuttons = [{
			text: '保存',
			iconCls: 'save',
			handler: this.save,
			scope: this
		}];
		
		// 未分配的用户
		this.notAssignStore = S.create({
			url: 'user/notAssignUsers',
			fields: ['userId', 'realName'],
			autoLoad: false,
			sortInfo: {
				field: 'realName',
				direction: 'asc'
			}
		});
		
		// 已分配的角色
		this.assignStore = S.create({
			url: 'user/assignUsers',
        	fields: ['userId', 'realName'],
        	autoLoad: false
		});
		
		com.sw.bi.scheduler.usergroup.UserGroupAssignUserModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			// beforesave: this.onBeforeSave,
			// savecomplete: this.onSaveComplete,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'userGroupRelation',
			
			items: [{
				columnWidth: 1,
				name: 'userGroupName',
				fieldLabel: '用户组',
				value: this.userGroup.name
			}]
		};
	},
	
	detailer: function() {
		return [{
			xtype: 'assignpanel',
			
			title: '分配用户',
			model: 'userGroups',
			childrenDataRoot: 'users',
			
			leftConfig: {
				legend: '未分配用户',
				displayField: 'realName',
                valueField: 'userId',
				store: this.notAssignStore
			},
			
			rightConfig: {
				legend: '已分配用户',
				displayField: 'realName',
                valueField: 'userId',
				store: this.assignStore
			}
		}]
	},
	
	save: function() {
		var mdl = this,
		
			userIds = [],
			users = this.assignStore.getRange();
			
		Ext.iterate(this.assignStore.getRange(), function(user) {
			userIds.push(user.get('userId'));
		});

		Ext.Ajax.request({
			url: 'userGroupRelation/assignUsers',
			params: {
				userGroupId: mdl.userGroup.userGroupId,
				userId: userIds.join(',')
			},
			
			success: function() {
				mdl.moduleWindow.close();
			}
		});
	},
	
	/////////////////////////////////////////////////////////////////////////////
	
	onLoadDataComplete: function(mdl, data) {
		mdl.assignStore.baseParams.userGroupId = mdl.userGroup.userGroupId;
		mdl.assignStore.load();
		
		mdl.notAssignStore.load();
	},
	
	onModuleRender: function() {
		com.sw.bi.scheduler.usergroup.UserGroupAssignUserModule.superclass.onModuleRender.apply(this, arguments);
		
		this.loadDataUrl = null;
	}
	
});