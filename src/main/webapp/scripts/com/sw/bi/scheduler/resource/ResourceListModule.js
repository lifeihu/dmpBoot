_package('com.sw.bi.scheduler.resource');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.resource.ResourceListModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'resource',
	autoLoad: false,
	
	condition: {userId: USER_ID},
	
	maintain: {
		title: '资源维护',
		width: 550,
		height: 200,
		
		module: 'com.sw.bi.scheduler.resource.ResourceMaintainModule'
	},
	
	initModule: function() {
		if (!USER_IS_ADMINISTRTOR)
			this.actionUpdateConfig = false;
			
		com.sw.bi.scheduler.resource.ResourceListModule.superclass.initModule.call(this);
		
		this.on({
			beforeremove: this.onBeforeRemove,
			removecomplete:this.onRemoveComplete
		});
	},
	
	detailer: function() {
		return [{
			title: '资源列表',
			allowDefaultButtons: USER_IS_ADMINISTRTOR,
			
			columns: [{
				header: '资源名称',
				dataIndex: 'name',
				width: 200,
				
				renderer: function(value, meta, record) {
					var depth = String(record.get('id')).length / 2,
						prefix = '';
					
					for (var i = 1; i < depth; i++)
						prefix += '&nbsp;&nbsp;';
						
					return prefix + value;
				}
			}, {
				header: '上级资源',
				dataIndex: 'parentName',
				width: 150,
				align: 'center'
			}, {
				header: '资源地址',
				dataIndex: 'url',
				width: 350,
				
				renderer: function(value, meta, record) {
					var type = record.get('type');
					meta.style += 'color:' + (type == 1 ? 'orange' : type == 2 ? 'green' : 'black') + ';';
					
					return value;
				}
			}],
			
			store: {
				url: 'permission/resourceList',
				fields: ['type']
			}
		}];
	},
	
	////////////////////////////////////////////////////////
	
	onBeforeRemove: function(mdl, ids) {
		// 被删除的资源ID
		mdl._removedResourceId = ids;
	},
	
	onRemoveComplete: function(mdl) {
		var tree = Ext.getCmp('treeResource'),
			ids = mdl._removedResourceId;
			
		for (var i = 0; i < ids.length; i++) {
			var node = tree.getNodeById(ids[i]);
			if (!Ext.isEmpty(node)) {
				var parentNode = node.parentNode;
				if (parentNode.childNodes.length > 1) {
					parentNode.removeChild(node);
					parentNode.select();
					
				} else {
					parentNode.parentNode.reload(function() {
						tree.getNodeById(parentNode.id).select();
					});
				}
			}
		}
		
		delete mdl._removedResourceId;
	}
});

Ext.reg('resourcelistmodule', com.sw.bi.scheduler.resource.ResourceListModule);