_package('framework.widgets.window');

_import([
	'framework.widgets.container.IframeModuleContainer',
	'framework.widgets.container.ModuleContainer'
]);

framework.widgets.window.ModuleWindow = Ext.extend(Ext.Window, {
	frame: true,
    border: true,
	modal: true,

    minWidth: 400,
    minHeight: 400,
    
    offsetSize: {width: 30, height: 30},
    
    /**
     * @cfg relayEvent
     * @type Array<String>
     * @description 需要从Module中传递至Window的事件集
     */
    relayEvent: null, 
    
    /**
     * @cfg allowAppendModuleButtons
     * @type Boolean
     * @description 允许添加Module中tbuttons属性定义的按钮
     */
    allowAppendModuleButtons: true,
    
    /**
     * 窗口中是否使用IFrame加载Module类
     * @type Boolean
     */
    useIframeContainer: false,
    
    /**
     * @cfg moduleContainerCfg
     * @type Object
     */
    moduleContainerCfg: null,
	
	initComponent: function() {
		var me = this,
			mcCfg = me.moduleContainerCfg || {},
			
			useIframeContainer = me.useIframeContainer,
			moduleContainerType = useIframeContainer ? 'moduleiframecontainer' : 'modulecontainer';
			
		if (!Ext.isEmpty(me.module, false)){
			if (Ext.isString(me.module))
				Ext.apply(mcCfg, {module: me.module});
			else
				Ext.apply(mcCfg, me.module);
		}
		
		if (!Ext.isEmpty(me.url, false))
			Ext.apply(mcCfg, {url: me.url});
		me.moduleContainerCfg = mcCfg;
		
		var tools = [{
			id: 'refresh',
			tooltip: '重载模块',
			handler: function() { me.redirect(); },
			scope: me
		}].concat(me.tools || []);
 		
		Ext.apply(me, {
			id: Ext.id(null, 'pf-module-window-'),
			
			tbar: [me.BUTTON_CLOSE = new Ext.Button({
				text: '关闭',
				iconCls: 'close',
				tooltip: '快捷键: Esc',
				handler: me.close,
				scope: me
			}), '-'].concat(me.tbar || []).concat(me.buttons || []),
			
			tools: [{
				id: 'refresh',
				tooltip: '重载模块',
				handler: function() { me.redirect(); },
				scope: me
			}].concat(me.tools || []),
			
			items: Ext.ComponentMgr.create(mcCfg, moduleContainerType),
			
			// 用户指定大小
			originSize: {
				w: me.width,
				h: me.height
			}
		});
		delete me.buttons;
		delete me.width;
		delete me.height;
		
		me.addEvents(
			/**
			 * @event beforemoduleload
			 * @param {framework.widgets.container.IframeModuleContainer} mc
			 * @param {Object} params
			 */
			'beforemoduleload',
		
			/**
			 * @event moduleload
			 * @param {framework.widgets.container.IframeModuleContainer/framework.widgets.container.ModuleContainer} mc
			 * @param {framework.core.Module} mdl
			 */
			'moduleload',
			
			/**
			 * @event beforebuttonrender
			 * @param {framework.widgets.window.ModuleWindow}
			 * @param {framework.core.Module}
			 */
			'beforebuttonrender',
			
			/**
			 * @event buttonrender
			 * @param {framework.widgets.window.ModuleWindow}
			 * @param {framework.core.Module}
			 */
			'buttonrender'
		);
		
		framework.widgets.window.ModuleWindow.superclass.initComponent.call(this);
		
		if (me.allowAppendModuleButtons) {
			me.on({
				moduleload: me.onModuleLoaded,
				scope: me
			});
		}
	},
	
	/**
	 * 添加Module中指定的按钮
	 * @param {framework.core.Module} mdl
	 */
	initModuleButtons: function(mdl) {
		var me = this,
			tbar = me.getTopToolbar(),
			
			buttons = [].concat(mdl.tbuttons || []);

		if (Ext.isEmpty(tbar) || buttons.length == 0) return;
		
		var loadMask = mdl.loadMask,
		
			count = tbar.items.getCount(),
			
			btnCfg, btnPos;
		
		if (me.fireEvent('beforebuttonrender', me, mdl) === false) return;

		loadMask.msg = '正在加载按钮,请耐心等候...';
		loadMask.show();
		
		if (me.haveModuleButtons === true) {
			tbar.items.each(function(item) {
				if (item.fromModule === true)
					item.destroy();
			});
			
			me.haveModuleButtons = false;
		}
		
		tbar.disable();
		Ext.iterate(buttons, function(btn) {
			btnCfg = btn;
			btnCfg.fromModule = true;
			
			if (Ext.isArray(btn)) {
				btnCfg = btn[0];
				btnCfg.fromModule = true;
				if (btn.length == 1) {
					tbar.add(tbar.lookupComponent(btnCfg));
					
				} else {
					btnPos = btn[1] + count;
				}
				
			} else {
				if (!Ext.isEmpty(btnCfg.position, false)) {
					btnPos = btnCfg.position + count;
					delete btnCfg.position;
				} else {
					tbar.add(tbar.lookupComponent(btnCfg));
				}
			}
			
			if (!Ext.isEmpty(btnPos, false)) {
				tbar.insertButton(btnPos, btnCfg);
			}
		});
		tbar.doLayout();
		tbar.enable();
		
		// 工具条中有来自Module的按钮
		me.haveModuleButtons = true;
		
		loadMask.hide();
		
		me.fireEvent('buttonrender', me, mdl);
	},
	
	/**
	 * @param {Object} moduleCfg
	 */
	redirect: function(moduleCfg) {
		var me = this,
			mc = me.getModuleContainer();
		
		me.relayEvents(mc, ['beforemoduleload', 'moduleload']);
			
		if (me.useIframeContainer) {
			mc.redirect.timer(function() {
				return !Ext.isEmpty(mc.iframeWindow);
			}, mc, [moduleCfg]);
			
		} else {
			mc.loadModule(Ext.apply(me.moduleContainerCfg || {}, moduleCfg));
		}
	},
	
	getModule: function() {
		var mc = this.getModuleContainer();
		return mc == null ? null : mc.getModule();
	},
	
	getModuleContainer: function() {
		return this.getComponent(0);
	},
	
	/**
	 * 获得工具条中的指定按键
	 * @param {String} button
	 */
	getTBButton: function(button) {
		var tbar = this.getTopToolbar();
		return Ext.isEmpty(tbar) ? null : tbar.getComponent(button);
	},
	
	/**
	 * 获得调整后的宽高度
	 * @param {Object} size
	 */
	getAdjustSize: function(size) {
		var me = this,
			ct = me.container || Ext.getBody(),
			
			osize = me.originSize,
			ow = osize.w,
			oh = osize.h,
		
			csize = Ext.isEmpty(size) ? ct.getViewSize(true) : size,
			cw = csize.width,
			ch = csize.height,
			
			mw = me.minWidth,
			mh = me.minHeight,
			
			fsize = me.offsetSize;

		return {
			width: !Ext.isEmpty(ow, false) ? ow : Math.max(cw - fsize.width, mw), 
			height: !Ext.isEmpty(oh, false) ? oh : Math.max(ch - fsize.height, mh)
		};
	},
	
	/**
	 * 打开窗口
	 * @param {} params
	 */
	open: function(params) {
		this.openParams = params;
		this.show();
	},
	
	doWindowResize: function() {
		var me = this,
			ct = me.container || Ext.getBody(),
			
			size = me.getAdjustSize();

		// fixed Ext bug.
		if (me.modal)
			me.mask.setSize(ct.getViewSize(true));
			
		me.setSize(size);
		me.center();

		if (ct.isScrollable()) {
			var osize = me.offsetSize;
			me.alignTo(ct, 'c-c?', Ext.isIE ? null : [osize.width * -1, osize.height * -1]);
		}
	},
	
	/**
	 * 创建窗口
	 * @param {Object} winCfg
	 * @param {String} winClazz
	 */
	createWindow: function(winCfg, winClazz) {
		// var framework = top.window.Ext.get('centeriframe').dom.contentWindow.framework;
		return framework.createWindow(winCfg, winClazz || 'framework.widgets.window.ModuleWindow');
	},
	
	//////////////////////////////////
	
	onWindowResize: function() {
		framework.widgets.window.ModuleWindow.superclass.onWindowResize.apply(this, arguments);
		
		this.doWindowResize();
	},
	
	onShow: function() {
		framework.widgets.window.ModuleWindow.superclass.onShow.call(this);
		
		this.doWindowResize();
		
		var openParams = this.openParams;
		delete this.openParams;

		this.redirect(openParams);
	},
	
	onResize: function() {
		framework.widgets.window.ModuleWindow.superclass.onResize.apply(this, arguments);
		
		var mc = this.getModuleContainer(),
			activeModule = mc.getModule();
		
		if (activeModule) 
			mc.doLayout();
	},
	
	onModuleLoaded: function(mc, mdl) {
		var me = this,
			keyMap = mdl.getKeyMap();
			
		mdl.moduleWindow = me;
		
		me.relayEvents(mdl, [].concat(me.relayEvent || []));
		
		keyMap.addBinding({
			key: 27,
			fn: me.close,
			scope: me
		});
		
		me.initModuleButtons(mdl);
	},
	
	beforeDestroy: function() {
		var me = this;

		if (me.iframeEl) {
			me.iframeEl.removeAllListeners();
			me.iframeEl = null;
			me.iframeWindow = null;
		}
		
		framework.widgets.window.ModuleWindow.superclass.beforeDestroy.call(this);
	}
});