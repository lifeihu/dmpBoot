_package('framework.widgets.form');

framework.widgets.form.ComboBoxTree = Ext.extend(Ext.form.ComboBox, {
	/**
	 * @cfg tree 树
	 */
	tree: null,
	
	/**
	 * @cfg treeCfg 树的配置参数
	 */
	treeCfg: {},
	
	/**
	 * @cfg selectNodeModel 选择模式
	 * 		all: 所有节点都可选择
	 * 		exceptRoot: 除根节点其他节点都可选择(默认)
	 * 		folder: 只允许选择目录(非叶子和非根节点)
	 * 		leaf: 只允许选择叶子节点
	 */
	selectNodeModel: "leaf",
	
	trigger1Class: 'x-form-clear-trigger',

	constructor: function(config) {
		this.treeId = Ext.id() + "-combo-tree",
		this.maxHeight = config.maxHeight || config.height || 260;
		this.store = new Ext.data.SimpleStore({fields:[],data:[[]]});
		this.selectedClass = '';
		this.mode = 'local';
		this.triggerAction = 'all';
		this.onSelect = Ext.emptyFn;
		this.editable = false;
		this.tpl = '<div id="' + this.treeId + '" style="overflow:hidden;height:' + this.maxHeight + 'px;"></div>';
		
		framework.widgets.form.ComboBoxTree.superclass.constructor.apply(this, arguments);
	},
	
	initComponent: function() {
		if (!this.valueField)
			this.valueField = "id";
		
		if (!this.displayField)
			this.displayField = "text";
		
		this.initTree();
        
        framework.widgets.form.ComboBoxTree.superclass.initComponent.call(this);
        
        if (this.allowBlank !== false) {
	        this.triggerConfig = {
	            tag:'span', cls:'x-form-twin-triggers', cn:[
	            {tag: "img", src: Ext.BLANK_IMAGE_URL, cls: "x-form-trigger " + this.trigger1Class},
	            {tag: "img", src: Ext.BLANK_IMAGE_URL, cls: "x-form-trigger " + this.trigger2Class}
	        ]};
        }
	},
	
	/**
	 * 初始化树
	 */
	initTree: function() {
		var combox = this;
		
		if (!(this.tree instanceof Ext.tree.TreePanel)) {
			Ext.apply(this.treeCfg, this.tree);
			
			// 数据源
			this.treeCfg.loader = new Ext.tree.TreeLoader(this.treeCfg.loader);
			this.treeCfg.loader.on("beforeload", function(loader, node) {
				loader.baseParams[loader.paramName || "nodeId"] = node.id;
			}, this);
			
			// 根节点
			this.treeCfg.root = new Ext.tree.AsyncTreeNode(this.treeCfg.root);
			
			this.tree = new Ext.tree.TreePanel(Ext.apply({
				height: this.maxHeight,
				border: false,
				autoScroll: true
			}, this.treeCfg));
			
		} else {
			this.tree.setHeight(this.maxHeight);
		}
		
		this.tree.on('click', this.onNodeClick, this);
	},
	
	initTrigger : function(){
		if (this.allowBlank !== false) {
	        var ts = this.trigger.select('.x-form-trigger', true);
	        this.wrap.setStyle('overflow', 'hidden');
	        var triggerField = this;
	        ts.each(function(t, all, index){
	            t.hide = function(){
	                var w = triggerField.wrap.getWidth();
	                this.dom.style.display = 'none';
	                triggerField.el.setWidth(w-triggerField.trigger.getWidth());
	            };
	            t.show = function(){
	                var w = triggerField.wrap.getWidth();
	                this.dom.style.display = '';
	                triggerField.el.setWidth(w-triggerField.trigger.getWidth());
	            };
	            var triggerIndex = 'Trigger'+(index+1);
	
	            if(this['hide'+triggerIndex]){
	                t.dom.style.display = 'none';
	            }
	            this.mon(t, 'click', this['on'+triggerIndex+'Click'], this, {preventDefault:true});
	            t.addClassOnOver('x-form-trigger-over');
	            t.addClassOnClick('x-form-trigger-click');
	        }, this);
	        this.triggers = ts.elements;
	        
		} else
			framework.widgets.form.ComboBoxTree.superclass.initTrigger.call(this);
    },
    
    onTrigger1Click : function(e) {
    	if (this.disabled) return;
		
		this.clearValue();
		this.triggers[0].hide();
    },
    
    onTrigger2Click : function(e) {
    	this.onTriggerClick(e);
    },
    
    onNodeClick: function(node) {
    	var combox = this;
    	
    	var isRoot = (node == this.tree.getRootNode());
        	var selectNodeModel = this.selectNodeModel;
        	var isLeaf = node.isLeaf();

    	if (isRoot && selectNodeModel != 'all') {
    		return;
    	} else if(selectNodeModel=='folder' && isLeaf) {
    		return;
    	} else if(selectNodeModel=='leaf' && !isLeaf) {
    		return;
    	}
    	
    	this.setValue(node);
    	this.collapse();
    },
    
    clearValue : function(){
        if (this.hiddenField)
            this.hiddenField.value = null;
            
        this.setRawValue(null);
        this.lastSelectionText = null;
        this.applyEmptyText();
        this.value = null;
    },
	
	expand : function(){
		framework.widgets.form.ComboBoxTree.superclass.expand.call(this);
		
		if (!this.tree.rendered) {
	        this.tree.render(this.treeId);
			
			var root = this.tree.getRootNode();
			if(!root.isLoaded())
				root.reload();
		}
    },
    
    getName: function() {
    	return this.rendered && this.name ? this.name : this.hiddenName;
    },
    
	setValue : function(node) {
		if (Ext.isEmpty(node)) {
			if (this.allowBlank !== false)
				this.triggers[0].hide();
			
			node = {};
		}
		
    	if (typeof node != "object") {
    		var data = {};
    		data[this.valueField] = node;
    		data[this.displayField] = node;
    		node = new Ext.data.Node(data);
    	}
		
    	if (!node.attributes)
    		node.attributes = {};

    	var value = node[this.valueField] || node.attributes[this.valueField] || node.id;
        var text = node[this.displayField] || node.attributes[this.displayField];

        this.lastSelectionText = text;
        
        if(this.hiddenField)
            this.hiddenField.value = value;
        
        Ext.form.ComboBox.superclass.setValue.call(this, text);
        
        this.value = value;
        
     	if (this.allowBlank !== false && !Ext.isEmpty(value, false))
     		this.triggers[0].show();
    },
    
    getValue : function() {
    	return typeof this.value != 'undefined' ? this.value : '';
    }
});

Ext.reg('combotree', framework.widgets.form.ComboBoxTree);