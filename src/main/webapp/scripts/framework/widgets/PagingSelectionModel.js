_package('framework.widgets');

framework.widgets.PagingSelectionModel = Ext.extend(Ext.grid.CheckboxSelectionModel, {
	/**
	 * 每页选中的记录
	 * @type 
	 */
	pagingSelections: null,
	
	constructor: function() {
		this.pagingSelections = {};
		framework.widgets.PagingSelectionModel.superclass.constructor.call(this);
	},
	
	initEvents: function() {
		framework.widgets.PagingSelectionModel.superclass.initEvents.call(this);
		
		var paging = this.grid.getBottomToolbar();
		paging.on('beforechange', this.onBeforePagingChange, this);
		paging.on('change', this.onPagingChange, this);
	},
	
	onBeforePagingChange: function(paging) {
		if (this.selections.getCount() > 0) {
			var pageData = paging.getPageData();
			this.pagingSelections[pageData.activePage] = this.selections.clone();
		}
	},
	
	onPagingChange: function(paging, pageData) {
		var keyColumn = this.grid.gridKeyColumn;
		var page = pageData.activePage;
		var selections = this.pagingSelections[page];
		if (selections && selections.getCount() > 0) {
			var store = this.grid.store, 
				index = -1, 
				removeSelections = [];
				
			selections.each(function(rec) {
				index = store.findExact(keyColumn, rec.get(keyColumn), 0);
				
				if (index != -1) 
					this.selectRow(index, true);
				else
					removeSelections.push(rec);
			}, this);
			
			Ext.iterate(removeSelections, function(rec) {
				selections.remove(rec);
			});
		}
	},
	
	/**
	 * 获得选中的所有记录
	 * @param {Boolean} current 是否获得当前页选中的记录
	 */
	getSelections: function(current) {
		if (current === true)
			return [].concat(this.selections.items);
		else {
			this.onBeforePagingChange(this.grid.getBottomToolbar());
			
			var selections = [];
			Ext.iterate(this.pagingSelections, function(page) {
				selections = selections.concat(this.pagingSelections[page].items);
			}, this);

			return selections;
		}
	},
	
	/**
	 * 获得选中的单条记录
	 * @param {Boolean} current 是否获得当前页选中的记录
	 */
	getSelected: function(current) {
		if (current === true) 
			return this.selections.itemAt(0);
		else {
			this.onBeforePagingChange(this.grid.getBottomToolbar());
			
			// 返回第一条被选中的记录
			var pageData = this.grid.getBottomToolbar().getPageData(), selections;
			for (var i = 1; i <= pageData.pages; i++) {
				selections = this.pagingSelections[i];
				if (selections && selections.getCount() > 0)
					return selections.itemAt(0);
			}
		}
	},
	
	destroy: function() {
		this.pagingSelections = null;
		framework.widgets.PagingSelectionModel.superclass.destroy.call(this);
	}
});