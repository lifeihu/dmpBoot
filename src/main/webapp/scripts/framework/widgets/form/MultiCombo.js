_package('framework.widgets.form');

framework.widgets.form.MultiCombo = Ext.extend(Ext.form.ComboBox, {
	checkField: 'checked',
	separator: ',',
	// allowBlank: false,
	
	chooseAll: false,
	
	initComponent: function() {
		if (!this.valueField && this.store)
			this.valueField = this.store.fields.get(0).name;
		
		if (!this.displayField && this.store)
			this.displayField = this.store.fields.get(1).name;
		
		if(!this.tpl) {
			this.tpl = '<tpl for=".">'
				+ '<div class="x-combo-list-item">'
				+ '<img src="' + Ext.BLANK_IMAGE_URL + '" '
				+ 'class="'
				+ '{[values.blank ? "" : "x-multi-check"]} x-multi-check-'
				+ '{[values.' + this.checkField + ' ? "checked" : "unchecked"' + ']}"> '
				+ '{[values.'+ this.displayField + ']}'
				+ '</div></tpl>';
		}

		framework.widgets.form.MultiCombo.superclass.initComponent.apply(this, arguments);
		
		this.on({
			blur: this.onRealBlur,
			scope: this,
			beforequery: this.onBeforeQuery
		});
		
		this.onLoad = this.onLoad.createSequence(function() {
			if(this.el) {
				var v = this.el.dom.value;
				this.el.dom.value = '';
				this.el.dom.value = v;
			}
		});
	},
	
	initEvents: function() {
		framework.widgets.form.MultiCombo.superclass.initEvents.apply(this, arguments);
		this.keyNav.tab = false;

		if (this.chooseAll === true)
			this.selectAll();
	},
	
	clearValue: function() {
		this.value = '';
		this.setRawValue(this.value);
		
		this.store.clearFilter();
		this.store.each(function(r) {
			r.set(this.checkField, false);
		}, this);
		
		if(this.hiddenField) 
			this.hiddenField.value = '';
		
		this.applyEmptyText();
	},
	
	getCheckedDisplay: function() {
		var re = new RegExp(this.separator, "g");
		return this.getCheckedValue(this.displayField).replace(re, this.separator/* + ' '*/);
	},
	
	getCheckedValue: function(field) {
		field = field || this.valueField;
		var c = [];
		
		var snapshot = this.store.snapshot || this.store.data;
		snapshot.each(function(r) {
			if(r.get(this.checkField)) {
				c.push(r.get(field));
			}
		}, this);
		
		return c.join(this.separator);
	},
	
	onBeforeQuery: function(qe) {
		qe.query = qe.query.replace(new RegExp(RegExp.escape(this.getCheckedDisplay()) + '[ ' + this.separator + ']*'), '');
	},
	
	onRealBlur:function() {
		this.list.hide();
		var rv = this.getRawValue();
		var rva = rv.split(new RegExp(RegExp.escape(this.separator) + ' *'));
		var va = [];
		var snapshot = this.store.snapshot || this.store.data;

		// iterate through raw values and records and check/uncheck items
		Ext.each(rva, function(v) {
			snapshot.each(function(r) {
				if(v === r.get(this.displayField)) {
					va.push(r.get(this.valueField));
				}
			}, this);
		}, this);

		this.setValue(va.join(this.separator));
		this.store.clearFilter();
	},
	
	onSelect:function(record, index) {
		if (this.fireEvent('beforeselect', this, record, index) !== false) {
		
			// toggle checked field
			record.set(this.checkField, !record.get(this.checkField));
			
			// display full list
			if(this.store.isFiltered()) {
				this.doQuery(this.allQuery);
			}
		
			// set (update) value and fire event
			var v = record.get('blank') ? null : this.getCheckedValue();
			this.setValue(v);
			
			if (Ext.isEmpty(v, false)) {
				this.collapse();
				return;
			}
			
			this.fireEvent('select', this, record, index);
			
			
		}
	},
	
	setValue:function(v) {
		if (!Ext.isEmpty(v, false)) {
			v = '' + v;
			if(this.valueField) {
				this.store.clearFilter();
				this.store.each(function(r) {
					var checked = !(!v.match(
						'(^|' + this.separator + ')' + 
						RegExp.escape(r.get(this.valueField)) +
						'(' + this.separator + '|$)'));
						
					r.set(this.checkField, checked);
				}, this);
				
				this.value = this.getCheckedValue();
				this.setRawValue(this.getCheckedDisplay());
				if(this.hiddenField) {
					this.hiddenField.value = this.value;
				}
				
			} else {
				this.value = v;
				this.setRawValue(v);
				if(this.hiddenField) {
					this.hiddenField.value = v;
				}
			}
			
			if(this.el) {
				this.el.removeClass(this.emptyClass);
			}
			
			this.validate();
			
		} else {
			this.clearValue();
		}

		if (this.taskSetValue) {
	        Ext.TaskMgr.stop(this.taskSetValue);
	        this.taskSetValue = null;
        }
	},

	selectAll:function() {
		if (this.store.loaded !== true && this.store.getCount() == 0) {
        	if (this.taskSetValue == null) {
	        	this.taskSetValue = {
	        		run: this.selectAll,
	        		interval: 10,
	        		scope: this
	            };
	        	
	        	Ext.TaskMgr.start(this.taskSetValue);
        	}
        	
        	return;
        }
        
        if (this.taskSetValue) {
	        Ext.TaskMgr.stop(this.taskSetValue);
	        delete this.taskSetValue;
        }
		
		this.store.each(function(record){
			// toggle checked field
			record.set(this.checkField, true);
		}, this);
		
		//display full list
		this.doQuery(this.allQuery);
		this.setValue(this.getCheckedValue());
	},

	deselectAll:function() {
		this.clearValue();
	}
});

Ext.reg('multicombo', framework.widgets.form.MultiCombo);