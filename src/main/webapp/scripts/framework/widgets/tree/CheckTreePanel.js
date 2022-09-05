_package('framework.widgets.tree');

_import([
	'framework.widgets.tree.TreeCheckNodeUI'
]);

framework.widgets.tree.CheckTreePanel = Ext.extend(Ext.tree.TreePanel, {
	// lines: false,
	frame: true,
	border: true,
	autoScroll: true,
	
	/**
	 * @cfg checkModel
	 * @type String
	 * @description 选择模式
	 * 		single: 		单选
	 * 		multiple: 		多选(无级联效果)
	 * 		cascade:		级联选中父节点和子节点(默认)
	 * 		cascadeparent:  级联选中父节点
	 * 		cascadechild:	级联选中子节点
	 */
	checkModel: 'cascade',
	
	/**
	 * @cfg onlyLeafCheckable
	 * @type Boolean
	 * @description 是否只有叶子节点才允许选择
	 */
	onlyLeafCheckable: false,
	
	initComponent: function() {
		if (this.checkModel != 'single')
			this.selModel = new Ext.tree.MultiSelectionModel();
		
		this.root.uiProvider = framework.widgets.tree.TreeCheckNodeUI;
		
		var baseAttrs = this.loader.baseAttrs || {};
		baseAttrs.uiProvider = framework.widgets.tree.TreeCheckNodeUI;
		this.loader.baseAttrs = baseAttrs;
		
		framework.widgets.tree.CheckTreePanel.superclass.initComponent.call(this);
	},
	
	/**
	 * 获得选中的节点
	 * @param {String} attr
	 */
	getChecked: function(attr) {
		var nodes = [];
		var tree = this;
		Ext.iterate(this.nodeHash, function(key) {
			var node = tree.nodeHash[key];
			if (node.attributes.checked) {
				if (Ext.isEmpty(attr, false))
					nodes.push(node);
				else 
					nodes.push(attr == 'id' ? node.id : node.attributes[attr]);
			}
		});
		
		return nodes;
	},
	
	/**
	 * @param menus 
	 * @type {String}
	 * @description 菜单
	 */
	setValue: function(menus) {
		if (Ext.isEmpty(menus, false)) return;
		
		if (Ext.isString(menus))
			this.setValue(menus.split(','));

		var tree = this;
		Ext.iterate(menus, function(menu) {
			var node = tree.nodeHash[menu];
			if (node) {
				var ui = node.getUI();
				var checkModel = ui.checkModel;
				ui.checkModel = 'multiple';
				ui.check(true);
				ui.checkModel = checkModel;
			}
		});
	}
});

Ext.reg('checktree', framework.widgets.tree.CheckTreePanel);