_package('com.sw.bi.scheduler.usergroup');

_import([
	'framework.widgets.plugins.TreePagingPlugin'
]);

com.sw.bi.scheduler.usergroup.UserGroupTreePanel = Ext.extend(Ext.tree.TreePanel, {
	frame: false,
	border: false,
	autoScroll:true,
	bodyStyle: 'padding:3px;',
	rootVisible: false,
	
	/**
	 * 是否只显示未删除的公司和用户组
	 * @type Boolean
	 */
	isOnlyActive: true,
	
	/**
	 * @cfg allowMaintain
	 * @type Boolean
	 * @description 允许维护
	 */
	allowMaintain: false,
	
	/**
	 * @cfg companyIds
	 * @type String
	 * @description 只显示指定的公司组织机构(多个公司以,分隔)
	 */
	companyIds: null,
	
	plugins: [{
		ptype: 'treepagingplugin'
	}],
	
	initComponent: function() {
		var me = this,
		
			createNode = function(attr) {
				var id = attr.id;
				
				if (id != 'root') {
					attr.iconCls = 'user-group';
					attr.text = attr.name;
					attr.id = attr.userGroupId;
				}
				
				if (attr.active !== true) {
					attr.cls = 'x-record-unactive';
				}
				
				return Ext.tree.TreeLoader.prototype.createNode.call(this, attr);
			};
		
		me.root = {
			nodeType: 'async',
			id: 'root',
			expanded: true
		},
		
		me.loader = new Ext.tree.TreeLoader({
			dataUrl: 'userGroup/getUserGroups',
			createNode: createNode,
			listeners: {
				beforeload: this.onNodeBeforeLoad,
				scope: this
			}
		});
		
		me.initUserGroupMenu();
		
		com.sw.bi.scheduler.usergroup.UserGroupTreePanel.superclass.initComponent.call(this);
	},
	
	/**
	 * 初始化右键菜单
	 */
	initUserGroupMenu: function() {
		var me = this;
		
		me.userGroupMenu = new Ext.menu.Menu({
			items: [{
				name: 'addUserGroup',
				iconCls: 'add',
				text: '添加用户组'
			}, {
				name: 'updateUserGroup',
				iconCls: 'edit',
				text: '修改用户组'
			}, '-', {
				name: 'removeUserGroup',
				iconCls: 'remove',
				text: '删除用户组'
			}, {
				name: 'disableUserGroup',
				iconCls: 'remove',
				text: '禁用用户组'
			}, {
				name: 'recoveryUserGroup',
				iconCls: 'recovery',
				text: '恢复用户组'
			}, '-', {
				name: 'assignUser',
				iconCls: 'menu-user',
				text: '分配用户'
			}, '-', {
				name: 'refreshUserGroup',
				iconCls: 'refresh',
				text: '刷新用户组'
			}],
			
			listeners: {
				beforeshow: this.onUserGroupMenuBeforeShow,
				itemclick: this.onUserGroupMenuItemClick,
				scope: this
			}
		});
		
		me.on({
			contextmenu: me.onContextMenu,
			containercontextmenu: me.onContextMenu,
			scope: me
		});
	},
	
	/**
	 * 维护用户组
	 * @param {} node
	 * @param {} isCreate
	 */
	updateUserGroup: function(node, isCreate) {
		var win = framework.createWindow({
			title: '用户组维护',
			iconCls: 'user-group',
			width: 500,
			height: 400,
			
			module: {
				module: 'com.sw.bi.scheduler.usergroup.UserGroupMaintainModule',
				node: node
			}
		}, 'framework.widgets.window.MaintainWindow');
		
		win.on({
			savecomplete: this.onSaveComplete.createDelegate(this, [node], true),
			scope: this,
			single: true
		});
		
		if (isCreate === true) {
			win.create();
			
		} else {
			win.updateOnly({
				userGroupId: node.attributes.userGroupId
			});
		}
	},
	
	/**
	 * 删除用户组
	 * @param {} node
	 */
	removeUserGroup: function(node) {
		var mdl = this,
			attrs = node.attributes;
			
		Ext.Msg.confirm('提示', '是否需要删除 "' + node.text + '" 及其子用户组?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url:  'userGroup/remove',
				params: {id: attrs.userGroupId},
				
				success: function(response) {
					node.parentNode.reload();
				}
			});
		});
	},
	
	/**
	 * 禁用用户组
	 * @param {} node
	 */
	disableUserGroup: function(node) {
		var mdl = this,
			attrs = node.attributes;
			
		Ext.Msg.confirm('提示', '是否需要禁用 "' + node.text + '" 及其子用户组?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url:  'userGroup/logicRemove',
				params: {id: attrs.userGroupId},
				
				success: function(response) {
					node.attributes.active = false;
					node.getUI().addClass('x-record-unactive');
					
					node.reload();
				}
			});
		});
	},
	
	/**
	 * 恢复用户组
	 * @param {} node
	 */
	recoveryUserGroup: function(node) {
		var mdl = this,
			attrs = node.attributes;
			
		Ext.Msg.confirm('提示', '是否恢复 "' + node.text + '" 及其子用户组?', function(btn) {
			if (btn != 'yes') return;
			
			Ext.Ajax.request({
				url:  'userGroup/recovery',
				params: {id: attrs.userGroupId},
				
				success: function(response) {
					var parentNode = node;
					while (parentNode) {
						parentNode.attributes.active = true;
						parentNode.getUI().removeClass('x-record-unactive');
						
						parentNode = parentNode.parentNode;
					}
					
					node.parentNode.reload();
				}
			});
		});
	},
	
	assignUser: function(node) {
		framework.createWindow({
			title: '用户组分配用户',
			iconCls: 'menu-user',
			width: 700,
			height: 600,
			
			module: {
				module: 'com.sw.bi.scheduler.usergroup.UserGroupAssignUserModule',
				userGroup: node.attributes
			}
		}, 'framework.widgets.window.ModuleWindow').open();
	},
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	onNodeBeforeLoad: function(loader, node) {
		var me = this,
			dataUrl = null,
			attrs = node.attributes,
			
			condition = {};
		
		dataUrl = 'userGroup/list';
		
		if (node.id !== 'root') {
			condition['parentId-eq'] = attrs.userGroupId;
		} else {
			condition['parentId-nul'] = true;
		}
		
		if (me.isOnlyActive === true) {
			condition = Ext.apply(condition, {
				'active-eq': true
			});
		}
		
		loader.baseParams = {
			sort: 'sortNo',
			dir: 'asc',
			condition: condition
		}
		
		loader.dataUrl = dataUrl;
	},
	
	onContextMenu: function(node, e) {
		var me = this;

		if (me.userGroupMenu) {
			e.stopEvent();
			
			var menu = me.userGroupMenu;
			
			if (node instanceof Ext.tree.TreeNode) {
				node.ownerTree.getSelectionModel().select(node);
				menu.selectNode = node;
			} else {
				menu.selectNode = null;
			}
			
			menu.showAt(e.getXY());
		}
	},
	
	onUserGroupMenuBeforeShow: function(menu) {
		var me = this,
		
			mnuAdd = menu.getComponent(0),
			mnuUpdate = menu.getComponent(1),
			mnuSep1 = menu.getComponent(2),
			mnuRemove = menu.getComponent(3),
			mnuDisable = menu.getComponent(4),
			mnuRecovery = menu.getComponent(5),
			mnuSep2 = menu.getComponent(6),
			mnuAssign = menu.getComponent(7),
			mnuSep3 = menu.getComponent(8),
			mnuRefresh = menu.getComponent(9),
			
			node = menu.selectNode,
			allowMaintain = me.allowMaintain;
		
		mnuAdd.hide();
		mnuUpdate.hide();
		mnuSep1.hide();
		mnuRemove.hide();
		mnuDisable.hide();
		mnuRecovery.hide();
		mnuSep2.hide();
		mnuAssign.hide();
		mnuSep3.hide();
		mnuRefresh.hide();

		if (Ext.isEmpty(node)) {
			mnuAdd.show();
			mnuSep2.show();
			mnuRefresh.show();
			
			return;
			
		} else if (allowMaintain === false) {
			mnuRefresh.show();
			return;
		}
		
		var	attrs = node.attributes,
			isActive = attrs.active;

		if (isActive !== true) {
			mnuRecovery.show();
			mnuSep2.show();
			mnuRefresh.show();
			
		} else {
			mnuAdd.show();
			mnuUpdate.show();
			mnuSep1.show();
			mnuRemove.show();
			mnuDisable.show();
			mnuSep2.show();
			mnuAssign.show();
			mnuSep3.show();
			mnuRefresh.show();
		}
	},
	
	onUserGroupMenuItemClick: function(item, e) {
		var me = this,
			itemId = item.name,
			
			node = item.ownerCt.selectNode || me.getRootNode();
		
		if (itemId == 'disableUserGroup') {
			me.disableUserGroup(node);
			
		} else if (itemId == 'removeUserGroup') {
			me.removeUserGroup(node);
			
		} else if (itemId == 'refreshUserGroup') {
			node.reload();
			
		} else if (itemId == 'recoveryUserGroup') {
			me.recoveryUserGroup(node);
			
		} else if (itemId == 'assignUser') {
			me.assignUser(node);
			
		} else {
			var isCreate = itemId == 'addUserGroup';
			me.updateUserGroup(node, isCreate);
		}
	},
	
	onSaveComplete: function(mdl, result, data, node) {
		var me = this,
			action = mdl.moduleWindow.action;
		
		if (action == 'create') {
			node.reload(function() {
				var newNode = node.findChild('name', data['userGroup'].name);
				me.getSelectionModel().select(newNode);
			});
			
		} else if (action == 'updateOnly') {
			node.setText(data['userGroup'].name);
			
		}
	},
	
	beforeDestroy: function() {
		if (this.userGroupMenu) {
			this.userGroupMenu.destroy();
			this.userGroupMenu = null;
		}
		
		com.sw.bi.scheduler.usergroup.UserGroupTreePanel.superclass.beforeDestroy.apply(this, arguments);
	}
});