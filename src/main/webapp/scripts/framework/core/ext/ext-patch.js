Ext.override(Ext.form.Field, {	
	anchor: '99%',
	
	setValue: function(v) {
		// =================== add hsm =================== //
		v = (v == "&nbps;" || v == "&#160;" || v == "null" || v == "undefined") ? "" : v;
		// =================== add hsm =================== //
		
		this.value = v;
        if(this.rendered){
            this.el.dom.value = (Ext.isEmpty(v) ? '' : v);
            this.validate();
        }
        return this;
	}, 
	
	markInvalid : function(msg){
		// =================== 将事件放至最上面 =================== //
		msg = msg || this.invalidText;
		this.setActiveError(msg);
		// =================== 将事件放至最上面 =================== //
		
		
		if (this.rendered && !this.preventMark) {
            var mt = this.getMessageHandler();
            if(mt){
                mt.mark(this, msg);
            }else if(this.msgTarget){
                this.el.addClass(this.invalidClass);
                var t = Ext.getDom(this.msgTarget);
                if(t){
                    t.innerHTML = msg;
                    t.style.display = this.msgDisplay;
                }
            }
        }
        
        // =================== 将事件放至最上面 =================== //
        // this.setActiveError(msg);
       	// =================== 将事件放至最上面 =================== //
    },
    
    setFieldLabel: function(fieldLabel) {
    	this.fieldLabel = fieldLabel;
    	
    	if (this.rendered) {
    		this.el.parent().parent().first().dom.innerHTML = fieldLabel + ':';
    	}
    }
});

