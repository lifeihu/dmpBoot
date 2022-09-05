_package('framework.widgets.tree');

framework.widgets.tree.PagingTreeLoader = Ext.extend(Ext.tree.TreeLoader, {

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
	
	doPreload: function(node) {
		var me = this,
		
			attrs = node.attributes,
			pi = attrs.pageInfo;
			
		if (!Ext.isDefined(pi)) {
			attrs.pageInfo = pi = {
				start: 0,
				limit: me.pageSize
			};
		}
		
		if (me.pagingModel == 'local') {
			var children = attrs.children;
			if (!Ext.isEmpty(children)) {
				if (node.childNodes.length < 1) {
					var start = pi.start,
						limit = pi.limit,
						total = children.length,
						end = Math.min(start + limit, total);
						
					pi.total = total;
					
					node.beginUpdate();
					for (; start < end; start++) {
						var cn = node.appendChild(me.createNode(children[start]));
	                    if(me.preloadChildren){
	                        me.doPreload(cn);
	                    }
					}
					node.endUpdate();
					
					if (limit < total) {
						me.createToolbar(node);
					}
				}
				
				return true;
			}
		}
		
		Ext.apply(me.baseParams, pi);
		
		return false;
    },
    
    processResponse : function(response, node, callback, scope){
        var attrs = node.attributes,
        	pi = attrs.pageInfo,
        	
        	json = response.responseText;
        	
        try {
            var o = response.responseData || Ext.decode(json);
            
            if (Ext.isArray(o)) {
            	pi.total = o.length;
            } else {
            	var children = o[this.childrenRoot];
            	pi.total = o.total || children.length;
            	o = children;
            }
            
            if (this.pagingModel == 'local') {
            	node.attributes.children = o;
            }
            
            node.beginUpdate();
            for(var i = 0, len = o.length; i < len && i < pi.limit; i++){
                var n = this.createNode(o[i]);
                if(n){
                    node.appendChild(n);
                }
            }
            node.endUpdate();
            
            if (pi.limit < pi.total) {
				this.createToolbar(node);
			}
            
            this.runCallback(callback, scope || node, [node]);
        }catch(e){
            this.handleFailure(response);
        }
    }
	
});