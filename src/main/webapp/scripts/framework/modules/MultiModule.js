_package('framework.modules');

framework.modules.MultiModule = Ext.extend(framework.core.Module, {
	border: false,
	autoLoadData: false,
	
	/**
	 * 在多个列表时默认激活的列表
	 * @type Number
	 */
	activeTab: 0,
	
	initModule: function() {
		var mdl = this;
		
		mdl.addEvents(
			/**
             * @event beforetabchange
             * @param {TabPanel} tp
             * @param {Panel} newTab The tab being activated
             * @param {Panel} currentTab The current active tab
             */
            'beforetabchange',
            
            /**
             * @event tabchange
             * @param {TabPanel} tp
             * @param {Panel} tab The new active tab
             */
            'tabchange'
		);
		
		framework.modules.MultiModule.superclass.initModule.call(this);
	},
	
	center: function() {
		var mdl = this,
		
			tabPnl = new Ext.TabPanel({
				frame: false,
				border: true
			});
			
		mdl.relayEvents(tabPnl, ['beforetabchange', 'tabchange']);
		
		/**
		 * @method getActiveTab
		 * @see Ext.TabPanel.getActiveTab
		 */
		mdl.getActiveTab = tabPnl.getActiveTab.createDelegate(tabPnl);
		
		/**
		 * @method setActiveTab
		 * @see Ext.TabPanel.setActiveTab
		 */
		mdl.setActiveTab = tabPnl.setActiveTab.createDelegate(tabPnl);
		
		return tabPnl;
	},
	
	loadData: function() {
		var mdl = this,
			item = mdl.getActiveTab();
		
		if (!Ext.isEmpty(item.loadData)) {
			item.loadData();
		}
		/*if (item instanceof framework.core.Module)
			item.loadData();*/
	},
	
	doModuleLayout: function() {
		var mdl = this,
			ct = mdl.container;

		mdl.setHeight(ct.getSize(true).height);
	},
	
	afterRender: function() {
		framework.modules.MultiModule.superclass.afterRender.call(this);
	
		var mdl = this,
			tabPnl = mdl.centerPnl,
			modules = mdl.moduler() || [];
			
		Ext.each(modules, function(module) {
			tabPnl.add(Ext.apply(module, {
				renderTo: null,
				multiModule: mdl,
				autoScroll: false
			}));
		});
		mdl.setActiveTab(Number(Ext.isEmpty(mdl.activeTab, false) ? 0 : mdl.activeTab));
	},
	
	moduler: Ext.emptyFn
});

Ext.reg('multimodule', framework.modules.MultiModule);