Ext.override(Ext.form.ComboBox, {
	triggerAction: 'all',
	mode: 'local',
	editable: false,
	
	supportObjectValue: false,
	
	task: null,
	
	/**
	 * 匹配模式 true:匹配任何位置
	 */
	anyMatch: false,
	
	initComponent: function() {
		// ====================== hsm add ====================== //
		if (!Ext.isEmpty(this.displayField) && this.displayField.indexOf('.') != -1) {
			this.tpl = '<tpl for="."><div class="x-combo-list-item">{values.' + this.displayField + '}</div></tpl>';
		}
		
		if (this.forceSelection === true)
			this.lazyInit = false;
		// ====================== hsm add ====================== //

		Ext.form.ComboBox.superclass.initComponent.call(this);
        this.addEvents(
            /**
             * @event expand
             * Fires when the dropdown list is expanded
             * @param {Ext.form.ComboBox} combo This combo box
             */
            'expand',
            /**
             * @event collapse
             * Fires when the dropdown list is collapsed
             * @param {Ext.form.ComboBox} combo This combo box
             */
            'collapse',

            /**
             * @event beforeselect
             * Fires before a list item is selected. Return false to cancel the selection.
             * @param {Ext.form.ComboBox} combo This combo box
             * @param {Ext.data.Record} record The data record returned from the underlying store
             * @param {Number} index The index of the selected item in the dropdown list
             */
            'beforeselect',
            /**
             * @event select
             * Fires when a list item is selected
             * @param {Ext.form.ComboBox} combo This combo box
             * @param {Ext.data.Record} record The data record returned from the underlying store
             * @param {Number} index The index of the selected item in the dropdown list
             */
            'select',
            /**
             * @event beforequery
             * Fires before all queries are processed. Return false to cancel the query or set the queryEvent's
             * cancel property to true.
             * @param {Object} queryEvent An object that has these properties:<ul>
             * <li><code>combo</code> : Ext.form.ComboBox <div class="sub-desc">This combo box</div></li>
             * <li><code>query</code> : String <div class="sub-desc">The query</div></li>
             * <li><code>forceAll</code> : Boolean <div class="sub-desc">True to force "all" query</div></li>
             * <li><code>cancel</code> : Boolean <div class="sub-desc">Set to true to cancel the query</div></li>
             * </ul>
             */
            'beforequery'
        );
        if(this.transform){
            var s = Ext.getDom(this.transform);
            if(!this.hiddenName){
                this.hiddenName = s.name;
            }
            if(!this.store){
                this.mode = 'local';
                var d = [], opts = s.options;
                for(var i = 0, len = opts.length;i < len; i++){
                    var o = opts[i],
                        value = (o.hasAttribute ? o.hasAttribute('value') : o.getAttributeNode('value').specified) ? o.value : o.text;
                    if(o.selected && Ext.isEmpty(this.value, true)) {
                        this.value = value;
                    }
                    d.push([value, o.text]);
                }
                this.store = new Ext.data.ArrayStore({
                    idIndex: 0,
                    fields: ['value', 'text'],
                    data : d,
                    autoDestroy: true
                });
                this.valueField = 'value';
                this.displayField = 'text';
            }
            s.name = Ext.id(); // wipe out the name in case somewhere else they have a reference
            if(!this.lazyRender){
                this.target = true;
                this.el = Ext.DomHelper.insertBefore(s, this.autoCreate || this.defaultAutoCreate);
                this.render(this.el.parentNode, s);
            }
            Ext.removeNode(s);
        }
        //auto-configure store from local array data
        else if(this.store){
            this.store = Ext.StoreMgr.lookup(this.store);
            if(this.store.autoCreated){
                this.displayField = this.valueField = 'field1';
                if(!this.store.expandData){
                    this.displayField = 'field2';
                }
                this.mode = 'local';
            }
        }

        this.selectedIndex = -1;
        if(this.mode == 'local'){
            if(!Ext.isDefined(this.initialConfig.queryDelay)){
                this.queryDelay = 10;
            }
            if(!Ext.isDefined(this.initialConfig.minChars)){
                this.minChars = 0;
            }
        }
		
        // ====================== 默认选中第一项 ====================== //

        var cmb = this,
        	store = cmb.store,
        	
        	/**
        	 * 默认选中第一项
        	 */
        	selectDefaultFirst = function() {
        		if (Ext.isEmpty(cmb.getValue(), false)) {
	        		if (cmb.allowBlank !== false) {
	        			cmb.setValue(null);
	        			
	        		} else if (store.getCount() > 0) {
	        			cmb.onSelect(store.getAt(0), 0);
	        			cmb.selectedIndex = 0;
	        			
	        		}
        		}
        	};
        	
        this.on({
        	render: function() {
        		selectDefaultFirst.timer(function() {
        			return store.loaded === true;
        		});
        	},
        	scope: cmb,
        	single: true
        });
    	
		// ====================== 默认选中第一项 ====================== //
		
		// ====================== valueField: fields[0], displayField: fields[1] ====================== //

		var fields = store.fields;
        if (fields.getCount() == 1) {
        	this.valueField = this.displayField = fields.get(0).name;
        	
        } else {
			if (!this.valueField && store)
				this.valueField = fields.get(0).name;
			
			if (!this.displayField && store)
				this.displayField = fields.get(1).name;
        }

		// ====================== valueField: fields[0], displayField: fields[1] ====================== //
	},
	
	onLoad : function(){
		if(!this.hasFocus){
            return;
        }
        
        if(this.store.getCount() > 0 || this.listEmptyText){
        	// ====================== 如果下拉框不是必填时自动插入一个空白项 ====================== //
        	
        	var first = this.store.getAt(0),
        		isBlank = first.get('blank');
        		
        	if (this.allowBlank !== false && isBlank !== true) {
        		var data = {blank: true};
        		data[this.valueField] = null;
        		data[this.displayField] = this.emptyText || '&#160;';
        		this.store.insert(0, new this.store.recordType(data));
        		
        	} 
        	
        	if (this.allowBlank === false && isBlank === true) {
        		this.store.remove(first);
        	}
        		
        	// ====================== 如果下拉框不是必填时自动插入一个空白项 ====================== //
        	
            this.expand();
            this.restrictHeight();
            if(this.lastQuery == this.allQuery){
                if(this.editable){
                    this.el.dom.select();
                }

                if(this.autoSelect !== false && !this.selectByValue(this.value, true)){
                    this.select(0, true);
                }
            }else{
                if(this.autoSelect !== false){
                    this.selectNext();
                }
                if(this.typeAhead && this.lastKey != Ext.EventObject.BACKSPACE && this.lastKey != Ext.EventObject.DELETE){
                    this.taTask.delay(this.typeAheadDelay);
                }
            }
        }else{
            this.collapse();
        }
    },
    
    selectByValue : function(v, scrollIntoView){
    	// ====================== 支持对象 ====================== //
    	if (Ext.isObject(v))
    		v = v[this.valueField || this.displayField];
    	// ====================== 支持对象 ====================== //
    	
    	if(!Ext.isEmpty(v, true)){
            var r = this.findRecord(this.valueField || this.displayField, v);
            if(r){
                this.select(this.store.indexOf(r), scrollIntoView);
                return true;
            }
        }
        return false;
    },
    
    /**
     * hsm add
     * @return {}
     */
    getHiddenValue: function() {
		return this.hiddenField ? this.hiddenField.value : (this.value ? this.value[this.valueField] : '');
	},
	
	getName: function() {
		if (this.supportObjectValue)
			return this.rendered ? this.name : '';
		else {
			var hf = this.hiddenField;
        	return hf && hf.name ? hf.name : this.hiddenName || Ext.form.ComboBox.superclass.getName.call(this);
		}
	},
	
	onSelect : function(record, index){
		if(this.fireEvent('beforeselect', this, record, index) !== false){
			// ====================== 支持对象 ====================== //
            this.setValue(this.supportObjectValue ? record.data : record.data[this.valueField || this.displayField]);
            // ====================== 支持对象 ====================== //
			
            this.collapse();
            this.fireEvent('select', this, record, index);
        }
    },
    
    beforeBlur : function(){
        var val = this.getRawValue();
        if(this.forceSelection){
            if(val.length > 0 && val != this.emptyText){
               this.el.dom.value = Ext.isDefined(this.lastSelectionText) ? this.lastSelectionText : '';
                this.applyEmptyText();
            }else{
                this.clearValue();
            }
        }else{
        	// =================== remove by hsm ================== //
            /*var rec = this.findRecord(this.displayField, val);
            if(rec){
                val = rec.get(this.valueField || this.displayField);
            }
            this.setValue(val);*/
            // =================== remove by hsm ================== //
        }
    },
    
    setValue: function(v) {
    	if (v == '&#160;') {
    		v = '';
    	}
    	
    	if (Ext.isEmpty(v, false)) {
    		this.lastSelectionText = null;
	        if(this.hiddenField){
	            this.hiddenField.value = null;
	        }
    		Ext.form.ComboBox.superclass.setValue.call(this, null);
	        this.value = null;
    		return;
    	}
    	
		if (this.supportObjectValue) {
			if (typeof v == 'string') {
				var obj = {};
				obj[this.valueField] = obj[this.displayField] = v;
				v = obj;
			}
			var text = v ? v[this.displayField] || '' : '';
			
			this.lastSelectionText = text;
	
			if (this.hiddenField)
				this.hiddenField.value = v ? v[this.valueField] : '';

			Ext.form.ComboBox.superclass.setValue.call(this, text);
			this.value = v;
			return this;
			
		} else {
			if (this.store.loaded !== true && this.store.getCount() == 0) {
				if (!this.task) {
					this.task = {
						run: this.setValue,
		        		interval: 10,
		        		scope: this,
		        		args: [v]
					};
					
					Ext.TaskMgr.start(this.task);
				}

				return;
			}
			
			if (this.task) {
		        Ext.TaskMgr.stop(this.task);
		        this.task = null;
	        }

	        var text = v;
	        if(this.valueField){
	            var r = this.findRecord(this.valueField, v);
	            if(r){
	                text = r.data[this.displayField];
	            }else if(Ext.isDefined(this.valueNotFoundText)){
	                text = this.valueNotFoundText;
	            }
	        }
	        this.lastSelectionText = text;
	        if(this.hiddenField){
	            this.hiddenField.value = Ext.value(v, '');
	        }
	        Ext.form.ComboBox.superclass.setValue.call(this, text);
	        this.value = v;
	        return this;
		}
	},
    
    doQuery : function(q, forceAll){
    	q = Ext.isEmpty(q) ? '' : q;
        var qe = {
            query: q,
            forceAll: forceAll,
            combo: this,
            cancel:false
        };
        if(this.fireEvent('beforequery', qe)===false || qe.cancel){
            return false;
        }
        q = qe.query;
        forceAll = qe.forceAll;
        if(forceAll === true || (q.length >= this.minChars)){
            if(this.lastQuery !== q){
                this.lastQuery = q;
                if(this.mode == 'local'){
                    this.selectedIndex = -1;
                    if(forceAll){
                        this.store.clearFilter();
                    }else{
                    	// ================== 匹配模式 ================ //
                    	var matchField = this.displayField;
                    	if (!this.queryParam && this.queryParam != 'query')
                    		matchField = this.queryParam;
                    	this.store.filter(matchField, q, this.anyMatch);
                    	// ================== 匹配模式 ================ //
                    }
                    this.onLoad();
                }else{
                	// ==================== add by hsm ================= //
                	var p = {};
                	p[this.queryParam] = q;
                	var condition = this.store.baseParams['condition'] || {};
                	this.store.baseParams.condition = Ext.apply(condition, p);
                    this.store.load({
                        params: this.getParams(q)
                    });
                    // ==================== add by hsm ================= //

                    this.expand();
                }
            }else{
                this.selectedIndex = -1;
                this.onLoad();
            }
        }
    },
    
    findRecord : function(prop, value){
        var record;
        if(this.store.getCount() > 0){
            this.store.each(function(r){
            	// ==================== add by hsm ================= //
                // if(r.data[prop] == value){
                if(r.data[prop] === value){
                // ==================== add by hsm ================= //
                    record = r;
                    return false;
                }
            });
        }
        return record;
    }
});

