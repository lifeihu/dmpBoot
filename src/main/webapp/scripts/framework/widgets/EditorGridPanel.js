_package('framework.widgets');

framework.widgets.EditorGridPanel = EditorGridPanel = Ext.extend(Ext.grid.EditorGridPanel, {
	clicksToEdit: 1,
    
    /**
     * @cfg module
     * @type {framework.core.Module}
     */
    
    /**
     * @cfg fields
     * @type {Array<String>}
     * @description EditorGrid字段列表
     */
    
    /**
     * @private
     * @cfg hiddenFields
     * @type {Array<String>}
     * @description EditorGrid隐藏字段列表
     */
    
    /**
     * @cfg removeUrl
	 * @type {String} 
	 * @description 删除记录请求的地址
     */
    
    /**
     * @cfg removeParams
     * @type {Object}
     * @description 删除记录的自定义参数
     */
    
    /**
     * @cfg chooseWindow
     * @type {Object}
     * @description 允许批量添加数据(只在rowMode为false起作用)
     */
    
    /**
     * @requires
     * @cfg keyColumn
     * @type {String}
     * @description EditorGrid中主键名称
     */
    
    /**
     * @cfg enableValid
     * @type Boolean
     * @description 是否允许校验EditorGrid数据
     */
    
    /**
     * @cfg validEmpty
     * @type Boolean
     * @description 是否校验EditorGrid中的空数据
     */
    
    /**
     * @cfg defaults
     * @type {Object}
     * @description 新数据的默认值
     */
    defaults: {
    	actived: true,
		created: new Date().format('Y-m-d H:i:s'),
		updated: new Date().format('Y-m-d H:i:s'),
		createdBy: USER_NAME,
		updatedBy: USER_NAME
    },
    
    /**
     * @cfg readOnly
     * @type {Boolean}
     * @description 是否只读
     */
    readOnly: false,
    
    /**
     * @cfg buttonAddItemDisabled
     * @type {Boolean}
     * @description 新增明细按钮是否禁用
     */
    buttonAddItemDisabled: false,
    
    /**
     * @cfg buttonAddItemHidden
     * @type {Boolean}
     * @description 新增明细按钮是否隐藏
     */
    buttonAddItemHidden: false,
    
    /**
     * @cfg buttonRemoveItemDisabled
     * @type {Boolean}
     * @description 删除明细按钮是否禁用
     */
    buttonRemoveItemDisabled: false,
    
    /**
     * @cfg buttonAddItemHidden
     * @type {Boolean}
     * @description 删除明细按钮是否隐藏
     */
    buttonRemoveItemHidden: false,
    
    /**
     * @cfg buttonFormModeDisabled
     * @type {Boolean}
     * @description 表单编辑按钮是否禁用
     */
    buttonFormModeDisabled: false,
    
    /**
     * @cfg buttonAddItemHidden
     * @type {Boolean}
     * @description 表单编辑按钮是否隐藏
     */
    buttonFormModeHidden: true,
    
    /**
     * @cfg rowMode
     * @type {Boolean}
     * @description true 行模式编辑 false 表单模式编辑
     */
    rowMode: true,
    
    /**
     * @cfg modifiedRow
     * @type {Boolean}
     * @description 是否只取得已被编辑过的数据
     */
    modifiedRow: true,
    
    /**
     * @cfg emptyRemoveRecordMsg
     * @type {String}
     * @description 删除动作时没有选中记录的提示信息
     */
    emptyRemoveRecordMsg: '请选择需要删除的记录!',
    
    /**
     * @cfg confirmRemoveRecordMsg
     * @type {String}
     * @description 确认删除动作时的提示信息
     */
    confirmRemoveRecordMsg: '是否删除选中的 {0} 条记录?',
    
    /**
     * @cfg validEmptyMsg
     * @type {String}
     * @description 明细数据为空时的提示信息
     */
    validEmptyMsg: '"{0}" 不允许为空!',
    
    /**
     * @deprecated
     * @cfg formModuleCache
     * @type {Boolean}
     * @description 窗口模式编辑时是否缓存Module
     */
    // formModuleCache: true,
    
    initComponent: function() {
    	this.validEmptyMsg = String.format(this.validEmptyMsg, this.title);
    	
    	var store = this.store || new Ext.data.JsonStore({
	        root: "list",
	        fields: this.getFields(this.columns)
	    });

		var sm = new Ext.grid.CheckboxSelectionModel();
		this.columns = [sm].concat(this.columns);

		Ext.apply(this, {
			sm: sm,
			/*cm: new Ext.grid.ColumnModel([
			    sm
			].concat(this.cm)),*/

			store: store,
		    
		    // 工具条按钮
		    tbar: this.getButtons()
		});

		this.addEvents(
			/**
			 * @event beforeadd
			 * @param {framework.widgets.EditorGridPanel} p
			 * @return {Boolean}
			 */
			'beforeadd',
			
			/**
			 * @event addcomplete
			 * @param {framework.widgets.EditorGridPanel} p
			 */
			'addcomplete',
			
			/**
			 * @event beforeremove
			 * @param {framework.widgets.EditorGridPanel} p
			 * @param {Ext.data.Record} record
			 * @param {Object} params
			 * @return {Boolean}
			 */
			'beforeremove',
			
			/**
			 * @event removecomplete
			 * @param {framework.widgets.EditorGridPanel} p
			 * @param {Object} data
			 */
			'removecomplete'
		);
		
		framework.widgets.EditorGridPanel.superclass.initComponent.call(this);
		
		this.on('beforeedit', this.onBeforeEdit, this);
    },
    
    onRender: function() {
    	framework.widgets.EditorGridPanel.superclass.onRender.apply(this, arguments);
    	
    	this.store.pruneModifiedRecords = true;
    },
    
    onBeforeEdit: function() {
    	return this.readOnly !== true;
    },
    
    /**
	 * 获得主键名称
	 * @return {String}
	 */
	getKeyColumn: function() {
		if (Ext.isEmpty(this.keyColumn, false))
			this.keyColumn = Ext.isEmpty(this.model, false) ? this.module.getModuleId() : this.model + 'Id';
		
		return this.keyColumn;
	},
    
    /**
     * 设置需要被校验的字段
     * @return {Array<Ext.form.Field>}
     */
    getMonitoringValidateField: function() {
    	var fields = [], cm = this.getColumnModel();

		this.editors = {};
		for (var i = 1; i < cm.getColumnCount(); i++) {
			if (cm.isHidden(i)) continue;
			
			var editor = cm.getCellEditor(i);
			if (!editor) continue;
			
			var fld = editor.field.cloneConfig();
			fld.fieldLabel = fld.fieldLabel || cm.getColumnHeader(i);
			fld.colIndex = i;
			fld.grid = this;
			this.editors[cm.getDataIndex(i)] = fld;
			
			fields.push(fld);
		}

		return fields;
    },
    
    /**
     * 获得字段列表
     * @param {Array<Object>} columns
     */
    getFields: function(columns) {
    	if (!columns) return this.fields;
    	
    	var fields = [this.module.getModelId(), 'actived', 'created', 'createdBy', 'updated', 'updatedBy'].concat(this.hiddenFields || []);
    	
    	Ext.iterate(columns, function(c) {
    		var dataIndex = c.dataIndex;
    		if (!Ext.isEmpty(dataIndex, false)) {
    			fields.push(dataIndex);
    		}
    	});
    	
    	this.fields = fields;
    	
    	return fields;
    },
    
    /**
	 * 实例化Record对象
	 * @param {Number} rowIndex
	 * @return {Ext.data.Record}
	 */
	getInstanceRecord: function(rowIndex) {
		var entity = {};
		
		var fields = this.getFields();
		var defaultValues = this.defaults;
		for (var i = 0; i < fields.length; i++) {
			var field = fields[i];
			var value = "";
			
			// 自定义默认值
			var dv = defaultValues[field];
			if (dv) {
				if (typeof dv == "function")
					dv = dv(rowIndex, this);
				
				value = dv;
			}
			
			entity[field] = value;
		}	
	
		return new this.store.recordType(entity);
	},
    
    /**
     * 获得工具条默认按钮
     * @return {Array<Ext.Button>}
     */
    getButtons: function() {
    	var defaultButtons = [];
    	
    	if (this.buttonAddItemHidden === true && this.buttonRemoveItemHidden === true && this.buttonFormModeHidden === true)
    		return;
    	
    	if (!this.buttonAddItemHidden) {
	    	this.BUTTON_ADD = new Ext.Button({
				text: '添加',
				iconCls: "add",
				disabled: this.buttonAddItemDisabled,
				handler: this.addItem.createDelegate(this)
			});
			defaultButtons.push('-', this.BUTTON_ADD);
    	}
    	
    	if (!this.buttonRemoveItemHidden) {
    		this.BUTTON_REMOVE = new Ext.Button({
				text: '删除',
				iconCls: "remove",
				disabled: this.buttonRemoveItemDisabled,
				handler: this.removeItem.createDelegate(this)
			});
			defaultButtons.push(this.BUTTON_REMOVE, '-');	
    	}

    	if (!this.buttonFormModeHidden) {
			this.BUTTON_FORM = new Ext.Button({
				text: '表单编辑',
				iconCls: "form",
				disabled: this.buttonFormModeDisabled,
				handler: this.formMode.createDelegate(this, [])
			});
			defaultButtons.push(this.BUTTON_FORM, '-');
    	}
    	
		var tbuttons = this.tbuttons || [];
		for (var i = 0; i < tbuttons.length; i++) {
			var btn = tbuttons[i];
			
			// 当定义的是数组时第一个元素为按钮配置信息,第二个元素为想插入的位置
			if (btn instanceof Array && btn.length > 0) {
				var index = btn[1] + 6;
				defaultButtons.splice(index, 0, btn[0]);
				
			} else
				defaultButtons.push(btn);

		}
		delete this.tbuttons;

		return defaultButtons;
    },
    
    /**
     * 设置EditorGrid是否只读
     * @param {Boolean} readOnly
     */
    setReadOnly: function(readOnly) {
    	this.readOnly = readOnly;
    	
    	if (this.BUTTON_ADD)
    		this.BUTTON_ADD.setDisabled(readOnly);
    	if (this.BUTTON_REMOVE)
			this.BUTTON_REMOVE.setDisabled(readOnly);
		if (this.BUTTON_FORM)
			this.BUTTON_FORM.setText(readOnly ? '表单浏览' : '表单编辑');
    },
    
    /**
     * 加载EditorGrid数据
     * @param {Object} data
     */
    startLoad: function(data) {
    	this.store.modified = [];
		this.store.loadData({
			result: data.length, 
			list: data
		});
    },
    
    /**
     * 获得EditorGrid数据
     * @param {Boolean} modified
     * @param {Boolean} returnRecord
     * @return {Array<Object/Record>}
     */
    getData: function(modified, returnRecord) {
    	modified = modified == null ? this.modifiedRow : modified;
    	var recs = modified === true ? this.store.getModifiedRecords() : this.store.getRange();
    	if (!recs || recs.length == 0) return recs;

    	if (returnRecord === true)
    		return recs;
    	
		var data = [];
		Ext.iterate(recs, function(rec) {
			data.push(rec.data);
		});
		
		return data;
    },
    
    /**
     * 校验EditorGrid数据
	 * @param framework.widgets.plugins.ModuleValidatePlugin
     */
    validate: function(plg) {
    	if (this.enableValid === false) return true;
    	
		var me = this,
		
			editors = this.editors,
			store = this.store,
			records = this.getData(null, true),
			
			success = true,
			count = store.getCount();
		
		if (count > 0) {
			// 在校验前先将光标移至第一个复先框,
			// 用于解决下拉框、日期框在不离开焦点时点保存无法将修改过的值存入数据库的问题
			me.startEditing(0, 0);
			
		} else {
			if (me.validEmpty !== false) {
				if (plg) {
					plg.addError(me.validEmptyMsg);
				} else {
					Ext.Msg.alert('提示', me.validEmptyMsg);
				}
				
				return false;
			}
		}
		
		for (var i = 0; i < records.length; i++) {
			var record = records[i];
			var rowIndex = store.indexOf(record);
			var fields = record.fields;
			for (var j = 0; j < fields.length; j++) {
				var field = fields.get(j);
				var fieldName = field.name;
				var fld = editors[fieldName];
				if (!fld) continue;
				
				fld.rowIndex = rowIndex;
				fld.setValue(record.get(fieldName));
				if (!fld.isValid())
					success = false;
			}
		}
		
		return success;
    },
    
    /**
     * 往Store中添加记录
     * @param {Number} row
     * @return {Ext.data.Record}
     */
    addRecord: function(row) {
    	var store = this.store;
    	row = row || store.getCount();
    	
    	var rec = this.getInstanceRecord(row);
    	this.stopEditing();
    	store.insert(store.getCount(), rec);
	    this.getSelectionModel().selectLastRow();
	    
	    // 设置焦点默认定位的单元格
	    var cm = this.getColumnModel();
	    for (var i = 1; i < cm.getColumnCount(); i++) {
	    	if (!cm.isHidden(i) && cm.getCellEditor(i)) {
	    		this.startEditing(store.getCount() - 1, i);
	    		break;
	    	}
	    }
	    
	    return rec;
    },
    
    /**
     * 添加明细
     */
    addItem: function() {
    	if (this.fireEvent("beforeadd", this) === false) return;
    	
    	this.startAdd();
    	
    	this.fireEvent('addcomplete', this);
    },
    
    /**
     * 该方法可以被重载
     */
    startAdd: function() {
    	if (!this.chooseWindow) this.addRecord();
    		
    	if (this.rowMode) {
    		// 行编辑
    		if (this.chooseWindow) {
    			if (!(this.chooseWindow instanceof framework.widgets.PFChooseWindow))
    				this.chooseWindow = this.module.createWindow(this.chooseWindow, 'framework.widgets.PFChooseWindow');
    				// this.chooseWindow = new framework.widgets.PFChooseWindow(this.chooseWindow);
    				
    			this.chooseWindow.show();
    		}
    	} else {
    		// 表单编辑
    		this.formMode();
    	}
    },
    
    /**
     * 删除明细
     */
    removeItem: function() {
    	var records = this.getSelectionModel().getSelections();
		
		if (!records || records.length == 0) {
			Ext.MessageBox.alert('提示', this.emptyRemoveRecordMsg);

		} else {
			Ext.MessageBox.confirm('提示', String.format(this.confirmRemoveRecordMsg, records.length), function(btn) {
				if (btn != "yes") return;
				if (Ext.isEmpty(this.removeUrl, false)) return;

				var id = null, 
					ids = [], // 需要被删除的ID
					params = this.removeParams || {},
					keyColumn = this.getKeyColumn();
					
				for (var i = 0; i < records.length; i++) {
					if (this.fireEvent("beforeremove", this, records[i], params) === false) 
						return;
					
					this.store.remove(records[i]);

					if ((id = records[i].get(keyColumn)) > 0)
						ids.push(id);
				}
				params[this.module.getModelId()] = ids.join(',');

				if (ids.length == 0) return;

				Ext.Ajax.request({
					url: this.removeUrl,
					params: params,
					scope: this,
					success: function(response, options) {
						var data;
						
						try {
							data = Ext.decode(response.responseText);
						} catch (e) {
							data = null;
						}

						this.fireEvent("removecomplete", this, data);
					}
				});
			}, this);
		}
    },
    
    /**
     * 切换至表单编辑模式
     */
    formMode: function() {
    	if (this.store.getCount() == 0) return;
		var record = this.getSelectionModel().getSelected();
		if (!record) {
			this.getSelectionModel().selectFirstRow();
			record = this.getSelectionModel().getSelected();
		}
		if (!record) return;
		
		this.rowMode = false;

		this.mcw = this.module.createWindow({
			iconCls: "form",
			width: 550,
			useIframeContainer: false,
			
			module: {
				module: 'framework.modules.MaintainFormModeModule',
				grid: this
			},
			
			listeners: {
				close: function() {
					this.rowMode = true;
				},
				scope: this
			}
		}, 'framework.widgets.MaintainWindow');
		this.mcw.updateOnly();
    },
	
	getFormMode: function() {
		var mdl = this.mcw.getModule();
		if (!mdl) return null;
		
		return mdl.frmFormMode;
	},
	
	getFormModeModule: function() {
		return this.mcw.getModule();
	}
}); 

Ext.reg('editorgridpanel', framework.widgets.EditorGridPanel);