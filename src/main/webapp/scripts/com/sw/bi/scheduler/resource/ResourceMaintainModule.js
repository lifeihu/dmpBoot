_package('com.sw.bi.scheduler.resource');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.resource.ResourceMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	/**
	 * @cfg selectNode
	 * @type Ext.tree.TreeNode
	 */
	
	initModule: function() {
		com.sw.bi.scheduler.resource.ResourceMaintainModule.superclass.initModule.call(this);
		
		this.treeResource = Ext.getCmp('treeResource');
		this.selectNode = this.treeResource.getSelectionModel().getSelectedNode();

		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			savecomplete: this.onSaveComplete,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'resource',
			
			items: [{
				xtype: 'hidden',
				name: 'parentId'
			}, {
				xtype: 'hidden',
				name: 'sortNo'
			}, {
				xtype: 'hidden',
				name: 'iconCls'
			}, {
				columnWidth: 1,
				name: 'parentName',
				fieldLabel: '上级资源',
				readOnly: true
			}, {
				columnWidth: 1,
				name: 'name',
				fieldLabel: '资源名称'
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'menu',
				fieldLabel: '系统菜单',
				allowBlank: false,
				store: S.create('yesNo')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'type',
				fieldLabel: '资源类型',
				store: S.create('resourceType')
			}, {
				columnWidth: 1,
				name: 'url',
				fieldLabel: '资源链接'
			}]
		};
	},
	
	////////////////////////////////////////
	
	onLoadDataComplete: function(mdl, data) {
		var me = this,
			node = me.selectNode,
			
			parentId = node ? node.id : 1,
			parentName = node ? node.text : '系统资源';

		if (me.moduleWindow.action == 'create') {
			me.findField('parentId').setValue(parentId);
			me.findField('parentName').setValue(parentName);
			
			me.findField('resourceId').setValue(framework.syncRequest({
				url: 'resource/generate?parentId=' + parentId
			}));
		}
	},
	
	onBeforeSave: function(mdl, result, data) {
		var resource = data['resource'],
			parentId = resource.parentId;

		if (!Ext.isEmpty(parentId, false) && parentId > 1) {
			resource['parent.resourceId'] = parentId;
		}
			
		delete resource['parentId'];
		delete resource['parentName'];
		
		// 被保存的资源对象,在保存完毕后对象即被删除
		mdl._resource = resource;
	},
	
	onSaveComplete: function(mdl) {
		var action = mdl.moduleWindow.action,
			tree = mdl.treeResource,
			node = mdl.selectNode || tree.getRootNode();
			
		if (action == 'create') {
			if (!node.leaf) {
				node.reload();
				node.select();
			} else {
				node.parentNode.reload(function() {
					node = mdl.treeResource.getNodeById(node.id);
					node.expand();
					node.select();
				});
			}
		} else {
			var resource = mdl._resource,
				node = mdl.treeResource.getNodeById(resource.id);
				
			if (!Ext.isEmpty(node)) {
				node.parentNode.reload();
			}
		}
		
		delete mdl._resource;
	}
});