Ext.override(Ext.data.Store, {
	load : function(options){
        // =================== add by hsm =================== //
	    delete this.loaded;
		// =================== add by hsm =================== //
	    
	    options = Ext.apply({}, options);
        this.storeOptions(options);
        if(this.sortInfo && this.remoteSort){
            var pn = this.paramNames;
            options.params = Ext.apply({}, options.params);
            options.params[pn.sort] = this.sortInfo.field;
            options.params[pn.dir] = this.sortInfo.direction;
        }
        try {
            return this.execute('read', null, options); // <-- null represents rs.  No rs for load actions.
        } catch(e) {
            this.handleException(e);
            return false;
        }
	},
	
	loadRecords : function(o, options, success){
		// =================== 标记数据加载已完成 =================== //
    	this.loaded = true;
    	// =================== 标记数据加载已完成 =================== //
    	
    	var i, len;
        
        if (this.isDestroyed === true) {
            return;
        }
        if(!o || success === false){
            if(success !== false){
                this.fireEvent('load', this, [], options);
            }
            if(options.callback){
                options.callback.call(options.scope || this, [], options, false, o);
            }
            return;
        }
        var r = o.records, t = o.totalRecords || r.length;
        if(!options || options.add !== true){
            if(this.pruneModifiedRecords){
                this.modified = [];
            }
            for(i = 0, len = r.length; i < len; i++){
                r[i].join(this);
            }
            if(this.snapshot){
                this.data = this.snapshot;
                delete this.snapshot;
            }
            this.clearData();
            this.data.addAll(r);
            this.totalLength = t;
            this.applySort();
            this.fireEvent('datachanged', this);
        }else{
            var toAdd = [],
                rec,
                cnt = 0;
            for(i = 0, len = r.length; i < len; ++i){
                rec = r[i];
                if(this.indexOfId(rec.id) > -1){
                    this.doUpdate(rec);
                }else{
                    toAdd.push(rec);
                    ++cnt;
                }
            }
            this.totalLength = Math.max(t, this.data.length + cnt);
            this.add(toAdd);
        }
        
        // =================== 增加对异常的捕获 =================== //
        
        try {
        	this.fireEvent('load', this, r, options);
        } catch (e) {
        	// 捕获'不能释放的脚本错误'
        	if (e.number != -2146823277) 
        		throw e;
        }
        
        // =================== 增加对异常的捕获 =================== //
        
        if(options.callback){
            options.callback.call(options.scope || this, r, options, true);
        }
    },
	
	/**
	 * 根据指定的属性/值获得记录
	 * @param property
	 * @param value
	 * @param anyhMatch true: 匹配任何位置 false: 开始位置匹配(默认)
	 * @param caseSensitive 区分大小写
	 * @param index 在出现多条数据时取指定位置的数据,默认取第一条,为负数时从后向前取,-1为最后一条,-2为最后第二条.依此类推
	 */
	queryUnique: function(property, value, anyMatch, caseSensitive, index) {
		if (value == null) return value;
		
		if (String(value).trim().length == 0) 
			return null;
	
		var data = this.queryBy(function(r) {
			return value === r.get(property); 
		});
		
		if (!data || data.getCount() == 0) 
			return null;
		
		index = index || 0;
		if (index < 0) 
			index = data.getCount() + index;
		
		if (index >= data.getCount())
			index = 0;

		return data.get(index);
	}
});

Ext.override(Ext.PagingToolbar, {
	/**
	 * @param {Number} start
	 * @param {Boolean} cache 是否优先从缓存中获得数据
	 */
	doLoad : function(start, cache){
        var o = {}, pn = this.getParams();
        o[pn.start] = start;
        o[pn.limit] = this.pageSize;
        
        if (cache === false)
        	o['refresh'] = true;
        
        if(this.fireEvent('beforechange', this, o) !== false){
            this.store.load({params:o});
        }
    },
    
    doRefresh : function(btn) {
    	var cache = !(btn instanceof Ext.Toolbar.Button);
        this.doLoad(this.cursor, cache);
    }
});

Ext.override(Ext.Editor, {
	completeEdit : function(remainVisible){
        if(!this.editing){
            return;
        }
        // Assert combo values first
        if (this.field.assertValue) {
            this.field.assertValue();
        }
        var v = this.getValue();
        if(!this.field.isValid()){
            if(this.revertInvalid !== false){
                this.cancelEdit(remainVisible);
            }
            return;
        }

        // ================== hsm add ================== //
        // if(String(v) === String(this.startValue) && this.ignoreNoChange){
        if (Ext.encode(v) === Ext.encode(this.startValue) && this.ignoreNoChange) {
        // ================== hsm add ================== //
            this.hideEdit(remainVisible);
            return;
        }
        if(this.fireEvent("beforecomplete", this, v, this.startValue) !== false){
            v = this.getValue();
            if(this.updateEl && this.boundEl){
                this.boundEl.update(v);
            }
            this.hideEdit(remainVisible);
            this.fireEvent("complete", this, v, this.startValue);
        }
    },
    
	startEdit : function(el, value){
		// =================== 增加属性 =================== //
		this.field.record = this.record;
		this.field.row = this.row;
		this.field.col = this.col;
		this.field.grid = this.grid;
		// =================== 增加属性 =================== //

		if(this.editing){
            this.completeEdit();
        }
        this.boundEl = Ext.get(el);
        var v = value !== undefined ? value : this.boundEl.dom.innerHTML;
        
        // =================== 开始编辑时初始值设为空串 =================== //
	    v = v === "&nbsp;" ? "" : v;
	    // =================== 开始编辑时初始值设为空串 =================== //
	    
        if(!this.rendered){
            this.render(this.parentEl || document.body);
        }
        if(this.fireEvent("beforestartedit", this, this.boundEl, v) !== false){
            this.startValue = v;
            this.field.reset();
            this.field.setValue(v);
            this.realign(true);
            this.editing = true;
            
            // =================== hsm update =================== //
            // this.show();
            // 如果没延时执行此方法在按Tab键切换字段时无法定位在新的位置上
            this.show.defer(1, this); 
            // =================== hsm update =================== //
            
            // =================== 开始编辑时默认全选 =================== //
		    this.field.el.dom.select();
		    // =================== 开始编辑时默认全选 =================== //
        }
	}

});

