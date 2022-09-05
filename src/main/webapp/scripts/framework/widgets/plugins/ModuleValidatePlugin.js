_package('framework.widgets.plugins');

framework.widgets.plugins.ModuleValidatePlugin = Ext.extend(Ext.util.Observable, {
	
	/**
	 * 需要被校验的字段
	 * @type Array<Ext.form.Field>
	 */
	validateFields: [],
	
	/**
	 * 所有检验错误信息
	 */
	errors: null,
	
	init: function(module) {
		this.errors = new Ext.util.MixedCollection();
		
		this.module = module;
		
		module.getModuleValidatePlugin = this.getModuleValidatePlugin.createDelegate(this);
		
		module.on({
			modulerender: this.onInitValidateFields,
			beforedestroy: this.onModuleBeforeDestroy,
			scope: this
		});
	},
	
	/**
	 * 监听需要校验的Field
	 * @param {Ext.form.Field/Ext.util.MixedCollection<Ext.form.Field>/Array<Ext.form.Field>} fields
	 */
	monitoringValidateField: function(field) {
		if (Ext.isEmpty(field)) return;
		
		var plg = this;
		
		if (field instanceof Ext.util.MixedCollection) {
			field.each(function(f) { plg.monitoringValidateField(f); });
			
		} else if (Ext.isIterable(field)) {
			Ext.iterate(field, function(f) { plg.monitoringValidateField(f); });
			
		} else if (field instanceof Ext.form.Field) {
			field.on({
				valid: plg.onFieldValidation,
				invalid: plg.onFieldValidation,
				scope: plg
			});
			
			plg.validateFields.push(field);
		}
	},
	
	/**
	 * 检验数据
	 * @param {} validFn
	 * @param {} scope
	 */
	validate: function(validFn, scope) {
		if (Ext.isEmpty(validFn)) return true;
		
		var plg = this,
			success = validFn.apply(scope, []);

		if (!success) {
			plg.showErrors();
			
		} else {
			plg.clearError();
		}

		return success;
	},
	
	/**
	 * 添加错误信息
	 * @param {Object} err
	 * 		@param key
	 * 		@param msg
	 * 		@param grid
	 * 		@param field
	 * 		@param header
	 * 		@param rowIndex
	 * 		@param colIndex
	 * 		@param callback
	 * 		@param scope
	 */
	addError: function(err) {
		if (!err) return;
		
		if (typeof err == 'string')
			err = {msg: err};
		
		if (typeof err == 'function')
			err = {callback: err};
			
		if (err.grid) {
			if (typeof err.grid == 'string')
				err.grid = Ext.getCmp(err.grid);
		}
		
		err.key = err.key || Ext.id(null, 'pf-module-validate-error-');

		this.errors.add(err.key, {
			msg: err.msg,
			key: err.key,
			field: err.field,
			header: err.header || err.field ? err.field.fieldLabel || null : null,
			rowIndex: err.rowIndex || (err.field ? err.field.rowIndex : null),
			colIndex: err.colIndex || (err.field ? err.field.colIndex : null),
			grid: err.grid,
			callback: err.callback,
			scope: err.scope
		});
	},
	
	/**
	 * 删除指定错误信息
	 * @param {} key
	 */
	removeError: function(key) {
		if (this.errors != null) {
			this.errors.removeKey(key);
		}
	},
	
	/**
	 * 清除错误信息
	 */
	clearError: function() {
		this.errors.clear();
		if (this.invalidEl) {
			var el = this.invalidEl;
			el.update('');
			el.hide();
		}
	},
	
	updateErrorList: function() {
		var errors = this.errors;
		
		if (errors.getCount() > 0) {
			var msg = '<ul>';
			errors.each(function(err) {
				var fld = err.field;
				var key = err.key;
				var header = err.header;
				var rowIndex = err.rowIndex;
				var colIndex = err.colIndex;
				var prefix = '';

				if (!Ext.isEmpty(rowIndex, false)) {
					prefix += '<span style="color:blue;">[' + (rowIndex + 1) + '行, ' + colIndex + '列]</span> ';
				}
				
				if (!Ext.isEmpty(header, false)) {
					prefix += '"<b>' + header + '</b>" ';
				}
				
				msg += '<li id="err-' + key + '"><a href="#">' + prefix + '<span style="color:red;">' + err.msg + '</span></a></li>';
			});
			this.getInvalidEl().update(msg + '</ul>');
		} else
			this.getInvalidEl().update('');
	},
	
	getInvalidEl: function() {
		var plg = this,
			invalidEl = plg.invalidEl;
		
		if (!invalidEl) {
			invalidEl = plg.invalidEl = Ext.DomHelper.append(Ext.getBody(), {
				cls: 'invalid-error-list',
				title: '单击右键关闭错误提示信息'
			}, true);
			invalidEl.setOpacity(.75);
			
			invalidEl.on('click', function(e) {
				var t = e.getTarget('li', 10);
				if (t) {
					var key = t.id.split('err-')[1];
					var err = plg.errors.get(key);
					
					if (err.field) {
						if (err.grid) {
							err.grid.startEditing(err.rowIndex, err.colIndex);
						} else {
							err.field.focus(true, 1);
						}
					}
					
					if (typeof err.callback == 'function')
						Ext.callback(err.callback, err.scope || plg);
				}
			}, null, {stopEvent: true});
			
			invalidEl.on('contextmenu', function() {
				plg.clearError();
			}, null, {stopEvent: true});
		}

		return invalidEl;
	},
	
	showErrors: function() {
		if (this.errors.getCount() == 0) return;

		this.updateErrorList();
		
		var el = this.getInvalidEl();
		el.show();
		el.alignTo(Ext.getBody(), 'tl-tl?');
	},
	
	getModuleValidatePlugin: function() {
		return this;
	},
	
	////////////////////////////////////////////////////
	
	onInitValidateFields: function(mdl) {
		this.monitoringValidateField(mdl.getMonitoringValidateField());
	},
	
	onFieldValidation: function(fld, msg) {
		var key = framework.keyString(this.module.id, fld.id, fld.rowIndex, fld.colIndex);
		
		if (typeof msg == 'string') {
			this.addError({
				key: key,
				msg: msg,
				field: fld,
				grid: fld.grid
			});
		} else {
			this.removeError(key);
		}
	},
	
	onModuleBeforeDestroy: function() {
		var plg = this,
			mdl = plg.module;

		framework.debug('before destroy validate plugin.');
			
		if (mdl) {
			mdl.un('modulerender', plg.onInitValidateFields, plg);
		}
		
		if (plg.invalidEl) {
			plg.invalidEl.removeAllListeners();
			Ext.destroy(plg.invalidEl);
		}
		
		plg.errors.clear();
		delete plg.errors;
	}
});

Ext.preg('modulevalidateplugin', framework.widgets.plugins.ModuleValidatePlugin);