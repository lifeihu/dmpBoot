_package('framework.widgets.plugins');

framework.widgets.plugins.FormPaginationPlugin = Ext.extend(Ext.util.Observable, {
	
	/**
	 * @requires
	 * @cfg store
	 * @type Ext.data.Store
	 */
	
	/**
	 * @property form
	 * @type Ext.form.FormPanel
	 */
	
	init: function(form) {
		var plg = this;
		
		plg.form = form;
		plg.store = form.store;

		form.addEvents(
			/**
			 * @event beforechange
			 * @param Ext.PagingToolbar paging
			 * @param Object params
			 */
			'beforechange',
			
			/**
			 * @event change
			 * @param Ext.PagingToolbar paging
			 * @param Object pageData
			 */
			'change'
		)

		plg.bindStore(plg.store, true);
		plg.initPaginationToolbar();
		
		plg.form.getFormPaginationPlugin = plg.getFormPaginationPlugin.createDelegate(plg);
	},
	
	/**
	 * 初始化分页工具条
	 */
	initPaginationToolbar: function() {
		var plg = this,
			form = plg.form,
			bbar = form.getBottomToolbar(),
			
			paging = Ext.ComponentMgr.create({
				pageSize: 1,
				store: plg.store
			}, 'paging');
			
		if (Ext.isEmpty(bbar)) {
			form.elements += ',bbar';
			form.bottomToolbar = paging;
		} else {
			bbar.add(paging);
		}
		
		form.relayEvents(paging, ['beforechange', 'change']);

		plg.paging = paging;
	},
	
	/**
	 * 绑定数据源
	 * @param {Ext.data.Store} store
	 * @param {Boolean} initial 是否重新绑定
	 */
	bindStore: function(store, initial) {
		var plg = this;
		
		if (plg.store && !initial) {
			plg.store.un('load', plg.onLoad, plg);
		
			if (Ext.isEmpty(store)) {
				plg.store = null;
			}
		}
		
		if (!Ext.isEmpty(store)) {
			store = Ext.StoreMgr.lookup(store);
			
			// 每天只显示一条记录
			store.baseParams.limit = 1;
			
			store.on('load', plg.onLoad, plg);
		}
	},
	
	/**
	 * 获得表单分页插件
	 * @return {framework.widgets.plugins.FormPaginationPlugin}
	 */
	getFormPaginationPlugin: function() {
		return this;
	},
	
	///////////////////////////////////////
	
	onLoad: function(store, records, options) {
		if (records.length == 0) return;

		var plg = this,
			form = plg.form.form,
			paging = plg.paging,
			
			start = 0,
			params = options.params;
			
		if (params && records.length > 1)
			start = params.start == null ? 0 : params.start;

		form.loadRecord(records[start]);
	}
});

Ext.preg('formpaginationplugin', framework.widgets.plugins.FormPaginationPlugin);