Ext.override(Ext.grid.GridPanel, {
	frame: false,
	border: true,
	stripeRows: true,
	columnLines: true,
	loadMask: {msg: '正在加载数据,请耐心等候...'},
	
	/**
	 * @cfg actions
	 * @type Array<Object> 
	 * @description 每行数据后添加的一些动作按钮
	 */
	
	initComponent: function() {
		
		Ext.grid.GridPanel.superclass.initComponent.call(this);

        if (this.columnLines) {
            this.cls = (this.cls || '') + ' x-grid-with-col-lines';
        }
        // override any provided value since it isn't valid
        // and is causing too many bug reports ;)
        this.autoScroll = false;
        this.autoWidth = false;

        if(Ext.isArray(this.columns)){
        	// =============== add by hsm ================ //
			this.initActions();
			
			this.colModel = new Ext.grid.ColumnModel({
				defaults: {
			        sortable: false,
			        menuDisabled: true
			    },
				columns: this.columns
			});
			// this.colModel = new Ext.grid.ColumnModel(this.columns);
			
			// =============== add by hsm ================ //
			
            
            delete this.columns;
        }

        // check and correct shorthanded configs
        if(this.ds){
            this.store = this.ds;
            delete this.ds;
        }
        if(this.cm){
            this.colModel = this.cm;
            delete this.cm;
        }
        if(this.sm){
            this.selModel = this.sm;
            delete this.sm;
        }
        this.store = Ext.StoreMgr.lookup(this.store);

        this.addEvents(
            // raw events
            /**
             * @event click
             * The raw click event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'click',
            /**
             * @event dblclick
             * The raw dblclick event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'dblclick',
            /**
             * @event contextmenu
             * The raw contextmenu event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'contextmenu',
            /**
             * @event mousedown
             * The raw mousedown event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'mousedown',
            /**
             * @event mouseup
             * The raw mouseup event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'mouseup',
            /**
             * @event mouseover
             * The raw mouseover event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'mouseover',
            /**
             * @event mouseout
             * The raw mouseout event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'mouseout',
            /**
             * @event keypress
             * The raw keypress event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'keypress',
            /**
             * @event keydown
             * The raw keydown event for the entire grid.
             * @param {Ext.EventObject} e
             */
            'keydown',

            // custom events
            /**
             * @event cellmousedown
             * Fires before a cell is clicked
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Number} columnIndex
             * @param {Ext.EventObject} e
             */
            'cellmousedown',
            /**
             * @event rowmousedown
             * Fires before a row is clicked
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowmousedown',
            /**
             * @event headermousedown
             * Fires before a header is clicked
             * @param {Grid} this
             * @param {Number} columnIndex
             * @param {Ext.EventObject} e
             */
            'headermousedown',

            /**
             * @event groupmousedown
             * Fires before a group header is clicked. <b>Only applies for grids with a {@link Ext.grid.GroupingView GroupingView}</b>.
             * @param {Grid} this
             * @param {String} groupField
             * @param {String} groupValue
             * @param {Ext.EventObject} e
             */
            'groupmousedown',

            /**
             * @event rowbodymousedown
             * Fires before the row body is clicked. <b>Only applies for grids with {@link Ext.grid.GridView#enableRowBody enableRowBody} configured.</b>
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowbodymousedown',

            /**
             * @event containermousedown
             * Fires before the container is clicked. The container consists of any part of the grid body that is not covered by a row.
             * @param {Grid} this
             * @param {Ext.EventObject} e
             */
            'containermousedown',

            /**
             * @event cellclick
             * Fires when a cell is clicked.
             * The data for the cell is drawn from the {@link Ext.data.Record Record}
             * for this row. To access the data in the listener function use the
             * following technique:
             * <pre><code>
function(grid, rowIndex, columnIndex, e) {
    var record = grid.getStore().getAt(rowIndex);  // Get the Record
    var fieldName = grid.getColumnModel().getDataIndex(columnIndex); // Get field name
    var data = record.get(fieldName);
}
</code></pre>
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Number} columnIndex
             * @param {Ext.EventObject} e
             */
            'cellclick',
            /**
             * @event celldblclick
             * Fires when a cell is double clicked
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Number} columnIndex
             * @param {Ext.EventObject} e
             */
            'celldblclick',
            /**
             * @event rowclick
             * Fires when a row is clicked
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowclick',
            /**
             * @event rowdblclick
             * Fires when a row is double clicked
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowdblclick',
            /**
             * @event headerclick
             * Fires when a header is clicked
             * @param {Grid} this
             * @param {Number} columnIndex
             * @param {Ext.EventObject} e
             */
            'headerclick',
            /**
             * @event headerdblclick
             * Fires when a header cell is double clicked
             * @param {Grid} this
             * @param {Number} columnIndex
             * @param {Ext.EventObject} e
             */
            'headerdblclick',
            /**
             * @event groupclick
             * Fires when group header is clicked. <b>Only applies for grids with a {@link Ext.grid.GroupingView GroupingView}</b>.
             * @param {Grid} this
             * @param {String} groupField
             * @param {String} groupValue
             * @param {Ext.EventObject} e
             */
            'groupclick',
            /**
             * @event groupdblclick
             * Fires when group header is double clicked. <b>Only applies for grids with a {@link Ext.grid.GroupingView GroupingView}</b>.
             * @param {Grid} this
             * @param {String} groupField
             * @param {String} groupValue
             * @param {Ext.EventObject} e
             */
            'groupdblclick',
            /**
             * @event containerclick
             * Fires when the container is clicked. The container consists of any part of the grid body that is not covered by a row.
             * @param {Grid} this
             * @param {Ext.EventObject} e
             */
            'containerclick',
            /**
             * @event containerdblclick
             * Fires when the container is double clicked. The container consists of any part of the grid body that is not covered by a row.
             * @param {Grid} this
             * @param {Ext.EventObject} e
             */
            'containerdblclick',

            /**
             * @event rowbodyclick
             * Fires when the row body is clicked. <b>Only applies for grids with {@link Ext.grid.GridView#enableRowBody enableRowBody} configured.</b>
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowbodyclick',
            /**
             * @event rowbodydblclick
             * Fires when the row body is double clicked. <b>Only applies for grids with {@link Ext.grid.GridView#enableRowBody enableRowBody} configured.</b>
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowbodydblclick',

            /**
             * @event rowcontextmenu
             * Fires when a row is right clicked
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowcontextmenu',
            /**
             * @event cellcontextmenu
             * Fires when a cell is right clicked
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Number} cellIndex
             * @param {Ext.EventObject} e
             */
            'cellcontextmenu',
            /**
             * @event headercontextmenu
             * Fires when a header is right clicked
             * @param {Grid} this
             * @param {Number} columnIndex
             * @param {Ext.EventObject} e
             */
            'headercontextmenu',
            /**
             * @event groupcontextmenu
             * Fires when group header is right clicked. <b>Only applies for grids with a {@link Ext.grid.GroupingView GroupingView}</b>.
             * @param {Grid} this
             * @param {String} groupField
             * @param {String} groupValue
             * @param {Ext.EventObject} e
             */
            'groupcontextmenu',
            /**
             * @event containercontextmenu
             * Fires when the container is right clicked. The container consists of any part of the grid body that is not covered by a row.
             * @param {Grid} this
             * @param {Ext.EventObject} e
             */
            'containercontextmenu',
            /**
             * @event rowbodycontextmenu
             * Fires when the row body is right clicked. <b>Only applies for grids with {@link Ext.grid.GridView#enableRowBody enableRowBody} configured.</b>
             * @param {Grid} this
             * @param {Number} rowIndex
             * @param {Ext.EventObject} e
             */
            'rowbodycontextmenu',
            /**
             * @event bodyscroll
             * Fires when the body element is scrolled
             * @param {Number} scrollLeft
             * @param {Number} scrollTop
             */
            'bodyscroll',
            /**
             * @event columnresize
             * Fires when the user resizes a column
             * @param {Number} columnIndex
             * @param {Number} newSize
             */
            'columnresize',
            /**
             * @event columnmove
             * Fires when the user moves a column
             * @param {Number} oldIndex
             * @param {Number} newIndex
             */
            'columnmove',
            /**
             * @event sortchange
             * Fires when the grid's store sort changes
             * @param {Grid} this
             * @param {Object} sortInfo An object with the keys field and direction
             */
            'sortchange',
            /**
             * @event groupchange
             * Fires when the grid's grouping changes (only applies for grids with a {@link Ext.grid.GroupingView GroupingView})
             * @param {Grid} this
             * @param {String} groupField A string with the grouping field, null if the store is not grouped.
             */
            'groupchange',
            /**
             * @event reconfigure
             * Fires when the grid is reconfigured with a new store and/or column model.
             * @param {Grid} this
             * @param {Ext.data.Store} store The new store
             * @param {Ext.grid.ColumnModel} colModel The new column model
             */
            'reconfigure',
            /**
             * @event viewready
             * Fires when the grid view is available (use this for selecting a default row).
             * @param {Grid} this
             */
            'viewready'
        );
	},
	
	initActions: function() {
		var grid = this,
			actions = grid.actions,
			columns = grid.columns;
		
		if (Ext.isEmpty(actions) || actions.length == 0)
			return;

		Ext.iterate(actions, function(actionCfg) {
			columns.push(Ext.apply({
				xtype: 'customcolumn',
				dataIndex: 'action-' + Ext.id()
			}, actionCfg));
		});
	}
});

