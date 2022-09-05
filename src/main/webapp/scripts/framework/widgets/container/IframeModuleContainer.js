_package('framework.widgets.container');

framework.widgets.container.IframeModuleContainer = Ext.extend(framework.core.Module, {
	autoLoadData: false,
	frame: false,
	border: false,
	style: 'padding:0px;',
	
	/**
	 * @private
	 * @property activeModule
	 * @type Module
	 */
	activeModule: null,
	
	initModule: function() {
		var mdl = this;
		
		mdl.id = Ext.id(null, 'pf-iframe-module-container-');
		mdl.iframeId = Ext.id(null, 'pf-iframe-');
		
		mdl.addEvents(
		
			/**
			 * @event beforemoduleload
			 * @param {framework.widgets.container.IframeModuleContainer} mc
			 * @param {Object} params
			 */
			'beforemoduleload',
		
			/**
			 * @event moduleload
			 * @param {framework.widgets.container.IframeModuleContainer} mic
			 * @param {framework.core.Module} mdl
			 */
			'moduleload'
		);
		
		framework.widgets.container.IframeModuleContainer.superclass.initModule.call(this);
	},
	
	center: function() {
		return {
			frame: false,
			border: false,
			margins: '0',
			html: '<iframe id="' + this.iframeId + '" width="100%" height="100%" frameborder="0" scrolling="auto"></iframe>'
		};
	},
	
	redirect: function(params) {
		params = params || {};

		if (Ext.isString(params)) {
			params = {url: params};
		}
		
		var mdl = this,
			loadMask = mdl.loadMask;
		
		params.url = params.url || mdl.url;
		if (Ext.isEmpty(params.url, false)) return;
			
		loadMask.msg = '正在加载页面,请耐心等候...';
		loadMask.show.defer(10, loadMask);
		
		if (mdl.fireEvent('beforemoduleload', mdl, params) === false) return;

		// 再次获得URL，因为这个URL可能在beforemoduleload事件中被更改
		var url = params.url,
			urlParams = url.urlParams(),
			params = Ext.apply(urlParams || {}, params),
			forwardParam = params.forward,
			
			pos = url.indexOf('?'),
			newUrl = (pos == -1 ? url : url.substring(0, pos)) + '?';
		
		if (!Ext.isEmpty(forwardParam, false)) {
			newUrl += 'forward=' + forwardParam;
			delete params.forward;
		}

		if (!Ext.isEmpty(params.pathTitle)) {
			mdl.path(params.pathTitle);
			delete params.pathTitle;
		}

		mdl.iframeWindow.location.href = framework.getUrl(newUrl, params);
	},
	
	getModule: function() {
		return this.activeModule;
	},
	
	path: function(path, overwrite) {
		if (Ext.isEmpty(path, false)) return;
		
		overwrite = overwrite == null ? false : overwrite;
		if (!overwrite) path = ' - ' + path;

		nt.path(path, overwrite);
	},
	
	doModuleLayout: function() {
		var mdl = this,
		
			ct = mdl.container,
			module = mdl.getModule();

		mdl.setSize(ct.getViewSize(true));
			
		if (!Ext.isEmpty(module)) {
			var bodySize = mdl.iframeWindow.Ext.getBody().getViewSize(true);
			module.setSize(bodySize);
		}
	},
	
	///////////////////////////////////////////////
	
	onModuleRender: function(mdl) {
		mdl.iframeEl = Ext.get(mdl.iframeId);
		mdl.iframeEl.on('load', mdl.onIframeLoad, mdl);
		mdl.iframeWindow = mdl.iframeEl.dom.contentWindow;
		
		framework.widgets.container.IframeModuleContainer.superclass.onModuleRender.call(this, mdl);
	},
	
	onIframeLoad: function() {
		var mdl = this,
			iframeFramework = mdl.iframeWindow.framework,
			
			activeModule = null;
			
		(function() {
			mdl.debug('iframe module ' + iframeFramework.getActiveModule().moduleId + ' rendered.');
			
			// activeModule.register();
			
			mdl.activeModule = activeModule;
			mdl.doModuleLayout();
			mdl.loadMask.hide();
			
			mdl.fireEvent('moduleload', mdl, activeModule);
			
		}).timer(function() {
			activeModule = iframeFramework.getActiveModule();
			return Ext.isObject(activeModule) && activeModule.allComponentsRendered === true;
		}, mdl);
	}
});

Ext.reg('moduleiframecontainer', framework.widgets.container.IframeModuleContainer);