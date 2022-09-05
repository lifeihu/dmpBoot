_package('framework.core');

_import([
	'framework.core.Parameter',
	'framework.widgets.plugins.ModuleValidatePlugin'
]);

framework.core.Module = Ext.extend(Ext.Panel, {
	frame: false,
	border: false,
	layout: 'border',
	style: 'padding:1px;',
	
	/**
	 * @cfg minHeight
	 * @type Number
	 */
	
	/**
	 * @cfg autoLoadData
	 * @type Boolean
	 * @description 是否自动加载应用模块数据(以前版本是autoLoad,仍可用)
	 */
	autoLoadData: true,
	
	/**
	 * @cfg allowLoadData
	 * @type Boolean
	 * @description 是否允许开始加载数据(以前版本是inited,仍可用)
	 */
	allowLoadData: true,
	
	/**
	 * @cfg monitorValidate
	 * @type Boolean
	 * @description 是否监听校验
	 */
	
	/**
	 * @property loadMask
	 * @type Ext.LoadMask
	 */
	
	/**
	 * @property modulePnl
	 * @type Ext.Panel
	 */
	
	/**
	 * @property moduleLayout
	 * @type String
	 * @description Module使用的布局(border,container)
	 */
	
	/**
	 * @cfg defaultAction
	 * @type Object
	 * 		@property fn
	 * 		@property scope
	 * @description 按Shift-Enter后执行的默认动作
	 */
	
	parameter: new framework.core.Parameter(),
	
	initComponent: function() {
		var mdl = this;
		
		mdl.id = Ext.id(mdl, 'pf-module-');
		
		// 初始化Session数据
		mdl.initSessionData();
		
		// 初始化布局
		mdl.initLayout();

		//////////////////////////////////////
		
		// 检验插件
		if (mdl.monitorValidate === true) {
			mdl.plugins = [].concat(mdl.plugins || []).concat(Ext.ComponentMgr.createPlugin({ptype: 'modulevalidateplugin'}));
		}

		//////////////////////////////////////
			
		// 兼容以前版本
		if (!Ext.isEmpty(mdl.autoLoad, false)) {
			mdl.autoLoadData = mdl.autoLoad;
			delete mdl.autoLoad;
		}
		
		//////////////////////////////////////
		
		/*mdl.keys = {
			key: [10, 13],
			shift: true,
			fn: mdl.loadData,
			scope: mdl
		};*/
		
		//////////////////////////////////////
		
		mdl.addEvents(
			/**
			 * @event modulerender
			 * @param {framework.core.Module} mdl
			 */
			'modulerender',
			
			/**
			 * @event moduleresize
			 * @param Number width
			 * @param Number height
			 */
			'moduleresize'
		);
		
		mdl.initModule();

		// buttons属性可以用fbar代替,buttons使用tbuttons代替,框架中对tbuttons有特殊处理
		if (!Ext.isEmpty(mdl.buttons)) {
			mdl.tbuttons = mdl.buttons;
			delete mdl.buttons;
		}
		
		framework.core.Module.superclass.initComponent.call(this);
		
		Ext.EventManager.onWindowResize(this.onWindowResize, this, {delay: 1});
		
		mdl.on({
			modulerender: mdl.onComponentsRendered,
			moduleresize: mdl.onModuleResize,
			
			scope: mdl
		});
	},
	
	/**
	 * 初始化Session数据
	 */
	initSessionData: function() {
		var p = this.parameter;
		
		// 客户
		this.CUSTOMER_ID = p.get('customerId') || (framework.crossDomain ? (typeof CUSTOMER_ID == 'undefined' ? parent.CUSTOMER_ID : CUSTOMER_ID) : top.CUSTOMER_ID);
		this.CUSTOMER_NAME = p.get('customerName') || (framework.crossDomain ? (typeof CUSTOMER_NAME == 'undefined' ? parent.CUSTOMER_NAME : CUSTOMER_NAME) : top.CUSTOMER_NAME);
		
		// 登录用户
		this.AD_USER_ID = p.get('adUserId') || (framework.crossDomain ? (typeof AD_USER_ID == 'undefined' ? parent.AD_USER_ID : AD_USER_ID) : top.AD_USER_ID);
		this.AD_USER_NAME = p.get('adUserName') || (framework.crossDomain ? (typeof AD_USER_NAME == 'undefined' ? parent.AD_USER_NAME : AD_USER_NAME) : top.AD_USER_NAME);
		this.AD_USER_REALNAME = p.get('adUserRealName') || (framework.crossDomain ? (typeof AD_USER_REALNAME == 'undefined' ? parent.AD_USER_REALNAME : AD_USER_REALNAME) : top.AD_USER_REALNAME);
		
		// 组织机构
		this.AD_ORG_ID = p.get('adOrgId') || (framework.crossDomain ? (typeof AD_ORG_ID == 'undefined' ? parent.AD_ORG_ID : AD_ORG_ID) : top.AD_ORG_ID);
		this.AD_ORG_NAME = p.get('adOrgName') || (framework.crossDomain ? (typeof AD_ORG_NAME == 'undefined' ? parent.AD_ORG_NAME : AD_ORG_NAME) : top.AD_ORG_NAME);
		this.AD_ORG_NO = p.get('adOrgNo') || (framework.crossDomain ? (typeof AD_ORG_NO == 'undefined' ? parent.AD_ORG_NO : AD_ORG_NO) : top.AD_ORG_NO);
	},
	
	/**
	 * 获得Model的ID
	 * @param {} model
	 * @return {}
	 */
	getModelId: function(model) {
		if (Ext.isEmpty(model, false)) {
			model = this.model;
		}
		
		return model + 'Id';
	},
	
	getModuleLayout: function() {
		var mdl = this,
		
			w = mdl.width,
			h = mdl.height,
			mh = mdl.minHeight;
		
		if (!Ext.isEmpty(mdl.moduleLayout, false))
			return mdl.moduleLayout;
			
		return Ext.isEmpty(w, false) && Ext.isEmpty(h, false) && Ext.isEmpty(mh, false) ? 'border' : 'container';
	},
	
	initLayout: function() {
		var mdl = this,
			
			w = mdl.width,
			h = mdl.height,
			
			moduleLayout = mdl.getModuleLayout();

		if (moduleLayout == 'border') {
			
		} else {
			mdl.items = {
				id: Ext.id(null, 'pf-module-bwrap-'),
				region: 'center', 
				frame: false, 
				border: false, 
				autoScroll: true,
				
				items: {
					id: Ext.id(null, 'pf-module-container-'),
					region: 'center',
					layout: 'border', 
					frame: false,
					border: false,
					autoScroll: false,
					
					width: w,
					height: h
				}
			};
		}
		
		mdl.moduleLayout = moduleLayout;
	},
	
	doLayout: function() {
		if (!this.rendered) return;
		
		var mdl = this,
		
			ct = mdl.container,
			csize = ct.getViewSize(true),
			
			mh = mdl.minHeight,
			
			moduleLayout = mdl.getModuleLayout();

		mdl.setHeight(csize.height);

		// fixed ie8
		if (Ext.isIE8) {
			mdl.setWidth(csize.width - parseInt(mdl.el.getStyle('padding-right')));
		}

		// 调整最小高度
		if (moduleLayout == 'container' && !Ext.isEmpty(mh, false)) {
			var h;
			if (csize.height <= mh) {
				h = mh;
				
			} else {
				var el = mdl.el,
			
					header = mdl.header,
					tbar = mdl.getTopToolbar(),
					bbar = mdl.getBottomToolbar(),
					fbar = mdl.getFooterToolbar(),
					
					excludeHeight = el.getMargins('tb') + 2;

				if (!Ext.isEmpty(header))
					excludeHeight += header.getHeight();
				if (!Ext.isEmpty(tbar))
					excludeHeight += tbar.getHeight();
				if (!Ext.isEmpty(bbar))
					excludeHeight += bbar.getHeight();
				if (!Ext.isEmpty(fbar))
					excludeHeight += fbar.getHeight();
					
				h = csize.height - excludeHeight;
			}
			
			mdl.getComponent(0).getComponent(0).setHeight(h);
		}
		
		mdl.doModuleLayout(mdl);
		
		framework.core.Module.superclass.doLayout.apply(this, arguments);
	},
	
	onRender: function() {
		var mdl = this,
		
			w = mdl.westPnl = mdl.createPanel(mdl.west()),
			e = mdl.eastPnl = mdl.createPanel(mdl.east()),
			n = mdl.northPnl = mdl.createPanel(mdl.north()),
			s = mdl.southPnl = mdl.createPanel(mdl.south()),
			c = mdl.centerPnl = mdl.createPanel(mdl.center());

		var ct = mdl.modulePnl = mdl.moduleLayout == 'border' ? mdl : mdl.getComponent(0).getComponent(0);
		
		if (!w && !n && !e && !s) {
			c.region = 'center';
			ct.add(c);
			
		} else {
			var r = (!w && !e) ? ct : new Ext.Panel({
				frame: false,
				border: false,
				plain: false,
				layout: 'border',
				autoScroll: true
			});
			
			if (n) {
				n.region = 'north';
				n.autoScroll = false;
				n.collapsible = false;
				n.autoHeight = n.height == null;
				
				r.add(n);
			}
			
			if (s) {
				s.region = 'south';
				s.collapsible = false;
				r.add(s);
			}
			
			c.region = 'center';
			r.add(c);
			
			if (w || e) {
				if (e) {
					e.region = 'east';
					e.collapsible = false;
					ct.add(e);
				}
				
				if (w) {
					w.region = 'west';
					w.collapsible = false;
					ct.add(w);	
				}
				
				r.region = 'center';
				ct.add(r);
			}
		}
		
		framework.core.Module.superclass.onRender.apply(this, arguments);
		
		mdl.loadMask = new Ext.LoadMask(mdl.el, {
			msg: '正在创建页面,请耐心等候...'
		});
		
		mdl.setDefaultAction(mdl.loadData, mdl);
	},
	
	afterRender: function() {
		var mdl = this;

		framework.core.Module.superclass.afterRender.apply(this, arguments);
		
		var allComponentsRendered = false,
			
			runer = new Ext.util.TaskRunner(),
			
			/**
			 * 检查所有对象是否都已经创建成功
			 * @param Ext.Panel pnl
			 */
			inspectComponentsRender = function(pnl) {
				var rendered = true,
					items = pnl.items;
				
				if (!(pnl instanceof Ext.Panel)) return true;
				if (!pnl.rendered) return false;
				
				if (pnl instanceof Ext.TabPanel) {
					items = new Ext.util.MixedCollection();
					items.add(pnl.getActiveTab());
					
					// 当前TabPanel的autoScroll属性设置为true时需要以下代码,否则会出滚动条
					/*if (Ext.isWebKit && pnl.body.isScrollable()) {
						pnl.body.setOverflow('hidden');
					}*/
				}
				
				if (!Ext.isEmpty(items)) {
					items.each(function(item) {
						rendered = inspectComponentsRender(item);
						if (rendered === false)
							return false;
					});
				}
				
				return rendered;
			}
			
			run = function() {
				try {
					mdl.loadMask.show();
					
					if (allComponentsRendered) {
						mdl.allComponentsRendered = true;
						
						mdl.loadMask.hide();
						
						mdl.fireEvent('modulerender', mdl);
						
						return false;
					}
	
					allComponentsRendered = inspectComponentsRender(mdl);
					
				} catch (e) {
					mdl.err(e);
					return false;
				}
			};
			
		runer.start({
			run: run,
			interval: 0
		});
	},
	
	/**
	 * @private
	 * @param {Object/Ext.Panel} cfg
	 * @return {Ext.Panel}
	 * @description 创建Panel
	 */
	createPanel: function(cfg) {
		if (Ext.isEmpty(cfg)) return null;
		
		var pnl = cfg;
		
		if (pnl instanceof Ext.Panel)
			return pnl;
			
		return Ext.create(pnl, pnl.xtype || 'panel');
	},
	
	/**
	 * @param {Object} options
	 * 		{String} url
	 * 		{Object} params
	 * 		{Long} customerId
	 * 		{Function} success
	 * 		{Object} scope
	 */
	navigate: function(options) {
		if (!options) return;
		
		if (Ext.isString(options))
			options = {url: options};
		if (Ext.isEmpty(options.url, false)) return;
		
		var customerId = options.customerId,
			params = options.params,
			url = framework.getUrl(options.url, params),
			callback = options.success,
			scope = options.scope;

		var redirect = function() {
			nt.redirect(url);
			
			if (callback)
				Ext.callback(callback, scope);
		};
		
		if (Ext.isEmpty(customerId, false)) {
			redirect();
		} else {
			nt.changeCustomer(customerId, function() {
				redirect();
			});
		}
	},
	
	/**
	 * 创建窗口
	 * @param {Object} winCfg
	 * @param {String} winClazz
	 */
	createWindow: function(winCfg, winClazz) {
		var moduleWindow = this.moduleWindow;
		
		if (!Ext.isEmpty(moduleWindow)) {
			return moduleWindow.createWindow(winCfg, winClazz);
			
		} else {
			// var framework = top.window.Ext.get('centeriframe').dom.contentWindow.framework;
			return framework.createWindow(winCfg, winClazz || 'framework.widgets.window.ModuleWindow');
		}
	},
	
	/**
	 * 设置默认动作
	 * @param Function fn
	 * @param Object scope
	 */
	setDefaultAction: function(fn, scope) {
		var keyMap = this.getDefaultActionKeyMap();
		keyMap.bindings = [];

		keyMap.addBinding({
			key: '\r',
			shift: true,
			fn: fn,
			scope: scope
		});
	},
	
	/**
	 * @return {}
	 */
	getDefaultActionKeyMap: function() {
		if (!this.defaultActionKeyMap)
			this.defaultActionKeyMap = new Ext.KeyMap(this.el);
			
		return this.defaultActionKeyMap;
	},
	
	/**
	 * 清空指定model对应的Store缓存数据
	 * @param {Array<String>} models
	 */
	clearStoreData: function(models) {
		if (Ext.isEmpty(models) || models.length == 0) return;
		
		Ext.iterate(models, function(model) {
			if (!Ext.isEmpty(model, false)) {
				var storeTypes = S.MODEL_STORE_TYPE[model];
				Ext.iterate(storeTypes, function(storeType) {
					S.clearStoreData(storeType);
				});
			}
		});
	},
	
	///////////////////////////////////////////////////
	
	/**
	 * 窗口大小发生改变
	 */
	onWindowResize: function() {
		var mdl = this;

		mdl.doLayout();
		
		try {
			var size = mdl.getSize();
			mdl.fireEvent('moduleresize', size.width, size.height);
		} catch (e) {}
	},
	
	onComponentsRendered: function(mdl) {
		// mdl.debug(mdl.moduleId + ' all components is rendered.');
		
		// 修复了按Backspace键时会退回到登录页面的问题
		Ext.getBody().on({
			'keydown': function(e, target) {
				var event = e.browserEvent,
					tagName = target.tagName;
					
				if (e.keyCode == Ext.EventObject.BACKSPACE) {
					if (!Ext.isEmpty(tagName) && (tagName == 'INPUT' || tagName == 'TEXTAREA')) {
						event.returnValue = true;
						
						var fld = Ext.getCmp(target.id);
						if (fld.readOnly === true || (fld instanceof Ext.form.ComboBox && fld.editable === false)) {
							event.returnValue = false;
							e.preventDefault();
						}
						
					} else {
						event.returnValue = false;
						e.preventDefault();
					}
				}
			}
		});

		mdl.onModuleRender(mdl);

		if (mdl.autoLoadData === true) {
			mdl.loadData.timer(function() {
				return mdl.allowLoadData === true;
			}, mdl);
		}
	},
	
	beforeDestroy: function() {
		var mdl = this;

		if	(mdl.onWindowResize) {
			Ext.EventManager.removeResizeListener(mdl.onWindowResize, mdl);
		}

		framework.core.Module.superclass.beforeDestroy.call(this);
	},
	
	/**
	 * 注册当前Module
	 */
	register: function() {
		framework.registerModule(this);
	},
	
	/**
	 * 删除当前Module
	 */
	unregister: function() {
		framework.unregisterModule(this);
	},
	
	getUrl: framework.getUrl,
	getManageUrl: framework.getManageUrl,
	log: framework.log,
	debug: framework.debug,
	err: framework.err,
	
	/**
	 * 初始化应用模块接口
	 * @interface
	 */
	initModule: Ext.emptyFn,
	
	/**
	 * 加载数据
	 * @type 
	 */
	loadData: Ext.emptyFn,
	
	/**
	 * 模块布局
	 * @type 
	 */
	doModuleLayout: Ext.emptyFn,
	
	/**
	 * 模块渲染完毕
	 * @type 
	 */
	onModuleRender: Ext.emptyFn,
	
	/**
	 * 模块改变大小
	 * @interface
	 */
	onModuleResize: Ext.emptyFn,
	
	west: Ext.emptyFn,
	north: Ext.emptyFn,
	east: Ext.emptyFn,
	south: Ext.emptyFn,
	center: Ext.emptyFn
}); 