Ext.override(Ext.grid.EditorGridPanel, {
	onEditComplete : function(ed, value, startValue){
		this.editing = false;
        this.lastActiveEditor = this.activeEditor;
        this.activeEditor = null;

        var r = ed.record,
            field = this.colModel.getDataIndex(ed.col);
        value = this.postEditValue(value, startValue, r, field);
        // ===================== 原先方法无法比较对象 ===================== //
        var encodeValue = Ext.encode(value);
        var encodeStartValue = Ext.encode(startValue);

        // if(this.forceValidation === true || String(value) !== String(startValue)){
        if (this.forceValidation === true || encodeValue !== encodeStartValue) {
		// ===================== 原先方法无法比较对象 ===================== //
            var e = {
                grid: this,
                record: r,
                field: field,
                originalValue: startValue,
                value: value,
                row: ed.row,
                column: ed.col,
                cancel:false
            };
            // ===================== 原先方法无法比较对象 ===================== //
            // if(this.fireEvent("validateedit", e) !== false && !e.cancel && String(value) !== String(startValue)){
            if(this.fireEvent("validateedit", e) !== false && !e.cancel && encodeValue !== encodeStartValue){
            // ===================== 原先方法无法比较对象 ===================== //
                r.set(field, e.value);
                delete e.cancel;
                this.fireEvent("afteredit", e);
            }
        }
        this.view.focusCell(ed.row, ed.col);
    },
    
	startEditing : function(row, col){
		this.stopEditing();
        if(this.colModel.isCellEditable(col, row)){
            this.view.ensureVisible(row, col, true);
            var r = this.store.getAt(row),
                field = this.colModel.getDataIndex(col),
                e = {
                    grid: this,
                    record: r,
                    field: field,
                    value: r.data[field],
                    row: row,
                    column: col,
                    cancel:false
                };
            if(this.fireEvent("beforeedit", e) !== false && !e.cancel){
                this.editing = true;
                var ed = this.colModel.getCellEditor(col, row);
                if(!ed){
                    return;
                }
                if(!ed.rendered){
                    ed.parentEl = this.view.getEditorParent(ed);
                    ed.on({
                        scope: this,
                        render: {
                            fn: function(c){
                                c.field.focus(false, true);
                            },
                            single: true,
                            scope: this
                        },
                        specialkey: function(field, e){
                            this.getSelectionModel().onEditorKey(field, e);
                        },
                        complete: this.onEditComplete,
                        canceledit: this.stopEditing.createDelegate(this, [true])
                    });
                }
                Ext.apply(ed, {
                	// ===== hsm add ===== //
                	grid	: this, 
                	// ===== hsm add ===== //
                    row     : row,
                    col     : col,
                    record  : r
                });
                this.lastEdit = {
                    row: row,
                    col: col
                };
                this.activeEditor = ed;
                // Set the selectSameEditor flag if we are reusing the same editor again and
                // need to prevent the editor from firing onBlur on itself.
                ed.selectSameEditor = (this.activeEditor == this.lastActiveEditor);
                var v = this.preEditValue(r, field);
                ed.startEdit(this.view.getCell(row, col).firstChild, Ext.isDefined(v) ? v : '');

                // Clear the selectSameEditor flag
                (function(){
                    delete ed.selectSameEditor;
                }).defer(50);
            }
        }
	}
});

