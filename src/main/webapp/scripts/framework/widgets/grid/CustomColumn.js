_package('framework.widgets.grid');

/*_import([
	'framework.modules.ImageViewerModule'
]);*/

framework.widgets.grid.CustomColumn = Ext.extend(Ext.grid.Column, {
	align: 'center',
	
	trueText: '是',
	falseText: '否',
	
	/**
	 * 布尔列(只要值为true或false时为自动便该类型列的渲染方式)
	 * @type Boolean
	 */
	booleanColumn: true,
	
	/**
	 * @cfg format
	 * @type String
	 * @description 日期格式化字符(如果该属性不为空时则默认认为该列为时间列)
	 */
	format: null,
	
	/**
	 * 日期列
	 * @type Boolean
	 */
	dateColumn: false,
	
	/**
	 * 时间列
	 * @type Boolean
	 */
	timeColumn: false,
	
	/**
	 * 日期时间列
	 * @type Boolean
	 */
	dateTimeColumn: false,

	/**
	 * @cfg storeColumn
	 * @type {Ext.data.Store/Object}
	 * 		@property Ext.data.Store store
	 * 		@property String valueField
	 * 		@property String displayField
	 */
	storeColumn: false,
	
	/**
	 * @cfg store
	 * @type Ext.data.Store
	 * @description 该列可以替换storeColumn = Ext.data.Store
	 */
	
	/**
	 * @cfg handler
	 * @type Function
	 */
	
	/**
	 * @cfg linkCellSelector
	 * @type String
	 */
	linkCellSelector: 'x-customcolumn-cell-link',
	
	/**
	 * @private 
	 * @property invalidateLinkCells
	 * @type Object
	 * @description 定义本列哪几行链接无效
	 */
	invalidateLinkCells: {},

	constructor: function(cfg) {
		var me = this;

		framework.widgets.grid.CustomColumn.superclass.constructor.call(me, cfg);
		
		me.id = 'cc-' + me.id + '-' + new Date().getTime();
		
		me.initDateColumn();
		me.initStoreColumn();
		me.initHandler();
		
		me.renderer = function(value, metaData, record, row, col, store, grid) {
			/**
			 * 设置当前自定义列对应的Grid
			 */
			me.grid = grid;
			
			me.invalidateLinkCells = {};
			
			var newValue = null;
			
			if (!Ext.isEmpty(value, false)) {
				var renderer = null;

				// 日期、时间列
				if (!Ext.isEmpty(me.format, false)) {
					renderer = me.dateRenderer;
				}
				
				// Store列				
				if (Ext.isObject(me.storeColumn)) {
					renderer = me.storeRenderer;
				}
				
				// Boolean列
				if (me.booleanColumn && typeof value === 'boolean') {
					renderer = me.booleanRenderer;
				}
				
				//
				
				if (Ext.isFunction(renderer)) {
					newValue = renderer.apply(me, arguments);
				} else {
					newValue = value;
				}
			}
			
			// 执行自定义的渲染方法
			if (Ext.isFunction(cfg.renderer)) {
				/**
				 * @method renderer
				 * @param newValue 通过CustomColumn渲染后的值
				 * @param oldValue 原始值
				 * @param metaData
				 * @param record
				 * @param row
				 * @param col
				 * @param store
				 * @param grid
				 * @param cc
				 */
				newValue = cfg.renderer(newValue, value, metaData, record, row, col, store, grid, me);
			}
			
			// 渲染链接列
			if (!Ext.isEmpty(me.handler)) {
				newValue = me.handlerRenderer(newValue, metaData, record, row, col, store, grid);
			}
			
			if (record.get('active') === false && Ext.isEmpty(me.handler, false)) {
				metaData.css += ' x-record-unactive';
			}
			
			return newValue;
		};
	},
	
	/**
	 * 初始化日期、时间列
	 */
	initDateColumn: function() {
		var me = this;
		
		if (me.dateColumn === true) {
			me.format = 'Y-m-d';
			me.width = 100;
			
		} else if (me.timeColumn === true) {
			me.format = 'H:i:s';
			me.width = 100;
			
		} else if (me.dateTimeColumn === true) {
			me.format = 'Y-m-d H:i:s';
			me.width = 150;
		}
	},
	
	/**
	 * 初始化Store列
	 */
	initStoreColumn: function() {
		var me = this,
			store = me.store,
			editor = me.getCellEditor(),
			storeColumn = me.storeColumn;
		
		if (!Ext.isEmpty(store)) {
			me.storeColumn = storeColumn = store;
		}
		
		if (!Ext.isEmpty(editor) && !storeColumn) {
			storeColumn = editor.field.store;
		}
		
		if (storeColumn) {	
			if (storeColumn instanceof Ext.data.Store) {
				storeColumn = {store: storeColumn};
			}
			
			var store = storeColumn.store,
				fields = store.fields,
				
				valueField = storeColumn.valueField,
				displayField = storeColumn.displayField;
				
			if (fields.getCount() == 1) { 
				storeColumn.valueField = storeColumn.displayField = fields.get(0).name;
				
			} else {
				if (Ext.isEmpty(valueField, false)) {
					storeColumn.valueField = fields.get(0).name || 'value';
				}
				
				if (Ext.isEmpty(displayField, false)) {
					storeColumn.displayField = fields.get(1).name || 'name';
				}
			}
			
			me.storeColumn = storeColumn;
		}
	},
	
	/**
	 * 
	 */
	initHandler: function() {
		var me = this;
		
		if (!Ext.isEmpty(me.handler)) {
			if (!Ext.isEmpty(me.iconCls, false)) {
				me.width = 20;
			}
			
			me.on({
				click: me.onCellClick,
				scope: me
			});
		}
	},
	
	/**
	 * 渲染布尔列的值
	 * @param {} v
	 */
	booleanRenderer: function(v) {
		var me = this;
		
		if (v === true) {
			return me.trueText;
		} else if (v === false) {
			return me.falseText;
		} else {
			return null;
		}
	},
	
	/**
	 * 渲染日期、时间列的值
	 * @param {} value
	 * @return {}
	 */
	dateRenderer: function(v) {
		v = v.replace(/[-]/g,'/');
		
		return Ext.util.Format.date(v, this.format);
	},
	
	/**
	 * 渲染Store列的值
	 * @param {} value
	 * @param {} metaData
	 * @param {} record
	 * @param {} row
	 * @param {} col
	 * @param {} store
	 * @param {} grid
	 */
	storeRenderer: function(value, metaData, record, row, col, store, grid) {
		var me = this,
		
			storeColumn = me.storeColumn,
			ds = storeColumn.store,
			valueField = storeColumn.valueField,
			displayField = storeColumn.displayField;
		
		if (Ext.isObject(value)) {
			return value[displayField];
		} else {
			var r = ds.queryUnique(valueField, value);
			return r ? r.get(displayField) : null;
		}
	},
	
	/**
	 * 渲染附件列
	 * @param {} value
	 * @return {}
	 */
	attachmentRenderer: function(value) {
		return Ext.isEmpty(value) ? null : value['name'];
	},
	
	/**
	 * 链接列
	 * @param {} v
	 * @param {} metaData
	 * @param {} record
	 * @param {} row
	 * @param {} col
	 * @param {} store
	 * @param {} grid
	 */
	handlerRenderer: function(v, metaData, record, row, col, store, grid) {
		var me = this,
			
			iconCls = me.iconCls,
			text = me.text,
		
			linkCell = me.isCellLink(row, col);

		if (linkCell === true) {
			metaData.css += ' ' + me.linkCellSelector;
		}
		
		if (linkCell && !Ext.isEmpty(text, false)) {
			v = text;
		}
		
		if (Ext.isEmpty(v, false)) {
			v = '&#160;';
		}
		
		if (!Ext.isEmpty(iconCls, false) && linkCell === true) {
			metaData.css += ' x-customcolumn-cell-icon ' + iconCls;
		}
		
		return v;
	},
	
	////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 判断指定行,列是否可以点击
	 * @param {Number/HTMLElement} row
	 * @param {Number} col
	 */
	isCellLink: function(row, col) {
		var me = this;

		if (Ext.isNumber(row)) {
			return me.invalidateLinkCells[framework.keyString(me.id, row, col)] !== true;
		} else {
			return Ext.fly(row).parent().hasClass(me.linkCellSelector);
		}
	},
	
	/**
	 * 设置指定单元格是否可链接
	 * @param {Number/HTMLElement} row
	 * @param {Number} col
	 * @param Boolean allow
	 */
	setCellLink: function(row, col, allow) {
		var me = this;
			
		if (Ext.isNumber(row)) {
			var key = framework.keyString(me.id, row, col);

			if (allow === false) {
				me.invalidateLinkCells[key] = true;
			} else {
				delete me.invalidateLinkCells[key];
			}
			
		} else {
			allow = col;
			
			var el = Ext.get(row),	
				parent = el.parent(),
				
				linkCellSelector = me.linkCellSelector;
				
			if (allow === false) {
				parent.removeClass(linkCellSelector);
			} else {
				parent.addClass(linkCellSelector);
			}
		}
		
	},
	
	/**
	 * 重载方法,为了把colIndex传入事件
	 */
	processEvent : function(name, e, grid, rowIndex, colIndex){
        return this.fireEvent(name, this, grid, rowIndex, colIndex, e);
    },
    
	//////////////////////////////////////////////////////////////////////////
	
	onCellClick: function(column, grid, row, col, e) {
		var me = this;

		if (!me.isCellLink(e.target)) {
			return;
		}
		
		var handler = me.handler,
		
			store = grid.store,
			record = store.getAt(row);
		
		/*if (me.attachmentColumn && grid.readOnly !== true) {
			return;
		}*/
			
		if (Ext.isString(handler)) {
			var scope = grid.ownerCt || me.scope,
				moduleMethod = scope[handler];
			
			if (Ext.isFunction(moduleMethod)) {
				handler = function() {
					var params = {};

					if (scope instanceof framework.core.Module) {
						var modelId = scope.getModelId(scope.model);
						params[modelId] = record.get(modelId);
					}

					moduleMethod.call(scope, params);
				};
			}
		}
		
		if (!Ext.isEmpty(handler)) {
			handler.call(me.scope || me, record, me, grid, row, col, e);
		}
	}
	
});
Ext.grid.Column.types.customcolumn = framework.widgets.grid.CustomColumn;