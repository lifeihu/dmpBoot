_package('framework.modules');

_import([
	'framework.widgets.plugins.FormPaginationPlugin'
]);

framework.modules.MaintainFormModeModule = MaintainFormModeModule = Ext.extend(framework.core.Module, {
	id: 'framework.modules.MaintainFormModeModule',
	border: false,
	margin: '1',
	monitorValidate: true,
	
	/**
	 * @property grid
	 * @type Ext.grid.GridPanel
	 */
	
	/**
	 * @property record
	 * @type Ext.data.Record
	 * @description 当前编辑的记录
	 */
	
	/**
	 * @property recordIndex
	 * @type Number
	 * @description 当前编辑记录的序号
	 */
	
	initModule: function() {
		var mdl = this,
			g = mdl.grid,
			
			record = g.getSelectionModel().getSelected();
			
		Ext.apply(mdl, {
			record: record,
			recordIndex: g.store.indexOf(record)
		});
		
		mdl.addEvents(
			/**
			 * @event loaddatacomplete
			 * @param framework.modules.MaintainFormModeModule
			 * @param Object data
			 */
			'loaddatacomplete'
		);
		
		framework.modules.MaintainFormModeModule.superclass.initModule.call(this);
	},
	
	/**
	 * 创建数据源
	 * @return {Ext.data.Store}
	 */
	createStore: function() {
		var plg = this,
			g = plg.grid;

		return new Ext.data.Store({
			autoLoad: {params: {
				start: plg.recordIndex
			}},
			
			proxy: new Ext.data.MemoryProxy(g.getData(false, false)),
			reader: new Ext.data.JsonReader({}, g.getFields())
		});
	},
	
	getFields: function() {
		var mdl = this,

			g = mdl.grid,
			cm = g.getColumnModel(),
			
			readOnly = g.readonly,
			columnCount = cm.getColumnCount(),
			
			hiddenFields = g.hiddenFields,
			fields = [{name: g.getKeyColumn(), xtype: "hidden"}];
			
		if (!Ext.isEmpty(hiddenFields)) {
			Ext.each(hiddenFields, function(hf) {
				fields.push({
					xtype: 'hidden',
					name: hf
				});
			});
		}

		for (var i = 1; i < columnCount; i++) {
			var field, 
				columnHeader = cm.getColumnHeader(i),
				dataIndex = cm.getDataIndex(i),
				editor = cm.getCellEditor(i, mdl.recordIndex);
				
			if (!Ext.isEmpty(editor)) {
				var ef = editor.field;
				
				field = Ext.ComponentMgr.create(Ext.apply(ef.initialConfig, {
					fieldLabel: columnHeader,
					anchor: '99%',
					name: dataIndex,
					hiddenName: dataIndex,
					
					listeners: {
						render: {
							fn: function(f) { 
								f.setReadOnly(readOnly); 
							},
							single: true
						}
					}
				}), ef.getXType());
				
			} else {
				var cfg = {
					fieldLabel: columnHeader,
					name: dataIndex,
					anchor: "99%",
					readOnly: true
				};
				
				field = Ext.ComponentMgr.create(cfg, cm.isHidden(i) ? 'hidden' : 'textfield');
				// field = cm.isHidden(i) ? new Ext.form.Hidden(cfg) : new Ext.form.TextField(cfg);
				
			}
			
			// TODO 有需要再解除注释
			/*if (field.getXType() == "pfsearchfield") {
				if (!readOnly) {
					var bsw = field.initialConfig.chooseWindow;
					var backfill = bsw ? bsw.backfill : null;
					if (backfill) {
						backfill.cmp = "form-mode-" + this.id;
					}
				}
			}*/
			
			fields.push(field);
		}
		
		return fields;
	},
	
	center: function() {
		var mdl = this;
		
		return {
			xtype: 'form',
			labelWidth: 80,
			autoScroll: true,
			
			store: this.createStore(),
			
			plugins: Ext.ComponentMgr.createPlugin({
				ptype: 'formpaginationplugin'
			}),
			
			listeners: {
				beforechange: mdl.onPagingBeforeChange,
				change: {
					fn: mdl.onPagingChange,
					delay: 1
				},
				scope: mdl
			},
			
			items: mdl.getFields()
		};
	},
	
	save: function() {
		var mdl = this;
		
		if (mdl.saveToRecord(mdl.record))
			mdl.moduleWindow.close();
	},
	
	saveToRecord: function(record) {
		var mdl = this,
			form = mdl.centerPnl.form,
			
			plgValidate = mdl.getModuleValidatePlugin();

		if (!plgValidate.validate(form.isValid.createDelegate(form))) return false;

		var data = form.getValues();
		Ext.iterate(data, function(name) {
			record.set(name, data[name]);
		});
		
		mdl.centerPnl.store.proxy.data = mdl.grid.getData(false, false);
		
		return true;
	},
	
	/**
	 * 获得需要被校验的字段
	 * @return {Ext.form.Field/Ext.util.MixedCollection<Ext.form.Field>/Array<Ext.form.Field>} 
	 */
	getMonitoringValidateField: function() {
		return this.centerPnl.form.items;
	},
	
	/////////////////////////////////////////////////////
	
	onPagingBeforeChange: function() {
		var mdl = this,
			g = mdl.grid;
			
		if (g.readonly) return;
		
		var paging = mdl.centerPnl.getFormPaginationPlugin().paging,
			pageData = paging.getPageData();
			
		return mdl.saveToRecord(g.store.getAt(pageData.activePage - 1));
	},
	
	onPagingChange: function(paging, pageData) {
		var mdl = this,
			moduleWindow = mdl.moduleWindow;

		if (Ext.isEmpty(moduleWindow)) return;
		
		var g = mdl.grid,
			
			activePage = pageData.activePage,
			record = mdl.record = g.store.getAt(activePage - 1);
			
		moduleWindow.setTitle(String.format(i18n('megp.title.form.window'), 
			g.readonly ? i18n('btn.view') : i18n('btn.edit'),
			activePage
		));
		
		mdl.fireEvent('loaddatacomplete', mdl, record.data);
	}
});