Ext.override(Ext.grid.GridView, {	
	cellTpl: new Ext.Template(
        '<td class="x-grid3-col x-grid3-cell x-grid3-td-{id} x-selectable {css}" style="{style}" tabIndex="0" {cellAttr}>',
            '<div class="x-grid3-cell-inner x-grid3-col-{id}" {attr}>{value}</div>',
        '</td>'
    ),
    
    onHeaderClick : function(g, index) {
        if (this.headersDisabled || !this.cm.isSortable(index)) {
            return;
        }
        g.stopEditing(true);
        
        // =============== add by hsm ================ //
        // 如果表格中没有数据则字段排序功能会失效
        if (g.store.getCount() > 0) {
        	g.store.sort(this.cm.getDataIndex(index));
        }
        // =============== add by hsm ================ //
    },
    
	renderHeaders : function(){
		var colModel   = this.cm,
            templates  = this.templates,
            headerTpl  = templates.hcell,
            properties = {},
            colCount   = colModel.getColumnCount(),
            last       = colCount - 1,
            cells      = [],
            i, cssCls;
        
        for (i = 0; i < colCount; i++) {
            if (i == 0) {
                cssCls = 'x-grid3-cell-first ';
            } else {
                cssCls = i == last ? 'x-grid3-cell-last ' : '';
            }
            
            properties = {
                id     : colModel.getColumnId(i),
                value  : colModel.getColumnHeader(i) || '',
                style  : this.getColumnStyle(i, true),
                // =============== remove by hsm ================ //
                // tooltip: this.getColumnTooltip(i),
                // =============== remove by hsm ================ //
                css    : cssCls
            };
            
            if (colModel.config[i].align == 'right') {
                properties.istyle = 'padding-right: 16px;';
            } else {
                delete properties.istyle;
            }
            
            cells[i] = headerTpl.apply(properties);
        }
        
        return templates.header.apply({
            cells : cells.join(""),
            tstyle: String.format("width: {0};", this.getTotalWidth())
        });
    },
	
	// private
    doRender : function(columns, records, store, startRow, colCount, stripe) {
    	var templates = this.templates,
            cellTemplate = templates.cell,
            rowTemplate = templates.row,
            last = colCount - 1,
            tstyle = 'width:' + this.getTotalWidth() + ';',
            // buffers
            rowBuffer = [],
            colBuffer = [],
            rowParams = {tstyle: tstyle},
            meta = {},
            len  = records.length,
            alt,
            column,
            record, i, j, rowIndex;
            
        //build up each row's HTML
        for (j = 0; j < len; j++) {
            record    = records[j];
            colBuffer = [];

            rowIndex = j + startRow;

            //build up each column's HTML
            for (i = 0; i < colCount; i++) {
                column = columns[i];
                
                meta.id    = column.id;
                meta.css   = i === 0 ? 'x-grid3-cell-first ' : (i == last ? 'x-grid3-cell-last ' : '');
                meta.attr  = meta.cellAttr = '';
                meta.style = column.style;
                
                // ========================= hsm add ========================= //
                meta.value = column.renderer.call(column.scope, record.data[column.name], meta, record, rowIndex, i, store, this.grid);
                // ========================= hsm add ========================= //
                
                // ========================= 替换回车为空格 ========================= //
                if (Ext.isString(meta.value)) {
                	meta.value = meta.value.replace(/(\r\n|\\r\\n)/g, ' ');
                }
				// ========================= 替换回车为空格 ========================= //
                
                // ========================= 创建Tooltip ========================= //
                var cm = this.grid.getColumnModel(),
                	tooltip = meta.tooltip || cm.getColumnTooltip(i),
                	qt = {qtitle: null, qtip: null},
                	
                	stripTags = Ext.util.Format.stripTags;
                	
                if (!Ext.isEmpty(tooltip, false)) {
                	if (Ext.isObject(tooltip)) {
                		qt.qtitle = tooltip.qtitle;
                		qt.qtip = tooltip = tooltip.qtip;
                	}
                	
                	if (tooltip === true) {
		                qt.qtip = stripTags(meta.value);
		                
                	} else if (Ext.isString(tooltip) || tooltip instanceof Ext.XTemplate) {
		            	var tpl = Ext.isString(tooltip) ? new Ext.XTemplate(tooltip) : tooltip,
		            		data = {};
		            	data[cm.getDataIndex(i)] = stripTags(meta.value);
		            	data = Ext.applyIf(data, record.data);
		            	qt.qtip = tpl.apply(data);
		            }
                	
                	if (!cm.isHidden(i) && !Ext.isEmpty(cm.getDataIndex(i), false)) {
                		if (!Ext.isEmpty(qt.qtitle, false)) {
                			var tpl = Ext.isString(qt.qtitle) ? new Ext.XTemplate(qt.qtitle) : qt.qtitle,
                				data = {};
                				
                			data[cm.getDataIndex(i)] = stripTags(meta.value);
			            	data = Ext.applyIf(data, record.data);
			            	qt.qtitle = tpl.apply(data);
                			
                		} else {
		                	qt.qtitle = cm.getColumnHeader(i);
                		}
                		
		                if (!Ext.isEmpty(qt.qtitle, false))
		                	meta.attr += ' ext:qtitle="' + qt.qtitle + '"';
		                
		               	meta.attr += ' ext:qtip="' + qt.qtip + '"';
	                }
                	
                }
                // ========================= 创建Tooltip ========================= //

                if (this.markDirty && record.dirty && typeof record.modified[column.name] != 'undefined') {
                    meta.css += ' x-grid3-dirty-cell';
                }

                colBuffer[colBuffer.length] = cellTemplate.apply(meta);
            }

            alt = [];
            //set up row striping and row dirtiness CSS classes
            if (stripe && ((rowIndex + 1) % 2 === 0)) {
                alt[0] = 'x-grid3-row-alt';
            }

            if (record.dirty) {
                alt[1] = ' x-grid3-dirty-row';
            }

            rowParams.cols = colCount;

            if (this.getRowClass) {
                alt[2] = this.getRowClass(record, rowIndex, rowParams, store);
            }

            rowParams.alt   = alt.join(' ');
            rowParams.cells = colBuffer.join('');

            rowBuffer[rowBuffer.length] = rowTemplate.apply(rowParams);
        }

        return rowBuffer.join('');
    },
    
    refreshRow: function(record) {
        var store     = this.ds,
            colCount  = this.cm.getColumnCount(),
            columns   = this.getColumnData(),
            last      = colCount - 1,
            cls       = ['x-grid3-row'],
            rowParams = {
                tstyle: String.format("width: {0};", this.getTotalWidth())
            },
            colBuffer = [],
            cellTpl   = this.templates.cell,
            rowIndex, row, column, meta, css, i;
        
        if (Ext.isNumber(record)) {
            rowIndex = record;
            record   = store.getAt(rowIndex);
        } else {
            rowIndex = store.indexOf(record);
        }
        
        //the record could not be found
        if (!record || rowIndex < 0) {
            return;
        }
        
        //builds each column in this row
        for (i = 0; i < colCount; i++) {
            column = columns[i];
            
            if (i == 0) {
                css = 'x-grid3-cell-first';
            } else {
                css = (i == last) ? 'x-grid3-cell-last ' : '';
            }
            
            meta = {
                id      : column.id,
                style   : column.style,
                css     : css,
                attr    : "",
                cellAttr: "",
                tooltip : false
            };
            // Need to set this after, because we pass meta to the renderer
            // ========================= hsm add ========================= //
            meta.value = column.renderer.call(column.scope, record.data[column.name], meta, record, rowIndex, i, store, this.grid);
            // ========================= hsm add ========================= //
            
            // ========================= 替换回车为空格 ========================= //
            if (Ext.isString(meta.value)) {
            	meta.value = meta.value.replace(/(\r\n|\\r\\n)/g, ' ');
            }
			// ========================= 替换回车为空格 ========================= //
            
            // ========================= 创建Tooltip ========================= //
            var cm = this.grid.getColumnModel(),
            	tooltip = meta.tooltip || cm.getColumnTooltip(i),
            	qt = {qtitle: null, qtip: null},
            	
            	stripTags = Ext.util.Format.stripTags;
            	
            if (!Ext.isEmpty(tooltip, false)) {
            	if (Ext.isObject(tooltip)) {
            		qt.qtitle = tooltip.qtitle;
            		qt.qtip = tooltip = tooltip.qtip;
            	}
            	
            	if (tooltip === true) {
	                qt.qtip = stripTags(meta.value);
	                
            	} else if (Ext.isString(tooltip) || tooltip instanceof Ext.XTemplate) {
	            	var tpl = Ext.isString(tooltip) ? new Ext.XTemplate(tooltip) : tooltip,
	            		data = {};
	            	data[cm.getDataIndex(i)] = stripTags(meta.value);
	            	data = Ext.applyIf(data, record.data);
	            	qt.qtip = tpl.apply(data);
	            }
            	
            	if (!cm.isHidden(i) && !Ext.isEmpty(cm.getDataIndex(i), false)) {
            		if (!Ext.isEmpty(qt.qtitle, false)) {
            			var tpl = Ext.isString(qt.qtitle) ? new Ext.XTemplate(qt.qtitle) : qt.qtitle,
            				data = {};
            				
            			data[cm.getDataIndex(i)] = stripTags(meta.value);
		            	data = Ext.applyIf(data, record.data);
		            	qt.qtitle = tpl.apply(data);
            			
            		} else {
	                	qt.qtitle = cm.getColumnHeader(i);
            		}
            		
	                if (!Ext.isEmpty(qt.qtitle, false))
	                	meta.attr += ' ext:qtitle="' + qt.qtitle + '"';
	                
	               	meta.attr += ' ext:qtip="' + qt.qtip + '"';
                }
            	
            }
            // ========================= 创建Tooltip ========================= //
            
            if (this.markDirty && record.dirty && typeof record.modified[column.name] != 'undefined') {
                meta.css += ' x-grid3-dirty-cell';
            }
            
            colBuffer[i] = cellTpl.apply(meta);
        }
        
        row = this.getRow(rowIndex);
        row.className = '';
        
        if (this.grid.stripeRows && ((rowIndex + 1) % 2 === 0)) {
            cls.push('x-grid3-row-alt');
        }
        
        if (this.getRowClass) {
            rowParams.cols = colCount;
            cls.push(this.getRowClass(record, rowIndex, rowParams, store));
        }
        
        this.fly(row).addClass(cls).setStyle(rowParams.tstyle);
        rowParams.cells = colBuffer.join("");
        row.innerHTML = this.templates.rowInner.apply(rowParams);
        
        this.fireEvent('rowupdated', this, rowIndex, record);
    }
});

