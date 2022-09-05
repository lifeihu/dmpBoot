_package('com.sw.bi.scheduler.permission');

_import([
	'framework.widgets.tree.CheckTreePanel'
]);

com.sw.bi.scheduler.permission.PermissionModule = Ext.extend(framework.core.Module, {
	
	/**
	 * @property roleId
	 * @type Number
	 * @description 需要被分配的角色
	 */
	roleId: null,
	
	initModule: function() {
		this.buttons = [{
			text: '保存',
			iconCls: 'save',
			disabled: USER_IS_ADMINISTRTOR,
			handler: this.grant,
			scope: this
		}];
		
		this.addEvents(
			/**
			 * @event granted
			 * @param framework.core.Module mdl
			 */
			'garnted'
		);
		
		com.sw.bi.scheduler.permission.PermissionModule.superclass.initModule.call(this);
	},
	
	center: function() {
		return {
			xtype: 'resourcepermissiontree',
			onceLoad: true,
			
			listeners: {
				afterrender: this.onTreeAfterRender,
				scope: this,
				delay: 500
			}
		};
	},
	
	grant: function() {
		var mdl = this;
		
		Ext.Ajax.request({
			url: 'permission/grant',
			params: {
				roleId: this.roleId,
				resourceIds: Ext.encode(this.centerPnl.getChecked('resourceId'))
			},
			
			success: function() {
				Ext.Msg.alert('提示', '授权成功!');
				
				if (mdl.moduleWindow) {
					mdl.moduleWindow.close();
				}
			}
		});
	},
	
	///////////////////////////////////////////////////////
	
	onTreeAfterRender: function() {
		var tree = this.centerPnl;

		var grantResources = 0, // 已经被授权的资源数量
			resources = Ext.decode(framework.syncRequest('permission/resources?roleId=' + this.roleId));

		if (resources.length > 0) {
			var loadMask = new Ext.LoadMask(tree.body, {
				msg: '正在展开所有菜单项,请耐心等候...'
			});
		
			loadMask.show.defer(1, loadMask);
			tree.setValue([1]);
			for (var i = 0; i < resources.length; i++) {
				var id = String(resources[i]),
					path = [1];
				
				for (var j = 2; j <= id.length; j += 2) {
					path.push(id.substring(0, j));
				}

				tree.selectPath('/' + path.join('/'), 'resourceId', function(selected, node) {
					if (selected) {
						tree.setValue([node.id]);
					}
					
					grantResources++;
					// 所有权限已经被设置
					if (grantResources == resources.length) {
						loadMask.hide();
					}
				});
			}
		} else {
			var root = tree.getRootNode();
			if (root.attributes.checked === true) {
				root.getUI().check(false);
			}
			
			root.expand();
		}
	}
});

com.sw.bi.scheduler.permission.ResourcePermissionTreePanel = Ext.extend(framework.widgets.tree.CheckTreePanel, {
	frame: false,
	border: true,
	autoScroll:true,
	bodyStyle: 'padding:3px;',
	
	root: {
		nodeType: 'async',
		id: 1,
		resourceId: 1,
		text: '系统资源'
	},
	
	initComponent: function() {
		var me = this;

		me.loader = new Ext.tree.TreeLoader({
			dataUrl: 'permission/resourceTree',
			baseParams: {
				userId: USER_ID,
				onceLoad: true
			}
		});
		
		com.sw.bi.scheduler.permission.ResourcePermissionTreePanel.superclass.initComponent.call(this);
	}
});
Ext.reg('resourcepermissiontree', com.sw.bi.scheduler.permission.ResourcePermissionTreePanel);