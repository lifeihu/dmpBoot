_package('framework.modules');

_import([
	'framework.core.Module',
	
	'framework.widgets.window.MaintainWindow',	
	'framework.widgets.PagingSelectionModel'
]);

framework.modules.SearchGridModule = Ext.extend(framework.core.Module, {
	border: false,
	
	autoLoadData: null,
	allowLoadData: false,
	
	/**
	 * @requires
	 * @cfg model
	 * @type String
	 */
	
	/**
	 * @requires
	 * @cfg gridKeyColumn
	 * @type String
	 * @description 主键名称
	 */
	gridKeyColumn: null,
	
	/**
	 * @cfg condition
	 * @type Object
	 * @description 默认查询条件
	 */
	condition: null,
	
	/**
	 * @cfg singleSelect
	 * @type Boolean
	 * @description 是否单选
	 */
	singleSelect: false,
	
	/**
	 * @cfg pagingSelection
	 * @type Boolean/Object
	 * @description 是否使用分页多选
	 */
	pagingSelection: true,
	
	/**
	 * @cfg buttonAddDisabled
	 * @type Boolean
	 * @description 是否禁止添加按钮
	 */
	buttonAddDisabled: false,
	
	/**
	 * cfg buttonRemoveDisabled
	 * @type Boolean
	 * @description 是否禁止删除按钮
	 */
	buttonRemoveDisabled: false,
	
	/**
	 * 添加按钮
	 * @type 
	 */
	buttonAddConfig: {
		text: '添加',
		iconCls: 'add'
	},
	
	/**
	 * 每行的修改操作按钮
	 * @type 
	 */
	actionUpdateConfig: null,
	
	/**
	 * 每行的查看操作按钮
	 * @type 
	 */
	actionViewConfig: null,
	
	initModule: function() {
		var mdl = this;
		
		if (mdl.actionUpdateConfig !== false) {
			mdl.actionUpdateConfig = {
				iconCls: 'edit',
				tooltip: '修改',
				handler: 'updateOnly'
			};
		}
		
		if (mdl.actionViewConfig !== false) {
			mdl.actionViewConfig = {
				iconCls: 'view',
				tooltip: '查看',
				handler: 'viewOnly'
			};
		}
		
		if (Ext.isEmpty(mdl.gridKeyColumn, false))
			mdl.gridKeyColumn = mdl.getModelId();

		if (Ext.isEmpty(mdl.removeUrl, false))
			mdl.removeUrl = mdl.model + '/remove';
			
		if (Ext.isObject(mdl.actionViewConfig))
			mdl.actionViewConfig.module = mdl;
		if (Ext.isObject(mdl.actionUpdateConfig))
			mdl.actionUpdateConfig.module = mdl;

		mdl.addEvents(
			/**
			 * @event beforecreateform
			 * @param {framework.modules.SearchGridModule} sm
			 * @param {Object} cfg
			 */
			'beforecreateform',
			
			/**
			 * @event beforecreategrid
			 * @param {framework.modules.SearchGridModule} sm
			 * @param {Object} cfg
			 */
			'beforecreategrid',
			
			/**
			 * @event loaddatacomplete
			 * @param {framework.modules.SearchGridModule}
			 * @param {Ext.data.Store} store
			 * @param {Array<Ext.data.Record>} records
			 */
			'loaddatacomplete',
		
			/**
			 * @event beforeremove
			 * @param {framework.modules.SearchGridModule}
			 * @param {Array<Number>} ids
			 * @param {Object} params
			 */
			'beforeremove',
			
			/**
			 * @event removecomplete
			 * @param {framework.modules.SearchGridModule}
			 * @param {Response}
			 */
			'removecomplete',
			
			/**
			 * @event removefailure
			 * @param {framework.modules.SearchGridModule}
			 */
			'removefailure',
			
			 /**
			 * @event beforecreate
			 * @param {framework.modules.SearchGridModule} mdl
			 * @param {Object} params
			 */
			'beforecreate',
            
            /**
             * @event updatecondition
             * @param {framework.modules.SearchGridModule} mdl
             * @param {Object} values
             */
            'updatecondition',
			
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
		
		framework.modules.SearchGridModule.superclass.initModule.call(this);
		
		mdl.on({
			tabchange: mdl.onTabChange,
			scope: mdl
		})
	},
	
	north: function() {
		var mdl = this,
			forms = [].concat(mdl.searcher());

		if (forms.length > 0) {
			var formCfg = forms[0];
			if (Ext.isEmpty(formCfg)) return null;
			
			if (mdl.fireEvent('beforecreateform', this, formCfg) === false)
				return null;
			
			var items = formCfg.items;
			for (var i = 0, len = items.length; i < len; i++) {
				var item = items[i],
				
					fieldLabel = item.fieldLabel,
					columnWidth = item.columnWidth || .2,
					labelWidth = item.labelWidth ? item.labelWidth : (!fieldLabel ? 60 : fieldLabel.length * 15),
					labelSeparator = item.labelSeparator;
				delete item.columnWidth;
				delete item.labelWidth;

				items[i] = {
					columnWidth: columnWidth,
					layout: 'form',
					border: false,
					labelWidth: labelWidth,
					labelSeparator: labelSeparator,
					
					items: Ext.apply({
						xtype: 'textfield'
					}, item)
				};
			};
			
			formCfg.items = {
				layout: 'column',
				border: false,
				items: items.concat({
					columnWidth: .03,
					items: {
						xtype: 'button',
						iconCls: 'search',
						tooltip: '查询',
						handler: mdl.loadData.createDelegate(mdl, [0]),
						scope: mdl
					}
				})
			};
			
			return Ext.apply({
				xtype: 'form',
				margins: '0 0 2 0',
				frame: true,
				plain: true,
				border: false
			}, formCfg);
		}
	},
	
	center: function() {
		var mdl = this, items = [],
			grids = [].concat(mdl.detailer());
			
		if (grids.length == 0) return;
		
		Ext.each(grids, function(gridCfg) {
			if (mdl.fireEvent('beforecreategrid', this, gridCfg) !== false) {
				var grid;
				
				if (gridCfg instanceof framework.core.Module) {
					grid = gridCfg;
					
				} else if (Ext.isEmpty(gridCfg.xtype, false)) {
					grid = mdl.createGrid(gridCfg);
					
				} else {
					grid = Ext.create(gridCfg);
				}
				
				if (!Ext.isEmpty(grid)) {
					items.push(grid);
				}
			}
		});
		
		if (items.length > 1) {
			var tabPnl = mdl.tabPnl = new Ext.TabPanel({
				frame: false,
				border: true,
				plain: !Ext.isEmpty(mdl.northPnl),
				activeTab: mdl.activeTab || 0,
				
				items: items
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
			
		} else if (items.length == 1) {
			
			mdl.getActiveTab = function() {
				return mdl.centerPnl;
			};
			
			mdl.setActiveTab = Ext.emptyFn;

			return items[0];
		}
		
		return null;
	},
	
	/**
	 * @private
	 * @param Object cfg
	 * @description 创建列表
	 */
	createGrid: function(cfg) {
		if (Ext.isEmpty(cfg)) return null;
		if (cfg instanceof Ext.grid.GridPanel) return cfg;
		
		var mdl = this,
			store = cfg.store,
			defaultActions = [],
			columns = cfg.columns || [];
			// cm = cfg.cm;
			
		/*if (Ext.isArray(cm))
			cm = {columns: cm};
		cm = Ext.apply({
			defaultSortable: true
		}, cm);
		var columns = cm.columns;*/
		
		if (mdl.actionViewConfig !== false)
			defaultActions.push(mdl.actionViewConfig);
		if (mdl.actionUpdateConfig !== false)
			defaultActions.push(mdl.actionUpdateConfig);
		cfg.actions = defaultActions.concat(cfg.actions || []);
			
		// 定义数据源
		cfg.store = store = mdl.createGridStore(store, columns);
		
		// 定义列
		var fixColumns = [], sm;
		if (cfg.checkboxable !== false) {
			if (!cfg.gridKeyColumn)
				mdl.pagingSelection = false;

			if (mdl.pagingSelection) {
				sm = new framework.widgets.PagingSelectionModel(
					typeof mdl.pagingSelection != 'object' 
						? {} 
						: mdl.pagingSelection
				);
			} else 
				sm = new Ext.grid.CheckboxSelectionModel();

			sm.singleSelect = Ext.isEmpty(cfg.singleSelect) ? mdl.singleSelect : cfg.singleSelect;
			fixColumns.push(sm);
			cfg.sm = sm;
		}
		
		if (cfg.numberable !== false)
			fixColumns.push(new Ext.grid.RowNumberer());

		cfg.columns = fixColumns.concat(columns);
		/*cm.columns = fixColumns.concat(columns);
		cfg.cm = new Ext.grid.ColumnModel(cm);*/
		
		// 工具条按钮
		mdl.initGridTopToolbar(cfg);

		// 定义分页工具条
		if (cfg.allowPagingToolbar !== false) {
			cfg.bbar = cfg.paging = new Ext.PagingToolbar({
				pageSize: store.limit || cfg.pageSize || mdl.pageSize || 15,
				store: store,
				displayInfo: true
			});
		}

		return new Ext.grid.GridPanel(cfg);
	},
	
	/**
	 * 创建数据源
	 * @param {Object/Ext.data.Store} store
	 * @param {Array<Object>} columns
	 */
	createGridStore: function(store, columns) {
		var mdl = this,
			store = store || {};
		
		if (!(store instanceof Ext.data.Store)) {
			var fields = [mdl.getModelId(), 'createTime', 'updateTime'].concat(store.fields || []);
			for (var i = 0, len = columns.length; i < len; i++) {
				var dataIndex = columns[i].dataIndex;
				if (!Ext.isEmpty(dataIndex, false))  {
					fields.push(dataIndex);
				}
			}
			delete store.fields;
			
			if (Ext.isEmpty(store.url, false)) {
				store.url = mdl.model + '/paging';
			}

			store =	Ext.apply({
				autoLoad: false,
				totalProperty: "total",
				root: "paginationResults",
				remoteSort: true,
				fields: fields,
				limit: store.limit || mdl.pageSize || 15
			}, store);

			store = Ext.isEmpty(store.stype, false) ? new Ext.data.JsonStore(store) : S.create(store);
		}
		
		store.on({
			beforeload: mdl.onBeforeLoad,
			load: mdl.onLoadComplete,
			scope: mdl
		});
		/*if (Ext.isEmpty(store.type, false)) {
			store.on("beforeload", mdl.onBeforeLoad, mdl);
		} else {
			// 通过S.get方式获得的数据源如果需要组织查询条件不能通过beforeload事件，只能通过beforeCondition方法
			store.beforeCondition = mdl.onBeforeLoad.createDelegate(mdl);
		}*/
		
		return store;
	},
	
	/**
	 * 初始化列表的工具条
	 */
	initGridTopToolbar: function(cfg) {
		var mdl = this,
			allowDefaultButtons = cfg.allowDefaultButtons,
		
			buttons = [],
			tbuttons = cfg.tbuttons || [];
			
		if (allowDefaultButtons !== false) {
			buttons = [
				'-',
			
				mdl.BUTTON_ADD = new Ext.Button(Ext.apply({
					disabled: mdl.buttonAddDisabled,
					handler: mdl.create.createDelegate(mdl, [{}])
				}, mdl.buttonAddConfig)),
				
				mdl.BUTTON_REMOVE = new Ext.Button({
					text: "删除",
					iconCls: "remove",
					// disabled: mdl.buttonRemoveDisabled,
					hidden: mdl.buttonRemoveDisabled,
					handler: mdl.removed,
					scope: mdl
				}),
				
				'-'
			];
		}
		
		var count = buttons.length;
		Ext.each(tbuttons, function(btn) {
			var btnCfg = btn,
				btnPos;
			
			if (Ext.isArray(btn)) {
				btnCfg = btn[0];
				if (btn.length == 1) {
					buttons.push(btnCfg);
					
				} else {
					btnPos = btn[1] + count;
				}
				
			} else {
				if (!Ext.isEmpty(btnCfg.position, false)) {
					btnPos = btnCfg.position + count;
					delete btnCfg.position;
				} else
					buttons.push(btn);
			}
			
			if (!Ext.isEmpty(btnPos, false))
				buttons.splice(btnPos, 0, btnCfg);
		});
		
		if (buttons && buttons.length > 0)
			cfg.tbar = buttons;
	},
	
	loadData: function(params) {
		var mdl = this,

			form = Ext.isEmpty(mdl.northPnl) ? null : mdl.northPnl.form,
			pnl = mdl.getActiveTab();

		if (!Ext.isEmpty(form)) {
			if (!form.isValid()) return;
		}
		
		var start = null;
		if (params != null) {
			if (Ext.isNumber(params)) {
				start = params;
			} else {
				start = params.start;
			
				mdl.setCondition(params['condition']);
			}
		}		

		if (pnl instanceof Ext.grid.GridPanel) {
			var paging = pnl.paging;
			
			if (Ext.isEmpty(paging)) {
				pnl.store.load({params: Ext.apply({
					start: start == null ? 0 : start
				}, params)});
				
			} else {
				paging.doLoad(start == null ? paging.cursor : start);
			}
				
		} else {
			pnl.loadData(mdl.getCondition(true));
		}
	},
	
	/**
	 * 获得查询条件(从默认表单中获取)
	 * @param {Boolean} merge 是否将Form的数据与this.conidtion的数据合并
	 * @return {Object}
	 */
	getCondition: function(merge) {
		merge = merge !== true ? false : true;
		
		var mdl = this, cond,
			form = Ext.isEmpty(mdl.northPnl) ? null : mdl.northPnl.form;
			
		if (!Ext.isEmpty(form)) 
			cond = form.getValues();
		
		if (merge)
			cond = Ext.apply(mdl.condition || {}, cond);
			
		return cond;
	},
	
	/**
	 * 设置查询条件
	 * @param {Object} condition
	 */
	setCondition: function(condition) {
		var mdl = this;
		
		if (!Ext.isEmpty(condition)) {
			var form = mdl.northPnl;
			
			mdl.condition = condition;
			
			if (!Ext.isEmpty(form)) {
				var basicForm = form.form;
				
				basicForm.setValues(condition);
				mdl.fireEvent('updatecondition', mdl, basicForm.getValues());
			}
		}
		
		mdl.allowLoadData = true;
	},
	
	/**
	 * 设置列表是否单选
	 * @param {Boolean} singleSelect
	 */
	setSingleSelect: function(singleSelect) {
		var mdl = this,
			items = mdl.centerPnl.items;
			
		mdl.singleSelect = singleSelect;
		
		if (!Ext.isEmpty(items)) {
			items.each(function(item) {
				if (item instanceof Ext.grid.GridPanel)
					item.getSelectionModel().singleSelect = singleSelect;
			});
		}
	},
	
	/**
	 * 获得当前激活的Grid中所有选中的记录
	 * @return {Array<Record>}
	 */
	getSelections: function() {
		var item = this.getActiveTab();
		
		if (item instanceof Ext.grid.GridPanel)
			return item.getSelectionModel().getSelections();
			
		else if (!Ext.isEmpty(item.getSelections))
			return item.getSelections();
		
		return [];
	},
	
	/**
	 * 获得选中记录(单选)
	 * @return {Record}
	 */
	getSelected: function() {
		var item = this.getActiveTab();
		
		if (item instanceof Ext.grid.GridPanel)
			return item.getSelectionModel().getSelected();
			
		else if (!Ext.isEmpty(item.getSelected))
			return item.getSelected();
		
		return null;
	},
	
	/**
	 * 获得指定名称的查询字段
	 * @param {} name
	 */
	findField: function(name) {
		if (this.northPnl == null) {
			return;
		}
		
		var form = this.northPnl.form;
		return form.findField(name);
	},
	
	////////////////////////////////////////////////////////////////////
	
	create: function(params) {
		var mdl = this,
			params = params || {};
		
	    if (mdl.fireEvent('beforecreate', mdl, params) !== false) {
	    	mdl.createWindow(null, 'framework.widgets.window.MaintainWindow').create(params);
		}
	},
	
	/**
	 * 查看数据
	 * @param {} params
	 */
	view: function(params) {
		this.createWindow(null, 'framework.widgets.window.MaintainWindow').view(params);
	},
	
	/**
	 * 查看数据
	 * @param {} params
	 */
	viewOnly: function(params) {
		this.createWindow(null, 'framework.widgets.window.MaintainWindow').viewOnly(params);
	},
	
	/**
	 * 修改数据
	 * @param {} params
	 */
	update: function(params) {
		this.createWindow(null, 'framework.widgets.window.MaintainWindow').update(params);
	},
	
	/**
	 * 修改数据
	 * @param {} params
	 */
	updateOnly: function(params) {
		this.createWindow(null, 'framework.widgets.window.MaintainWindow').updateOnly(params);
		
		// 动画效果
		/*var g = params.grid,
			cell = g.getView().getCell(params.rowIndex, params.colIndex);
		
		this.createWindow({
			animateTarget: cell
		}, 'framework.widgets.window.MaintainWindow').updateOnly(params);*/
	},
	
	/** 
	 * @description 删除列表中选中的数据
	 */
	removed: function() {
		var mdl = this,
			item = mdl.getActiveTab();
			
		if (Ext.isEmpty(item)) return;
		
		if (item instanceof Ext.grid.GridPanel) {
			var removeColumn = item.removeColumn || item.gridKeyColumn || mdl.gridKeyColumn,
				
				maintainCfg = mdl.maintain || {},
				gridMaintainCfg = item.maintain || {},
				url = item.removeUrl || gridMaintainCfg.removeUrl || maintainCfg.removeUrl || mdl.removeUrl;

			if (Ext.isEmpty(url, false)) return;
			
			var selections = mdl.getSelections(),
				len = selections.length;
			if (len == 0) return;
			
			var ids = [], params = {};
			Ext.each(selections, function(record) {
				ids.push(record.get(removeColumn));
			});

			if (mdl.fireEvent('beforeremove', mdl, ids, params) !== false) {
				Ext.Msg.confirm('提示', String.format('是否删除选中的 {0} 条记录?', len), function(btn) {
					if (btn != 'yes') return;
					
					params['id'] = ids.join(',');

					var loadMask = mdl.loadMask;
					loadMask.msg = '正在删除数据, 请耐心等候...';
					loadMask.show();
					
					Ext.Ajax.request({
						url: url,
						params: params,
						// waitMsg: '正在删除数据,请耐心等候...',
						
						success: function(response) {
							mdl.onAfterAction();

							mdl.clearStoreData([mdl.model]);
							mdl.fireEvent('removecomplete', mdl, response);
							
							loadMask.hide();
						},
						
						failure: function() {
							mdl.fireEvent('removefailure', mdl);
						}
					});
				});
			}
			
		} else if (!Ext.isEmpty(item.removed))
			item.removed();
	},
	
	/**
	 * @override
	 * @param {Object} winCfg
	 * @param {String} winClazz
	 */
	createWindow: function(winCfg, winClazz) {
		var mdl = this;
		
		if (winClazz == 'framework.widgets.window.MaintainWindow') {
			var maintainCfg = mdl.getActiveTab().maintain || mdl.maintain;
			winCfg = Ext.apply({
				searchGridModule: mdl,
				
				listeners: {
					savecomplete: mdl.onAfterAction,
					removecomplete: mdl.onAfterAction,
					scope: mdl,
					
					single: true
				}
			}, Ext.apply(winCfg || {}, maintainCfg));
		} 
		
		return framework.modules.SearchGridModule.superclass.createWindow.call(this, winCfg, winClazz);
	},
	
	////////////////////////////////////////////////////////////////////
	
	onModuleRender: function(mdl) {
		mdl.setDefaultAction(mdl.loadData.createDelegate(mdl, [0]), mdl);
		
		if (mdl.autoLoadData == null)
			mdl.autoLoadData = !(mdl.getActiveTab() instanceof Ext.TabPanel);
		
		mdl.setCondition(mdl.condition);

		framework.modules.SearchGridModule.superclass.onModuleRender.call(this, mdl);
	},
	
	onTabChange: function() {
		var mdl = this;

		mdl.loadData.timer(function() {
			return mdl.allowLoadData === true;
		}, mdl);
	},
	
	onBeforeLoad: function(store, options) {
		var mdl = this,
			condition = mdl.getCondition(true);

		var bp = store.baseParams || {};
		bp.condition = Ext.apply(/*bp.condition || */{}, condition);
	},
	
	onLoadComplete: function(store, records) {
		var mdl = this;
		mdl.fireEvent('loaddatacomplete', mdl, store, records);
	},
	
	onAfterAction: function() {
		var mdl = this,
			item = mdl.getActiveTab(),
			store = item.store;
			
		if (store && !Ext.isEmpty(store.stype, false)) {
			S.clearStoreData(store.stype);
		}
			
		this.loadData();
	},
	
	/*beforeDestroy: function(mdl) {
		var mdl = this,
			items = mdl.centerPnl.items;

		if (items) {
			items.each(function(item) {
				var store = item.store;
				if (store) {
					store.un('beforeload', mdl.onBeforeLoad, mdl);
					store.un('load', mdl.onLoadComplete, mdl);
				}
			});
		}
		
		framework.modules.SearchGridModule.superclass.beforeDestroy.call(this);
	},*/
	
	searcher: Ext.emptyFn,
	detailer: Ext.emptyFn
});