Ext.override(Ext.grid.Column, {
	align: 'center'
});

Ext.override(Ext.form.BasicForm, {
	setValues : function(values){
        if(Ext.isArray(values)){ // array of objects
            for(var i = 0, len = values.length; i < len; i++){
                var v = values[i];
                var f = this.findField(v.id);
                if(f){
                    f.setValue(v.value);
                    if(this.trackResetOnLoad){
                        f.originalValue = f.getValue();
                    }
                }
            }
        }else{ // object hash
            // ===================== hsm add ===================== //
            var form = this;
	    	form.items.each(function(field) {
	    		var value = null;
	    		try {
	    			value = values[field.getName(true)];
	    			if (value == null) {
	    				value = eval("values." + field.getName(true));
	    			}
	    		} catch(e) {
	    			// value = null;
	    			value = undefined;
	    		}

	    		if (typeof value != 'undefined' && typeof value != "function") {
	    			if (field instanceof Ext.form.TextArea && !Ext.isEmpty(value, false))
	    				value = value.replace(/\\r\\n/g, '\r\n');
	    			else if (typeof value == 'string')
	    				value = value.replace(/(\\r\\n|\r\n)/g, ' ');

		    		field.setValue(value);
	                if(this.trackResetOnLoad){
	                    field.originalValue = field.getValue();
	                }
	    		}
	    	});
	    	// ===================== hsm add ===================== //
        }
        return this;
    },
    
    getValues: function(asString) {
    	// ===================== hsm add ===================== //
    	this.items.each(function(field) {
			if (field.getXType() == 'htmleditor') {
				field.sync();
			}
		});
    	// ===================== hsm add ===================== //
    
    	var fs = Ext.lib.Ajax.serializeForm(this.el.dom);
        if(asString === true){
            return fs;
        }
        return Ext.urlDecode(fs);
    }
});

