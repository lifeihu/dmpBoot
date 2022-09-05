_package('framework.widgets.tree');

framework.widgets.tree.TreePagingToolbar = Ext.extend(Ext.Toolbar, {
	autoCreate: {
		style: Ext.isIE 
			? 'display: inline; white-space:nowrap; vertical-align:middle;'
			: ''
	},
	
	/**
	 * @cfg node
	 * @type Ext.tree.TreeNode
	 */
	node: null,
	
	firstText: Ext.PagingToolbar.prototype.firstText,
    prevText: Ext.PagingToolbar.prototype.prevText,
    nextText: Ext.PagingToolbar.prototype.nextText,
    lastText: Ext.PagingToolbar.prototype.lastText,
    afterPageText: '/{0}',
	
	initComponent: function() {
		var pagingItems = [this.first = new Ext.Toolbar.Button({
			scale: 'little',
			tooltip: this.firstText,
            overflowText: this.firstText,
            iconCls: 'x-tbar-page-first',
            disabled: true,
	        handler: this.onClick.createDelegate(this, ["first"]),
	        scope: this
        }), this.prev = new Ext.Toolbar.Button({
			scale: 'little',
            tooltip: this.prevText,
            overflowText: this.prevText,
            iconCls: 'x-tbar-page-prev',
            cls: 'x-tbar-page-small',
            disabled: true,
	        handler: this.onClick.createDelegate(this, ["prev"]),
	        scope: this
        }), this.inputItem = new Ext.form.NumberField({
            cls: 'x-tbar-page-number',
            width: 25,
            height: 14,
            style: 'text-align:center;',
            allowDecimals: false,
            allowNegative: false,
            enableKeyEvents: true,
            selectOnFocus: true,
            submitValue: false,
            listeners: {
                scope: this,
                keydown: this.onPagingKeyDown,
                blur: this.onPagingBlur
            }
        }), this.afterTextItem = new Ext.Toolbar.TextItem({
            text: String.format(this.afterPageText, 1)
        }), this.next = new Ext.Toolbar.Button({
        	scale: 'little',
            tooltip: this.nextText,
            overflowText: this.nextText,
            iconCls: 'x-tbar-page-next',
            disabled: true,
            handler: this.onClick.createDelegate(this, ["next"]),
            scope: this
        }), this.last = new Ext.Toolbar.Button({
        	scale: 'little',
            tooltip: this.lastText,
            overflowText: this.lastText,
            iconCls: 'x-tbar-page-last',
            disabled: true,
            handler: this.onClick.createDelegate(this, ["last"]),
            scope: this
        })/*, this.refresh = new Ext.Toolbar.Button({
        	scale: 'little',
            tooltip: this.refreshText,
            overflowText: this.refreshText,
            iconCls: 'x-tbar-loading',
            handler: this.onClick,
            scope: this
        })*/];
        
        this.items = pagingItems;
        
        framework.widgets.tree.TreePagingToolbar.superclass.initComponent.call(this);
        
        this.on({
        	afterrender: this.updateInfo,
        	scope: this
        })
	},
	
	getTreeNode: function() {
		if (Ext.isEmpty(this.node)) {
			return null
		}
		
		return this.node.ownerTree.getNodeById(this.node.id);
	},
	
	doLoad: function(current) {
		var node = this.getTreeNode(),
			pi = node.attributes.pageInfo;
			
		pi.start = current;

		this.updateInfo();
		node.reload();
	},
	
	updateInfo : function(){
        var pi = this.getTreeNode().attributes.pageInfo,
        
        	start = pi.start,
			limit = pi.limit,
        	total = pi.total,
        	
        	fp = start == 0,
        	nl = (start + limit) >= total,
        
        	d = this.getPageData(), 
        	ap = d.activePage,
        	ps = d.pages;
        	
		this.afterTextItem.setText(String.format(this.afterPageText, d.pages));
		this.inputItem.setValue(ap);

		this.first.setDisabled(fp);
        this.prev.setDisabled(fp);
        this.next.setDisabled(nl);
        this.last.setDisabled(nl);
    },
    
    getPageData : function(){
		var pi = this.getTreeNode().attributes.pageInfo,
		
			start = pi.start,
			limit = pi.limit,
        	total = pi.total;
        	
        return {
            total: total,
            activePage: Math.ceil((start + limit) / limit),
            pages:  total < limit ? 1 : Math.ceil(total / limit)
        };
    },
	
    readPage : function(d){
        var v = this.inputItem.getValue(), pageNum;
        if (!v || isNaN(pageNum = parseInt(v, 10))) {
            this.inputItem.setValue(d.activePage);
            return false;
        }
        return pageNum;
    },
    
    ////////////////////////////////////////////////////////////////////////////////
	
	onClick: function(action) {
		var node = this.getTreeNode(),
			pi = node.attributes.pageInfo,
			
			current = 0,
			start = pi.start,
			limit = pi.limit,
			total = pi.total;
		
		switch (action) {
			case 'first':
				current = 0;
			break;
			case 'prev':
				current = Math.max(start - limit, 0);
			break;
			case 'next':
				current = Math.min(start + limit, total);
			break;
			case "last":
                var extra = total % limit;
				current = extra ? (total - extra) : (total - limit);
            break;
		}
		
		this.doLoad(current);
	},
    
	onPagingKeyDown: function(field, e) {
		var k = e.getKey(), 
			d = this.getPageData(), 
			pageNum;
			
        if (k == e.RETURN) {
            e.stopEvent();
            pageNum = this.readPage(d);
            if(pageNum !== false){
                pageNum = Math.min(Math.max(1, pageNum), d.pages) - 1;
                
                var node = this.getTreeNode(),
                	pi = node.attributes.pageInfo;
                	
                this.doLoad(pageNum * pi.limit);
                this.inputItem.getEl().dom.select();
            }
        }else if (k == e.HOME || k == e.END){
            e.stopEvent();
            pageNum = k == e.HOME ? 1 : d.pages;
            field.setValue(pageNum);
        }else if (k == e.UP || k == e.PAGEUP || k == e.DOWN || k == e.PAGEDOWN){
            e.stopEvent();
            if((pageNum = this.readPage(d))){
                var increment = e.shiftKey ? 10 : 1;
                if(k == e.DOWN || k == e.PAGEDOWN){
                    increment *= -1;
                }
                pageNum += increment;
                if(pageNum >= 1 & pageNum <= d.pages){
                    field.setValue(pageNum);
                }
            }
        }
	},
	
	onPagingBlur: function() {
		this.inputItem.setValue(this.getPageData().activePage);
	}
	
});