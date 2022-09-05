_package('framework.widgets.container');

_import('framework.core.Module');

framework.widgets.container.ModuleContainer = Ext.extend(framework.core.Module, {
	border: false,
	
	/**
	 * @optional
	 * @cfg autoModule
	 * @type Object/String
	 * @description 需要自动加载的模块参数
	 */
	
	initModule: function() {
		var mdl = this;
		mdl.id = Ext.id(null, 'pf-modulecontainer-');
		
		mdl.addEvents(
			
			/**
			 * @event beforemoduleload
			 * @param {framework.widgets.container.ModuleContainer} mc
			 * @param {Object} params
			 */
			'beforemoduleload',
		
			/**
			 * @event moduleload
			 * @param {framework.widgets.container.ModuleContainer} mc
			 * @param {framework.core.Module} mdl
			 */
			'moduleload'
		);
		
		framework.widgets.container.ModuleContainer.superclass.initModule.call(this);
	},
	
	center: function() {
		return {
			id: Ext.id(null, 'pf-modulecontainer-bwrap-'),
			frame: false,
			border: false,
			autoScroll: false
		};
	},
	
	/**
	 * 加载指定Module
	 * @param {Object} module
	 * 		{String} module 需要加载的Module
	 * @param {Function} fn
	 * 		{framework.widgets.container.ModuleContainer} mc
	 * 		{framework.core.Module} mdl
	 * 		{Boolean} fromCache 
	 * @param {Object} scope
	 */
	loadModule: function(module, fn, scope) {
		if (Ext.isEmpty(module, false)) return;
		
		if (Ext.isString(module))
			module = {module: module};
			
		var mdl = this,
			loadMask = mdl.loadMask,
			
			clazz = module.module;
			
		if (Ext.isEmpty(clazz, false)) return;
		
		if (mdl.fireEvent('beforemoduleload', mdl, module) === false) return;
		
		loadMask.show();

		(function() {
			_import(clazz, function() {
				mdl.onModuleLoaded(module, fn, scope);
			});
			loadMask.hide();
		}).defer(1);
	},
	
	addModule: function(module) {
		if (Ext.isEmpty(module)) return;
		
		var mdl = this;
		
		module.moduleContainer = mdl;

		mdl.removeModule();
		mdl.centerPnl.add(module);
		mdl.centerPnl.doLayout();
		
		mdl.activeModule = module;
		mdl.doLayout();
	},
	
	removeModule: function() {
		var mdl = this,
			activeModule = mdl.activeModule;
		
		if (activeModule) {
			mdl.centerPnl.remove(activeModule, true);
			// activeModule.unregister();
			mdl.activeModule = activeModule = null;
		}
	},
	
	getModule: function() {
		return this.activeModule;
	},
	
	doModuleLayout: function() {
		var mdl = this,
			ct = mdl.container,
			
			activeModule = mdl.activeModule;
		
		mdl.setSize(ct.getViewSize(true));

		if (activeModule) {
			activeModule.setSize(mdl.body.getViewSize(true));
		}
		
		framework.widgets.container.ModuleContainer.superclass.doModuleLayout.call(this);
	},
	
	onModuleRender: function(mdl) {
		mdl.loadMask.msg = '正在加载模块,请耐心等候...';
		
		// 自动加载Module
		mdl.loadModule(mdl.autoModule);
		
		framework.widgets.container.ModuleContainer.superclass.onModuleRender.call(this, mdl);
	},
	
	onModuleLoaded: function(mdlCfg, callback, scope) {
		if (Ext.isEmpty(mdlCfg, false)) return;
		
		var mdl = this,
			module = null,
			clazz = eval(mdlCfg.module);
	
		module = new clazz(mdlCfg);
		module.region = 'center';
		module.moduleContainer = mdl;
		// module.register();
		
		module.on('modulerender', function() {
			mdl.fireEvent('moduleload', mdl, module);
		}, {single: true});
		
		mdl.addModule(module);
		
		if (!Ext.isEmpty(callback))
			Ext.callback(callback, scope || mdl, [mdl, module]);
	}
});

Ext.reg('modulecontainer', framework.widgets.container.ModuleContainer);