Ext.override(Ext.XTemplate, {
	// private
    compileTpl : function(tpl) {
        var fm = Ext.util.Format,
            useF = this.disableFormats !== true,
            sep = Ext.isGecko ? "+" : ",",
            body;

        function fn(m, name, format, args, math){
            if(name.substr(0, 4) == 'xtpl'){
                return "'"+ sep +'this.applySubTemplate('+name.substr(4)+', values, parent, xindex, xcount)'+sep+"'";
            }
            var v;
            if(name === '.'){
                v = 'values';
            }else if(name === '#'){
                v = 'xindex';
            }else if(name.indexOf('.') != -1){
                // v = name;
            	v = 'values[\'' + name + '\']';
            }else{
                v = "values['" + name + "']";
            }
            if(math){
                v = '(' + v + math + ')';
            }
            if (format && useF) {
                args = args ? ',' + args : "";
                if(format.substr(0, 5) != "this."){
                    format = "fm." + format + '(';
                }else{
                    format = 'this.call("'+ format.substr(5) + '", ';
                    args = ", values";
                }
            } else {
                args= ''; format = "("+v+" === undefined ? '' : ";
            }
            return "'"+ sep + format + v + args + ")"+sep+"'";
        }

        function codeFn(m, code){
            // Single quotes get escaped when the template is compiled, however we want to undo this when running code.
            return "'" + sep + '(' + code.replace(/\\'/g, "'") + ')' + sep + "'";
        }

        // branched to use + in gecko and [].join() in others
        if(Ext.isGecko){
            body = "tpl.compiled = function(values, parent, xindex, xcount){ return '" +
                   tpl.body.replace(/(\r\n|\n)/g, '\\n').replace(/'/g, "\\'").replace(this.re, fn).replace(this.codeRe, codeFn) +
                    "';};";
        }else{
            body = ["tpl.compiled = function(values, parent, xindex, xcount){ return ['"];
            body.push(tpl.body.replace(/(\r\n|\n)/g, '\\n').replace(/'/g, "\\'").replace(this.re, fn).replace(this.codeRe, codeFn));
            body.push("'].join('');};");
            body = body.join('');
        }
        eval(body);
        return this;
    }
});

Ext.util.Format.fileSize = function(size) {
	if (size < 1024) {
        return size + " B";
    } else if (size < 1048576) {
        return (Math.round(((size*10) / 1024))/10) + " KB";
    } else if (size < 1073741824) {
        return (Math.round(((size*10) / 1048576))/10) + " MB";
    } else if (size < 1099511627776) {
    	return (Math.round(((size*10) / 1073741824))/10) + " GB";
    } else {
    	return (Math.round(((size*10) / 1099511627776))/10) + " TB";
    }
};

Ext.apply(Ext.data.Connection, {
	timeout: 600000 // 10分钟
});

Ext.apply(Ext.lib.Ajax, {
	timeout: 1200000, // 20分钟
	
	serializeForm : function(form) {
		var fElements = form.elements || (document.forms[form] || Ext.getDom(form)).elements, 
            hasSubmit = false, 
            encoder = encodeURIComponent, 
            name, 
            data = '', 
            type, 
            hasValue;
    
        Ext.each(fElements, function(element){
            name = element.name;
            type = element.type;
    
            // ================ 禁用字段的值被获得 ================ //
            // if (!element.disabled && name) {
            if (name) {
            // ================ 禁用字段的值被获得 ================ //
                if (/select-(one|multiple)/i.test(type)) {
                    Ext.each(element.options, function(opt){
                        if (opt.selected) {
                            hasValue = opt.hasAttribute ? opt.hasAttribute('value') : opt.getAttributeNode('value').specified;
                            data += String.format("{0}={1}&", encoder(name), encoder(hasValue ? opt.value : opt.text));
                        }
                    });
                } else if (!(/file|undefined|reset|button/i.test(type))) {
                    if (!(/radio|checkbox/i.test(type) && !element.checked) && !(type == 'submit' && hasSubmit)) {
                    	// ========================= 带id的字段如果值为空则该字段不传至服务器 ========================= //
                    	if (!(name.indexOf('.id') > -1 && Ext.isEmpty(element.value, false)))
                    	// ========================= 带id的字段如果值为空则该字段不传至服务器 ========================= //
                        data += encoder(name) + '=' + encoder(element.value) + '&'; 
                    	// data += encoder(name) + '=' + encoder(Ext.isEmpty(element.value, false) ? null : element.value) + '&';
                        hasSubmit = /submit/i.test(type);
                        
                    // ========================= 增加对未选中的支持 ========================= //
                    } else if (/radio|checkbox/i.test(type) && !element.checked) {
                    	data += encoder(name) + '=&';
                    }
                    // ========================= 增加对未选中的支持 ========================= //
                }
            }
        });
        return data.substr(0, data.length - 1);
    }
});

Ext.override(Ext.data.JsonReader, {
	createAccessor : function(){
		var re = /[\[\.]/;
        return function(expr) {
            if(Ext.isEmpty(expr)){
                return Ext.emptyFn;
            }
            if(Ext.isFunction(expr)){
                return expr;
            }
            var i = String(expr).search(re);
            if(i >= 0){
                return new Function("obj", "var v; try {v = obj." + expr + ";} catch (e) {try {v = obj['" + expr + "'];} catch (e) {v = null}} return v;");
            }
            return function(obj){
                return obj[expr];
            };

        };
    }()
});

(function() {
	var ajax = Ext.Ajax;
	
	var onAjaxBeforeRequest = function(conn, options) {
		var params = options.params;
		if (Ext.isObject(params) && !Ext.isString(params['condition'])) {
			options.params.condition = Ext.encode(params['condition']);
		}
		
		var waitMsg = options.waitMsg;
		if (waitMsg) {
	        Ext.Msg.wait(waitMsg, '等候');
	        Ext.Msg.updateProgress(0.1);
	    }
	};
	
	var onAjaxRequestComplete = function(conn, response, options) {
		// 返回值中有Exception则认为是抛出异常
		if (response.responseText.indexOf('errorType') > -1) {
			options.success = false;
			Ext.Ajax.fireEvent('requestexception', conn, response, options);
			return;
		}
		
		if (options.waitMsg) {
			Ext.Msg.updateProgress(1);
	        Ext.Msg.hide();
	    }
	};
	
	var onAjaxRequestException = function(conn, response, options) {
		if (response.status == -1) {
			Ext.Msg.alert('提示', '系统繁忙.');
			return;
		}
		
		if (response.responseText) {
			// 取消了Ajax的failure回调,只能在点了错误对话框中点击关闭才能触发failure回调
			var callback = options.failure;
			delete options.failure;
			
			var dlg = Ext.Msg.getDialog();
			var error = Ext.decode(response.responseText);
			var isWarning = error.errorType == 'warning';
			
			var msg = options.errorMsg;
			if (typeof msg == "function")
				msg = msg(error, options);
			
			if (!msg)
				msg = error.msg.replace(/\\r\\n/g, '\r\n') || '操作失败!';
			
			Ext.Msg.show({
				title: isWarning ? '警告' : '<span style="color:red;">错误</span>',
				icon: isWarning ? Ext.Msg.WARNING : Ext.Msg.ERROR,
				msg: '<b>' + msg + '<br/></b>',
				width: 700,
				
				buttons: isWarning ? {ok: '关闭'} : {ok: '关闭', yes: '查看错误信息'},
				fn: function(btn) {
					if (btn == 'yes') {
						Ext.Msg.show({
							title: '<span style="color:red;">错误</span>',
							icon: Ext.Msg.ERROR,
							msg: '<span style="color:blue;font-weight:bold;">' + msg + '<br/></span>',
							width: 700,
							prompt: true,
							multiline: 400,
							value: error.stackTrace.replace(/\\r\\n/g, '\r\n'),
							buttons: {cancel: '关闭'},
							fn: function() {
								if (Ext.isFunction(callback))
									callback();
							}
						});
						
					} else {
						if (Ext.isFunction(callback)) {
							callback();
						}
					}
				}
			});
		}
	};
	
	ajax.defaultHeaders = {Accept: 'text/json,application/json'};
	ajax.on({
		beforerequest: onAjaxBeforeRequest,
		requestcomplete: onAjaxRequestComplete,
		requestexception: onAjaxRequestException
	});
})();

Ext.apply(Ext, {
	isEmpty : function(v, allowBlank){
		if (v == 'null' || v == 'undefined')
			return true;
			
        return v === null || v === undefined || ((Ext.isArray(v) && !v.length)) || (!allowBlank ? v === '' : false);
    }
});

Ext.apply(Ext.form.VTypes, {
	'mutilnum': function(v) {
		var reg = /^\d+(\,\d+)*$/g;
		return reg.test(v);
	},	
	'mutilnumText': '该输入项只允许数字和逗号,且逗号不允许出现在开始和结束的位置',
	
	'mutilemail': function(v) {
		var reg = /^(\w+)([\-+.][\w]+)*@(\w[\-\w]*\.){1,5}([A-Za-z]){2,6}(\,(\w+)([\-+.][\w]+)*@(\w[\-\w]*\.){1,5}([A-Za-z]){2,6})*$/;
		return reg.test(v);
	},	
	'mutilemailText': '该输入项必须是电子邮件地址，格式如： "user@example.com"，当有多个邮箱时请用逗号分隔',
	
	'mutilmobile': function(v) {
		var reg = /^(0?)1[3|4|5|8][0-9]\d{8}(\,(0?)1[3|4|5|8][0-9]\d{8})*$/g;
		return reg.test(v);
	},
	'mutilmobileText': '该输入项必须是手机号码，多个手机时逗号分隔'
});

RegExp.escape = function(s) {
	if ('string' !== typeof s) {
		return s;
	}
	
	// Note: if pasting from forum, precede ]/\ with backslash manually
	return s.replace(/([.*+?^=!:${}()|[\]\/\\])/g, '\\$1');
};

Ext.override(String, {
	/**
	 * 获得汉字长度
	 * @return {Number}
	 */
	len: function() {
		return this.replace(/[\u4e00-\u9fa5]/g, 'aa').length;
	},
	
	/**
	 * 获得url中的参数
	 */
	urlParams: function() {
		var s = this, search,
			pos = s.indexOf('?');
			
		if (pos == -1) return null;
		search = s.substring(pos + 1);
		
		return Ext.isEmpty(search) ? null : Ext.urlDecode(search);
	}
});

Ext.override(Function, {
	/**
	 * @param {Function} fnc
	 * @param {Object} obj
	 * @param {Array} args
	 */
	timer: function(fnc, obj, args) {
		var me = this,
			timer = null,
			fn = me.createDelegate(obj, args);
		
		var i = 0;
		(function() {
			if (i < 100 && !fnc || fnc.apply(obj || me)) {
				clearTimeout(timer);
				timer = null;
				
				fn();
			} else {
				timer = setTimeout(arguments.callee, 0);
			}
		})();
	}
});

Ext.Ajax.timeout = 24 * 60 * 60 * 1000;