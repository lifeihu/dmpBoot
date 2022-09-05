_package('framework.widgets.tree');

framework.widgets.tree.TreeCheckNodeUI = Ext.extend(Ext.tree.TreeNodeUI, {
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
	
	renderElements : function(n, a, targetNode, bulkRender){
		var tree = n.getOwnerTree();
		this.checkModel = this.checkModel || tree.checkModel;
		this.onlyLeafCheckable = tree.onlyLeafCheckable || this.onlyLeafCheckable;
		
        // add some indent caching, this helps performance when rendering a large tree
        this.indentMarkup = n.parentNode ? n.parentNode.ui.getChildIndent() : '';

        var cb = (!this.onlyLeafCheckable || a.leaf);
		var href = a.href ? a.href : Ext.isGecko ? "" : "#";
        var buf = ['<li class="x-tree-node"><div ext:tree-node-id="',n.id,'" class="x-tree-node-el x-tree-node-leaf x-unselectable ', a.cls,'" unselectable="on">',
            '<span class="x-tree-node-indent">',this.indentMarkup,"</span>",
            '<img src="', this.emptyIcon, '" class="x-tree-ec-icon x-tree-elbow" />',
            '<img src="', a.icon || this.emptyIcon, '" class="x-tree-node-icon',(a.icon ? " x-tree-node-inline-icon" : ""),(a.iconCls ? " "+a.iconCls : ""),'" unselectable="on" />',
            cb ? ('<input class="x-tree-node-cb" type="checkbox" ' + (a.checked ? 'checked="checked" />' : '/>')) : '',
            '<a hidefocus="on" class="x-tree-node-anchor" href="',href,'" tabIndex="1" ',
             a.hrefTarget ? ' target="'+a.hrefTarget+'"' : "", '><span unselectable="on">',n.text,"</span></a></div>",
            '<ul class="x-tree-node-ct" style="display:none;"></ul>',
            "</li>"].join('');

        var nel;
        if(bulkRender !== true && n.nextSibling && (nel = n.nextSibling.ui.getEl())){
            this.wrap = Ext.DomHelper.insertHtml("beforeBegin", nel, buf);
        }else{
            this.wrap = Ext.DomHelper.insertHtml("beforeEnd", targetNode, buf);
        }
        
        this.elNode = this.wrap.childNodes[0];
        this.ctNode = this.wrap.childNodes[1];
        var cs = this.elNode.childNodes;
        this.indentNode = cs[0];
        this.ecNode = cs[1];
        this.iconNode = cs[2];
        var index = 3;
        if(cb){
            this.checkbox = cs[3];
			// fix for IE6
			// this.checkbox.defaultChecked = this.checkbox.checked;
            Ext.fly(this.checkbox).on('click', this.check.createDelegate(this,[null]));
            index++;
        }
        this.anchor = cs[index];
        this.textNode = cs[index].firstChild;
    },
    
    /**
     * @param {Boolean} checked
     */
    check : function(checked){
        var n = this.node;
		var tree = n.getOwnerTree();
		this.checkModel = this.checkModel || tree.checkModel;
		
		if( checked === null ) {
			checked = this.checkbox.checked;
		} else {
			this.checkbox.checked = checked;
		}
		
		n.attributes.checked = checked;
		tree.fireEvent('check', n, checked);

		if (this.checkModel == 'single') {
			var checkedNodes = tree.getChecked();
			for (var i = 0; i < checkedNodes.length; i++){
				var node = checkedNodes[i];
				if(node.id != n.id){
					node.getUI().checkbox.checked = false;
					node.attributes.checked = false;
					tree.fireEvent('check', node, false);
				}
			}
		} else if(!this.onlyLeafCheckable){
			if(this.checkModel == 'cascade' || this.checkModel == 'cascadeparent'){
				var parentNode = n.parentNode;
				if(parentNode !== null) {
					this.parentCheck(parentNode,checked);
				}
			}
			if(this.checkModel == 'cascade' || this.checkModel == 'cascadechild'){
				if( !n.expanded && !n.childrenRendered ) {
					n.expand(false,false,this.childCheck);
				}else {
					this.childCheck(n);  
				}
			}
		}
	},
	
	/**
	 * 选中/取消指定节点的所有子节点
	 * @param {Ext.tree.TreeNode} node
	 * @param {Boolean} checked 为空时根据指定节点的值
	 */
	childCheck : function(node, checked){
		var a = node.attributes;
		checked = checked || a.checked;
		
		if (!a.leaf) {
			var cs = node.childNodes;
			var csui;
			for (var i = 0; i < cs.length; i++) {
				csui = cs[i].getUI();
				if (csui.checkbox.checked ^ checked)
					csui.check(checked);
			}
		}
	},
	
	/**
	 * 选中/取消指定节点的所有父节点
	 * @param {Ext.data.TreeNode} node
	 * @param {Boolean} checked
	 */
	parentCheck : function(node, checked){
		var checkbox = node.getUI().checkbox;
		
		if (typeof checkbox == 'undefined') return ;
		if (!(checked ^ checkbox.checked)) return;
		if (!checked && this.childHasChecked(node)) return;
		
		checkbox.checked = checked;
		node.attributes.checked = checked;
		node.getOwnerTree().fireEvent('check', node, checked);
		
		var parentNode = node.parentNode;
		if (parentNode !== null)
			this.parentCheck(parentNode,checked);
	},
	
	/**
	 * 指定节点的所有节点中是否至少有一个被选中
	 * @param {Ext.tree.TreeNode} node
	 * @return {Boolean}
	 */
	childHasChecked : function(node){
		var childNodes = node.childNodes;
		if (childNodes || childNodes.length > 0) {
			for (var i = 0; i < childNodes.length; i++) {
				if (childNodes[i].getUI().checkbox.checked)
					return true;
			}
		}
		return false;
	},
	
    toggleCheck : function(value){
    	var cb = this.checkbox;
        if (cb) {
            var checked = (value === undefined ? !cb.checked : value);
            this.check(checked);
        }
    }
});