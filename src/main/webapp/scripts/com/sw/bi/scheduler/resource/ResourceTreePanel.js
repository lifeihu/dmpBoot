_package('com.sw.bi.scheduler.resource');

com.sw.bi.scheduler.resource.ResourceTreePanel = Ext.extend(Ext.tree.TreePanel, {
	frame: false,
	border: true,
	autoScroll:true,
	bodyStyle: 'padding:3px;',
	
	/**
	 * @cfg onceLoad
	 * @type Boolean
	 * @description 是否一次加载所有资源
	 */
	onceLoad: true,
	
	root: {
		nodeType: 'async',
		id: 1,
		text: '系统资源',
		expanded: true
	},
	
	initComponent: function() {
		var me = this,
			createNode = function(attr) {
				if (Ext.isEmpty(attr.id, false)) {
					attr.id = attr.resourceId;
				}
				
				return Ext.tree.TreeLoader.prototype.createNode.call(this, attr);
			};
		
		me.loader = new Ext.tree.TreeLoader({
			dataUrl: 'permission/resourceTree',
			createNode: createNode,
			baseParams: {
				userId: USER_ID,
				onceLoad: this.onceLoad
			}
		});
		
		com.sw.bi.scheduler.resource.ResourceTreePanel.superclass.initComponent.call(this);
	}
});

Ext.reg('resourcetree', com.sw.bi.scheduler.resource.ResourceTreePanel);