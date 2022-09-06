_package('framework.modules');

_import([
	'framework.core.Module',
	'framework.widgets.MaintainFormPanel',
	'framework.widgets.MaintainEditorGridPanel'
]);

framework.modules.MaintainModule = Ext.extend(framework.core.Module, {
	monitorValidate: true,
	minHeight: 400,
	
	/**
	 * @requires
	 * @cfg loadDataUrl
	 * @type String
	 * @description 加载数据的URL
	 */
	
	/**
	 * @requires
	 * @cfg saveUrl
	 * @type String
	 * @description 保存记录的URL 
	 */
	
	/**
	 * @requires
	 * @cfg removeUrl
	 * @type String
	 * @description 删除记录的URL
	 */
	
	/**
	 * @cfg buttonSaveHidden
	 * @type Boolean
	 * @description 是否加入保存按钮
	 */
	buttonSaveHidden: true,
	
	/**
	 * @cfg autoHideWindow
	 * @type Boolean
	 * @description 保存完毕后是否关闭窗口
	 */
	autoHideWindow: true,
	
	/**
	 * @cfg mergeToMaster
	 * @type Boolean
	 * @description 是否将子表数据合并至主表数据中,在子表的Panel中也可以设置该属性
	 */
	mergeToMaster: true,
	
	initModule: function() {
		var mdl = this,
			p = this.parameter;
		
		Ext.apply(mdl, {
			maintainPanels: new Ext.util.MixedCollection(),
			readOnly: mdl.view === true
		});

		// 是否需要自动添加保存按钮
		if (!mdl.buttonSaveHidden) {
			mdl.tbar = [mdl.BUTTON_SAVE = new Ext.Button({
				text: '保存',
				iconCls: 'save',
				handler: mdl.save,
				scope: mdl
			}), '-'].concat(mdl.tbar || []);
		}
		
		mdl.addEvents(
				
			/**
			 * @event beforeloaddata
			 * @param {framework.modules.MaintainModule} mdl
			 * @param {Object} data
			 * @description 在加载数据前触发
			 */
			"beforeloaddata",
				
			/**
			 * @event loaddatacomplete
			 * @param {framework.modules.MaintainModule} mdl
			 * @param {Object} data
			 * @description 所有维护面板的数据加载完成后触发
			 */
			"loaddatacomplete", 
			
			/**
			 * @event beforesave
			 * @param {framework.modules.MaintainModule} mdl
			 * @param {Object} param 提交时的参数
			 * @return {Boolean} false: 提交操作终止
			 * @description 数据在提交后台前触发
			 */
			"beforesave",
			
			/**
			 * @event savecomplete
			 * @param {framework.modules.MaintainModule} mdl
			 * @param {Object} result 保存后服务器端传回的结果
			 * @param {Object} data 提交服务器保存的数据
			 * @description 数据提交成功后触发
			 */
			"savecomplete",
			
			/**
			 * @event savefailure
			 * 数据提交失败后触发
			 */
			"savefailure",
			
			/**
			 * @event beforeremove
			 * 数据在删除前触发
			 * @param {Object} param 删除时的参数
			 * @return {Boolean} false: 删除操作终止
			 */
			"beforeremove",
			
			/**
			 * @event removecomplete
			 * 数据删除成功后触发
			 * @param {Object} data
			 */
			"removecomplete",
			
			/**
			 * @event removefailure
			 * @param {framework.modules.MaintainModule} mdl
			 */
			'removefailure',
			
			/**
			 * @event beforecreatepanel
			 * @param {framework.modules.MaintainModule}
			 * @param {Object} cfg
			 */
			"beforecreatepanel",
			
			/**
             * @event beforetabchange
             * @param {Ext.TabPanel} tp
             * @param {Ext.Panel} newTab The tab being activated
             * @param {Ext.Panel} currentTab The current active tab
             */
            'beforetabchange',
			
			/**
             * @event tabchange
             * @param {Ext.TabPanel} tp
             * @param {Ext.Panel} tab The new active tab
             */
            'tabchange',
            
            /**
             * @event beforevalidateorg
             * @param {framework.modules.MaintainModule} mdl
             * @param {Object} data
             */
            'beforevalidateorg'
		);
		
		mdl.border = !Ext.isEmpty(mdl.tbar) || !Ext.isEmpty(mdl.bbar);
		
		framework.modules.MaintainModule.superclass.initModule.call(this);
		
		mdl.detailItems = mdl.detailer() || [];
	},
	
	north: function() {
		var mdl = this;
		if (mdl.detailItems.length == 0) return null;

		return mdl.createMaster();
	},
	
	center: function() {
		var mdl = this;
		if (mdl.detailItems.length == 0) {
			return mdl.createMaster();
			
		} else {
			var items = mdl.detailItems;
			
			if (items.length > 0) {
				for (var i = 0, len = items.length; i < len; i++) {
					var item = items[i];
					
					if (!Ext.isEmpty(item.removeUrl, false)) {
						item.removeUrl = item.removeUrl;
					}
					
					var maintainPnl = mdl.panel(item, 'maintaineditorgrid');
					// tabPnl.add(maintainPnl);
					items[i] = maintainPnl;	
				};
				
				var tabPnl = new Ext.TabPanel({
					frame: false,
					border: true,
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
				
				mdl.on({
					tabchange: mdl.onTabChange,
					scope: mdl
				});
				
				delete mdl.detailItems;
				
				return tabPnl;
			}
		}
	},
	
	/**
	 * @private
	 */
	createMaster: function() {
		var mdl = this,
			pnl = mdl.masterPnl = mdl.panel(Ext.apply({
				isMaster: true,
				bodyStyle: 'padding:2px;'
			}, mdl.master()));
		
		return pnl;
	},
	
	/**
	 * @private
	 * 创建维护Panel
	 * @param {Object} cfg
	 * @param {String} xtype
	 */
	panel: function(cfg, xtype) {
		if (!cfg) return;
		
		var mdl = this, 
			pnl = null;
			
		cfg.module = mdl;
		mdl.fireEvent('beforecreatepanel', mdl, cfg);

		if (cfg instanceof Ext.Panel && Ext.isDefined(cfg.getMaintainPanelPlugin)) {
			pnl = cfg;
			
		} else {
			cfg = Ext.apply(cfg, {
				border: false,
				xtype: cfg.xtype || xtype || 'maintainform'
			});
			pnl = Ext.ComponentMgr.create(cfg);
		}
		
		mdl.maintainPanels.add(pnl);
		
		return pnl;
	},
	
	/////////////////////////////////////////////////////////////
	
	loadData: function() {
		var mdl = this,
			loadDataUrl = mdl.loadDataUrl;

		if (Ext.isEmpty(loadDataUrl, false)) {
			mdl.fireEvent('loaddatacomplete', mdl, null, true);
			return;
		}
		
		var params = {},
			keyColumn = mdl.getKeyColumn();
		params['id'] = mdl.getKeyValue();
		
		var loadMask = mdl.loadMask;
		loadMask.msg = '正在加载数据, 请耐心等候...';
		loadMask.show();

		console.log('loadData', loadDataUrl)
		Ext.Ajax.request({
			url: loadDataUrl,
			// waitMsg: '正在加载数据,请耐心等候...',
			params: params,
			
			success: function(response) {
				var data;
				try {
					data = Ext.decode(response.responseText);
				} catch(e) {
					return;
				}

				if (mdl.fireEvent('beforeloaddata', mdl, data) === false) return;
				    for (item in data.jobDatasyncConfig) {
						if (item == 'sourceCustomerParameter' || item == 'targetCustomerParameter') {
							if(data.jobDatasyncConfig[item] && data.jobDatasyncConfig[item].substr(0, 1) == ',') {
								data.jobDatasyncConfig[item] = data.jobDatasyncConfig[item].substr(1,data.jobDatasyncConfig[item].length);
							}
						}
				    }
				var items = mdl.maintainPanels;
				items.each(function(item) {
					item.loaded = false;
					item.setData(data, true);

					if (item.rendered) {
						item.loadData();
					}
				});

				mdl.fireEvent('loaddatacomplete', mdl, data, true);
				
				loadMask.hide();
				
				// 焦点定位在第一个可编辑区域
				var masterPnl = mdl.masterPnl;
				if (masterPnl) {
					var items = masterPnl.form.items;
					items.each(function(item) {
						if (item.readOnly === false && item.disabled === false && item.getXType() != 'hidden') {
							item.focus(true, 1);
							return false;
						}
					});
				}
			},
			
			failure: function() {
				loadMask.hide();
			}
		});
	},
	
	view: function() {
		this.setReadOnly(true);
	},
	
	save: function(callback, scope) {
		var mdl = this,
			saveUrl = mdl.saveUrl;
			
		if (Ext.isEmpty(saveUrl, false)) return;
			
		if (!mdl.validate()) return;
		
		var 
			// 需要被传入beforesave事件的数据，此时子表数据并未合并至主表
			data = {},
		
			// 需要被合并至主表的主从映射
			needMergeMapping = {};
			
		mdl.maintainPanels.each(function(pnl) {
			if (!Ext.isEmpty(pnl.getValue) && pnl.rendered === true) {
				var mergeToMaster = pnl.mergeToMaster !== false && mdl.mergeToMaster,
					childrenDataRoot = pnl.childrenDataRoot,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
					specialConfig = '';

				if (mergeToMaster && !Ext.isEmpty(childrenDataRoot)) {
					needMergeMapping[pnl.model] = childrenDataRoot;
				}
				
				Ext.iterate(pnl.form.items.items, function(item) {
					// 新增参数
					if (item.initialConfig.cls == 'special') {
					    specialConfig += '"' + item.name + '":"' + item.value + '",'
					}
					
					// 去掉列里的格式提示信息，否则会传到后台
					if (item.name == 'sourceColumnFields' || item.name == 'targetColumnFields' || item.name == 'sourceCustomerParameter' || item.name == 'targetCustomerParameter') {
						if(item.getValue() == item.emptyText || item.getValue() == '') {
							item.setValue('');
							item.emptyText = '';
						    item.reset();
						}
					}
				});
				specialConfig = specialConfig.substr(0, specialConfig.length - 1);
				if(pnl.form.findField('specialConfig')) {
					pnl.form.findField('specialConfig').setValue(specialConfig);
				}
				Ext.apply(data, pnl.getValue(false, true));
			}
		});
		if (mdl.fireEvent('beforesave', mdl, data) === false) return;
		// console.log(data);
		var masterPnl = mdl.masterPnl;
		if (!Ext.isEmpty(masterPnl)) {
			var masterModel = masterPnl.model,
				masterData = data[masterModel],
				
				childrenDataRoots = [];

			// 将明细表数据合并至主表
			Ext.iterate(needMergeMapping, function(model) {
				var childrenDataRoot = needMergeMapping[model],
					childrenData = data[model];

				masterData[childrenDataRoot] = childrenData;
				delete data[model];

				childrenDataRoots.push(childrenDataRoot);
			});
			
			if (childrenDataRoots.length > 0) {
				data['childrenDataRoot'] = childrenDataRoots.join(',');
			}
		}

		var saveData = Ext.apply({}, data);
		Ext.iterate(data, function(model) {
			var value = data[model];
			if (!Ext.isString(value))
				data[model] = Ext.encode(value);
		});
		
		var loadMask = mdl.loadMask;
		loadMask.msg = '正在保存数据, 请耐心等候...';
		loadMask.show();

		Ext.Ajax.request({
			url: saveUrl,
			params: data,
			// waitMsg: '正在提交数据,请耐心等候...',
			
			success: function(response) {
				var result;
				
				try {
					result = Ext.decode(response.responseText);
				} catch(e) {
					result = null;
				}

				mdl.clearStoreData();
				mdl.fireEvent("savecomplete", mdl, result, saveData);
				
				if (callback) {
					Ext.callback(callback, scope, [mdl, result, saveData]);
				}
				
				loadMask.hide();
			},
			
			failure: function() {
				if (mdl.moduleWindow)
					mdl.moduleWindow.BUTTON_SAVE.enable();

				mdl.fireEvent('savefailure', mdl);
				loadMask.hide();
			}
		});
	},
	
	removed: function() {
		var mdl = this,
			removeUrl = mdl.removeUrl;
			
		if (Ext.isEmpty(removeUrl, false)) return;
		
		Ext.Msg.confirm('提示', '是否删除选中的记录?', function(btn) {
			if (btn != "yes") return;
		
			// TODO 如果没有主表的情况下该如何处理?
			var masterPnl = mdl.masterPnl,
				value = masterPnl.getValue(),
				masterKeyColumn = masterPnl.getKeyColumn(),
				
				params = {};
				
			params[masterKeyColumn] = value[masterKeyColumn];
			// params = Ext.apply(this.removeParams || {}, params);

			if (mdl.fireEvent("beforeremove", params) === false) return;
		
			var loadMask = mdl.loadMask;
			loadMask.msg = '正在删除数据, 请耐心等候...';
			loadMask.show();
			
			Ext.Ajax.request({
				url: removeUrl,
				params: params,
				
				success: function(response, options) {
					var data;
					
					try {
						data = Ext.decode(response.responseText);
					} catch (e) {
						data = null;
					}
				
					mdl.clearStoreData();-
					mdl.fireEvent("removecomplete", data);
					
					loadMask.hide();
				},
				
				failure: function() {
					mdl.fireEvent('removefailure', mdl);
				}
			});
		});
	},
	
	clearStoreData: function() {
		var mdl = this,
			storeTypes = [],
			maintainPanels = mdl.maintainPanels;
		
		maintainPanels.each(function(pnl) {
			var model = pnl.model;
			if (!Ext.isEmpty(model, false)) {
				storeTypes.push(model);
			}
		});
		
		if (storeTypes.length > 0) {
			framework.modules.MaintainModule.superclass.clearStoreData.call(mdl, storeTypes);
		}
	},
	
	/////////////////////////////////////////////////////////////
	
	/**
	 * 获得需要被校验的字段
	 * @return {Ext.form.Field/Ext.util.MixedCollection<Ext.form.Field>/Array<Ext.form.Field>} 
	 */
	getMonitoringValidateField: function() {
		var mdl = this, fields = [], 
			maintainPanels = mdl.maintainPanels;

		maintainPanels.each(function(pnl) {
			if (Ext.isDefined(pnl.getMonitoringValidateField)) {
				fields = fields.concat(pnl.getMonitoringValidateField());
			}
		});

		return fields;
	},
	
	/**
	 * 检验数据
	 * @return {}
	 */
	validate: function() {
		var mdl = this,
			plg = mdl.getModuleValidatePlugin(),
			
			validFn = function() {
				var maintainPanels = mdl.maintainPanels,
					success = !Ext.isEmpty(mdl.saveUrl, false);
					
				maintainPanels.each(function(pnl) {
					if (!Ext.isEmpty(pnl.valid) && pnl.rendered === true) {
						if (pnl.valid(plg) === false) {
							success = false;
							return false;
						}
					}
				});

				return success;
			};
			
		return plg.validate(validFn);
	},
	
	/**
	 * 获得主表维护字段
	 * @param {String} name 
	 */
	findField: function(name) {
		var masterPnl = this.masterPnl;
		if (Ext.isEmpty(masterPnl)) return null;
		
		return masterPnl.form.findField(name);
	},
	
	/**
	 * 获得当前激活的Grid中所有选中的记录
	 * @param {Boolean} modified 是否获得修改过的记录
	 */
	getSelections: function(modified) {
		var mdl = this,
			item = mdl.getActiveTab();
		
		if (item && Ext.isEmpty(item.getValue)) {
			return item.getValue(false, false, modified == null ? true : false);
		}
		
		return [];
	},
	
	/**
	 * 获得选中记录(单选)
	 * @param {Boolean} modified 是否获得修改过的记录
	 */
	getSelected: function(modified) {
		var selections = this.getSelections(modified);
		return selections.length > 0 ? selections[0] : null;
	},
	
	/**
	 * 获得主表的主键名称
	 * @return {String}
	 */
	getKeyColumn: function() {
		var mdl = this,
			masterPnl = mdl.masterPnl;
			
		return masterPnl ? masterPnl.getKeyColumn() : null;
	},
	
	/**
	 * 获得主表的主键值
	 * @return {String}
	 */
	getKeyValue: function() {
		var mdl = this,
			masterPnl = mdl.masterPnl,
			
			keyValue = mdl[mdl.getKeyColumn()];

		if (Ext.isEmpty(keyValue, false))
			return masterPnl ? masterPnl.getKeyValue() : null;
		else
			return keyValue;
	},
	
	/**
	 * 设置维护窗口数据是否只读
	 */
	setReadOnly: function(readOnly) {
		this.maintainPanels.each(function(item, index) {
			item.setReadOnly(readOnly);
		});
	},
	
	/**
	 * 创建默认的URL
	 */
	createDefaultUrl: function() {
		var mdl = this,
			masterPnl = mdl.masterPnl || {},
			model = mdl.model || masterPnl.model;
		
		if (Ext.isEmpty(mdl.loadDataUrl, false))
			mdl.loadDataUrl = model;
			
		if (Ext.isEmpty(mdl.saveUrl, false))
			mdl.saveUrl = model + '/save';
			
		if (Ext.isEmpty(mdl.removeUrl, false))
			mdl.removeUrl = model + '/remove';
			
	},
	
	/////////////////////////////////////////////////////////////

	onModuleRender: function(mdl) {
		mdl.setDefaultAction(mdl.save, mdl);
		
		/*var masterPnl = mdl.masterPnl || {},
			model = mdl.model || masterPnl.model;
			
		// 创建默认URL
		if (Ext.isEmpty(mdl.loadDataUrl, false))
			mdl.loadDataUrl = model;
			
		if (Ext.isEmpty(mdl.saveUrl, false))
			mdl.saveUrl = model + '/save';
			
		if (Ext.isEmpty(mdl.removeUrl, false))
			mdl.removeUrl = model + '/remove';*/
		mdl.createDefaultUrl();
			
		if (mdl.readOnly) {
			mdl.setReadOnly(true);
			
		} 
		
		framework.modules.MaintainModule.superclass.onModuleRender.call(this, mdl);
	},
	
	onTabChange: function(tabPnl, item) {
		item.loadData();
	},
	
	/////////////////////////////////////////////////////////////
	
	/**
	 * @interface
	 * @description 获得主表(North布局)的Panel
	 * @return {Object} 
	 */
	master: Ext.emptyFn,
	
	/**
	 * @interface
	 * @description 获得明细表(Center布局)的Panel
	 * @return {Array<Object>}
	 */
	detailer: Ext.emptyFn
});