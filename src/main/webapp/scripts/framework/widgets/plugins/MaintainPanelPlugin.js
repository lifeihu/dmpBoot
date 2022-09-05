_package('framework.widgets.plugins');

framework.widgets.plugins.MaintainPanelPlugin = Ext.extend(Ext.util.Observable, {
	
	/**
	 * @private
	 * @property data
	 */
	
	// 需要在Panel中定义的属性
	
	/**
	 * @requires
	 * @cfg model
	 * @type {String}
	 * @description 当前数据来源的Model类,主键的取值与该参数有关,不允许为空
	 */
	
	/**
	 * @requires
	 * @cfg dataRoot
	 * @type {String}
	 * @description JSON数据的开始元素
	 */
	
	init: function(pnl) {
		var plg = this;
		
		if (Ext.isEmpty(pnl.dataRoot, false))
			pnl.dataRoot = pnl.model;
		
		if (!pnl.isMaster && Ext.isEmpty(pnl.childrenDataRoot, false))
			pnl.childrenDataRoot = pnl.model + 's';
			
		pnl.getMaintainPanelPlugin = plg.getMaintainPanelPlugin.createDelegate(plg);	
		
		Ext.apply(plg, {
			pnl: pnl,
			
			model: pnl.model,
			dataRoot: pnl.dataRoot,
			childrenDataRoot: pnl.childrenDataRoot
		});
		
		pnl.addEvents(
			/**
			 * @event loaddatacomplete
			 * @param {Ext.Panel} mp
			 * @param {Object} data
			 */
			'loaddatacomplete',
			
			/**
			 * @event validate
			 * @param {Ext.Panel} mp
			 */
			'validate'
		);
		
		plg.initPanelMethods();
	},
	
	initPanelMethods: function() {
		var plg = this,
			pnl = plg.pnl;

		// pnl.getKeyColumn = plg.getKeyColumn.createDelegate(plg);
		// pnl.getKeyValue = plg.getKeyValue.createDelegate(plg);
		pnl.setData = plg.setData.createDelegate(plg);
		pnl.loadData = plg.loadData.createDelegate(plg);
		pnl.getValue = plg.getValue.createDelegate(plg);
		pnl.valid = plg.valid.createDelegate(plg);

		// 需要在Panel中实现
		
		if (!Ext.isEmpty(pnl.getData))
			plg.getData = pnl.getData.createDelegate(pnl);
		
		/**
		 * 开始加载数据
		 * @param {Object} data
		 * @return {Boolean}
		 */
		plg.startLoad = pnl.startLoad.createDelegate(pnl);
		
		/**
		 * 开始检验Panel数据
		 * @param framework.widgets.plugins.ModuleValidatePlugin
		 * @return {Boolean} 
		 */
		plg.validate = pnl.validate.createDelegate(pnl);
		
		/**
		 * @method setReadOnly
		 * @param {Boolean} readonly
		 * @description 设置Panel是否只读
		 * @return Boolean 
		 */
		
		/**
		 * @method getMonitoringValidateField
		 * @description 获得需要被校验的字段
		 * @return {Ext.form.Field/Ext.util.MixedCollection<Ext.form.Field>/Array<Ext.form.Field>} 
		 */
	},
	
	/**
	 * 将由MaintainModule中加载的数据设置到当前Panel
	 * @param {Object} data
	 * @param {Boolean} byDataRoot 默认true
	 */
	setData: function(data, byDataRoot) {
		if (data == null) return;
		
		this.data = data;
		if (byDataRoot !== false) {
			try {
				this.data = eval("data." + this.dataRoot);
			} catch (e) {
				this.data = null;
			}
		}
	},
	
	/**
	 * 加载Panel数据
	 * @param {Object} data
	 */
	loadData: function(data) {
		var plg = this,
			pnl = this.pnl;
			
		if (Ext.isObject(data))
			pnl.loaded = false;
		
		data = data || plg.data;
		if (pnl.loaded === true || data == null) return;

		plg.startLoad(data);
		
		pnl.loaded = true;
		pnl.fireEvent('loaddatacomplete', pnl, data);
	},
	
	/**
	 * 获得Panel数据(对外接口)
	 * @param {Boolean} asString
	 * @param {Boolean} byModel
	 * @return {Object}
	 */
	getValue: function(asString, byModel) {
		var data = this.getData();
		if (data == null) return null;

		if (byModel === true) {
			var value = {};
			value[this.model] = asString === true ? Ext.encode(data) : data;
			return value;
			
		} else
			return asString === true ? Ext.encode(data) : data;
	},
	
	/**
	 * 校验Panel数据
	 * @param framework.widgets.plugins.ModuleValidatePlugin
	 * @return {Boolean}
	 */
	valid: function(plgValidate) {
		var v = this.validate(plgValidate);
		this.pnl.fireEvent('validate', this.pnl);
		
		return v;
	},
	
	/**
	 * 获得Panel数据(内部接口)
	 * 一般情况下该方法需要被重载
	 * @private
	 * @return {Object}
	 */
	getData: function() {
		return this.data;
	},
	
	getMaintainPanelPlugin: function() {
		return this;
	}
});

Ext.preg('maintainpanelplugin', framework.widgets.plugins.MaintainPanelPlugin);