_package('framework.widgets.plugins');

_import([
	'framework.widgets.tree.TreePagingToolbar'
]);

framework.widgets.plugins.TreePagingPlugin = Ext.extend(Ext.util.Observable, {
	
	/**
	 * @cfg pagingModel
	 * @type String
	 * @description 分页模式(local,remote)
	 */
	pagingModel: 'local',
	
	/**
	 * @cfg childrenRoot
	 * @type String
	 * @description 子节点Root
	 */
	childrenRoot: 'paginationResults',
	
	/**
	 * @cfg pageSize
	 * @type Number
	 */
	pageSize: 20,
	
	/**
	 * @cfg alwaysShowPagingToolbar
	 * @type Boolean
	 * @description 是否一直显示节点分页工具条
	 */
	alwaysShowPagingToolbar: false,
	
	constructor: function(config) {
		Ext.apply(this, config);
		
		framework.widgets.plugins.TreePagingPlugin.superclass.constructor.apply(this, arguments);
	},
	
	init: function(tree) {
		var me = this;
		me.tree = tree;
		
		// 重载TreeLoader的doPreload和processResponse方法
		var loader = tree.loader;
		loader.doPreload = me.doPreload.createDelegate(me);
		loader.processResponse = me.processResponse.createDelegate(me);
		
		tree.on({
			append: function(tree, parent, node) {
				me.rebindNodeOver(node);
			},
			insert: function(tree, parent, node) {
				me.rebindNodeOver(node);
			},
			scope: me
		});
	},
	
	/**
	 * 重新绑定Node的mouseover事件
	 * @param {Ext.tree.TreeNode} node
	 */
	rebindNodeOver: function(node) {
		if (this.alwaysShowPagingToolbar === true) return;
		
		var ui = node.getUI();
		if (ui.isBindingOverEvent !== true) {
			ui.onOver = this.onTreeNodeOver.createDelegate(this, [node], 0);
			ui.isBindingOverEvent = true;
    	}
	},
	
	doPreload: function(node) {
		var me = this,
			loader = me.tree.loader,
		
			attrs = node.attributes,
			pi = attrs.pageInfo;

		if (!Ext.isDefined(pi)) {
			attrs.pageInfo = pi = {
				start: 0,
				limit: attrs.pageSize || me.pageSize
			};
		}
		
		if (me.pagingModel == 'local') {
			var children = attrs.children;
			if (children) {
				if (node.childNodes.length < 1) {
					var start = pi.start,
						limit = pi.limit,
						total = children.length,
						end = Math.min(start + limit, total);
						
					pi.total = total;
					
					node.beginUpdate();
					for (; start < end; start++) {
						var cn = node.appendChild(loader.createNode(children[start]));
	                    if(loader.preloadChildren){
	                        loader.doPreload(cn);
	                    }
					}
					node.endUpdate();
					
					if (limit < total) {
            			me.initPagingToolbar(node);
					}
				}
				
				return true;
			}
		}
		
		Ext.apply(loader.baseParams, pi);
		
		return false;
    },
    
    processResponse : function(response, node, callback, scope){
        var me = this,
        	loader = me.tree.loader,
        
        	attrs = node.attributes,
        	pi = attrs.pageInfo,
        	
        	json = response.responseText;
        	
        try {
            var start = 0, end = 0,
            	o = response.responseData || Ext.decode(json);
            
            if (Ext.isArray(o)) {
            	pi.total = o.length;
            	
            	start = pi.start;
            	end = Math.min(start + pi.limit, pi.total);
            	
            } else {
            	var children = o[me.childrenRoot];
            	pi.total = o.total || children.length;
            	o = children;
            	
            	start = 0;
            	end = o.length;
            }
            
            node.beginUpdate();
            for (; start < end; start++) {
                var n = loader.createNode(o[start]);
                if(n){
                    node.appendChild(n);
                }
            }
            node.endUpdate();
            
            if (end < pi.total) {
            	me.initPagingToolbar(node);
			}
            
            loader.runCallback(callback, scope || node, [node]);
        }catch(e){
            loader.handleFailure(response);
        }
    },
    
    initPagingToolbar: function(node) {
    	var me = this,
    		attrs = node.attributes,
    		
    		ui = node.getUI(),
    		uiEl = ui.getEl(),
    		
    		pbar = attrs.pbar,
    		showOnTop = !node.ownerTree.rootVisible && node.isRoot;

    	if (!Ext.isDefined(pbar)) {
    		attrs.pbar = pbar = new framework.widgets.tree.TreePagingToolbar({
    			node: node
    		});
    		
    		if (!showOnTop) {
				uiEl = Ext.get(uiEl.firstChild);
				uiEl = Ext.DomHelper.append(uiEl, {
					tag: 'div',
					style: 'float: right; display: inline; white-space:nowrap;'
				}, true);
			}
    		
    		pbar.render(uiEl);
    		
    		var els = Ext.fly(ui.getEl()).select('a span');
    		if (els.getCount() > 0) {
    			pbar.getEl().alignTo(els.first(), 'l-r');
    		}
    		pbar.getEl().slideIn('t');
    	}
    	
    	me.pbar = showOnTop ? me.pbar : pbar;
    	me.pbar.updateInfo();
    },
    
    ///////////////////////////////////////////////////////////////////////////////////////
    
    onTreeNodeOver: function(node, e) {
    	Ext.tree.TreeNodeUI.prototype.onOver.call(node.getUI(), e);

    	var me = this,
    		pbar = node.attributes.pbar;
    		
    	if (me.pbar !== pbar) {
			if (me.pbar) {
				me.pbar.getEl().ghost('t', {
					remove: false, 
					stopFx: true
				});
				me.pbar = null;
			}
			
			if (pbar){
				me.pbar = pbar;
				
				me.pbar.getEl().slideIn('t', {stopFx: true});
			}
		}
    }
	
});

Ext.preg('treepagingplugin', framework.widgets.plugins.TreePagingPlugin);