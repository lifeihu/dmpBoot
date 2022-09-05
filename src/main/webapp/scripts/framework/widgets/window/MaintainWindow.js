_package('framework.widgets.window');

_import('framework.widgets.window.ModuleWindow');

framework.widgets.window.MaintainWindow = Ext.extend(framework.widgets.window.ModuleWindow, {
	
	relayEvent: [
		'beforeloaddata', 'loaddatacomplete', 
		'beforesave', 'savecomplete', 'savefailure',
		'beforeremove', 'removecomplete', 'removefailure'
	],
	
	initComponent: function() {
		var me = this;
		
		me.tbar = [
			me.BUTTON_SAVE = new Ext.Button({
				text: '保存',
				iconCls: "save",
				disabled: true,
				tooltip: '快捷键: Shift + Enter',
				handler: me.onSave,
				scope: me
			}),
			
			me.BUTTON_REMOVE = new Ext.Button({
				text: '删除',
				iconCls: "remove",
				disabled: true,
				handler: me.onRemove,
				scope: me
			}),
			
			me.BUTTON_SWITCH = new Ext.Button({
				text: '编辑',
				iconCls: "switch",
				disabled: true,
				handler: me.onSwitch,
				scope: me 
			})
		];
		
		me.addEvents(
			/**
			 * @event beforeswitch
			 * 切换编辑/浏览模式前触发
			 * @param {framework.widgets.window.MaintainWindow}
			 */
			"beforeswitch",
			
			/**
			 * @event switchcomplete
			 * 切换编辑/浏览模式成功后触发
			 * @param {framework.widgets.window.MaintainWindow}
			 * @param action 当前操作模式 create/view/update
			 */
			"switchcomplete",
			
			/* ------------------ relayEvents ---------------- */
			
			/**
			 * @event beforeloaddata
			 * 在加载数据前触发
			 * @param {framework.modules.MaintainModule}
			 * @param {Object} data
			 */
			"beforeloaddata",
				
			/**
			 * @event loaddatacomplete
			 * 所有维护面板的数据加载完成后触发
			 * @param {framework.modules.MaintainModule}
			 * @param {Object} data
			 */
			"loaddatacomplete", 
			
			/**
			 * @event beforesave
			 * 数据在提交后台前触发
			 * @param {framework.modules.MaintainModule}
			 * @param {Object} param 提交时的参数
			 * @return {Boolean} false: 提交操作终止
			 */
			"beforesave",
			
			/**
			 * @event savecomplete
			 * 数据提交成功后触发
			 * @param {framework.modules.MaintainModule}
			 * @param {Object} data
			 */
			"savecomplete",
			
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
			"removecomplete"
		);
		
		framework.widgets.window.MaintainWindow.superclass.initComponent.call(this);
		
		me.on({
			loaddatacomplete: me.onModuleLoadDataComleted,
			scope: me
		});
	},
	
	//////////////////////////////////////////////////
	
	onSave: function() {
		var me = this,
			mdl = me.getModule();
		
		if (mdl) {
			mdl.on('beforesave', me.onBeforeSave, me, {single: true});
			mdl.on('savecomplete', me.onAfterSave, me, {single: true});
		
			mdl.save();
		}
	},
	
	onBeforeSave: function() {
		this.BUTTON_SAVE.disable();
	},
	
	onAfterSave: function() {
		var me = this,
			mdl = me.getModule(),
			
			autoHideWindow = true;
			
		if (mdl) {
			autoHideWindow = mdl.autoHideWindow;
		}
		
		if (autoHideWindow === true) {
			me.close();
		} else {
			me.BUTTON_SAVE.enable();
		}
	},
	
	onRemove: function() {
		var me = this,
			mdl = me.getModule();
			
		if (mdl) {
			mdl.on('removecomplete', me.onAfterRemove, me, {single: true});
			
			mdl.removed();
		}
	},
	
	onAfterRemove: function() {
		this.close();
	},
	
	onSwitch: function() {
		var me = this,
			mdl = me.getModule();
		
		if (me.fireEvent('beforeswitch', me, mdl) === false) return;
		
		var action = me.action,
			params = {view: action != 'view'},
			
			btnSave = me.BUTTON_SAVE,
			btnRemove = me.BUTTON_REMOVE,
			btnSwitch = me.BUTTON_SWITCH,
			
			keyColumn = mdl.getKeyColumn(),
			keyValue = mdl.getKeyValue();

		if (!Ext.isEmpty(keyColumn, false) && !Ext.isEmpty(keyValue, false)) {
			params[keyColumn] = keyValue;
		}

		me.action = action == 'view' ? 'update' : 'view';
		me.controlToolbar();
		
		btnSave.disable();
		btnRemove.disable();
		btnSwitch.disable();

		me.redirect(params);
	},
	
	/**
	 * 控制工具条中按钮的状态
	 * @param String action
	 */
	controlToolbar: function(action) {
		var me = this,
			mdl = me.getModule(),
			
			action = action || me.action,
			
			btnSave = me.BUTTON_SAVE,
			btnRemove = me.BUTTON_REMOVE,
			btnSwitch = me.BUTTON_SWITCH;

		if (action == 'create') {
			btnSave.enable();
			btnSave.show();
			btnRemove.hide();
			btnSwitch.hide();
			
		} else if (action == 'view') {
			btnSwitch.setText('编辑');
			
			btnSwitch.enable();
			btnSave.hide();
			btnRemove.hide();
			btnSwitch.show();
			
		} else if (action == 'viewOnly') {
			btnSave.hide();
			btnRemove.hide();
			btnSwitch.hide();
			
		} else if (action == 'update') {
			btnSwitch.setText('查看');
			
			btnSave.enable();
			btnRemove.enable();
			btnSwitch.enable();
			btnSave.show();
			btnRemove.show();
			btnSwitch.show();
			
		} else if (action == 'updateOnly') {
			btnSave.enable();
			btnSave.show();
			btnRemove.hide();
			btnSwitch.hide();

		}
		
		// 没有删除的URL则禁用删除按钮
		/*if (Ext.isEmpty(mdl.removeUrl, false))
			btnRemove.hide();*/
			
		// 没有保存的URL则禁用保存按钮
		/*if (Ext.isEmpty(mdl.saveUrl, false)) 
			btnSave.hide();*/
		
		// me.fireEvent('buttonrender', me, mdl);
	},
	
	//////////////////////////////////////////////////
	
	/**
	 * 以创建方式打开窗口
	 * @param {Object} params
	 */
	create: function(params) {
		var me = this;
		
		me.action = 'create';
		
		me.open(params);
	},
	
	/**
	 * 以修改方式打开窗口(此方式允许切换至只读方式)
	 * @param {Object} params
	 */
	update: function(params) {
		var me = this;
			
		me.action = 'update';

		me.open(params);
	},
	
	/**
	 * 仅以修改方式打开窗口(此方式不允许切换至只读方式)
	 */
	updateOnly: function(params) {
		var me = this;
			
		me.action = 'updateOnly';

		me.open(params);
	},
	
	/**
	 * 以只读方式打开窗口(此方式允许切换至修改方式)
	 * @param {Object} params 需要传递的参数,该值不为空时view方法会创建一个维护窗口
	 */
	view: function(params) {
		var me = this;
			
		me.action = 'view';
		
		params = params || {};
		params.view = true;
		
		me.open(params);
	},
	
	/**
	 * 仅以只读方式打开窗口(此方式不允许切换至修改方式)
	 * @param {Object} params
	 */
	viewOnly: function(params) {
		var me = this;
			
		me.action = 'viewOnly';
			
		params = params || {};
		params.view = true;
		
		me.open(params);
	},
	
	////////////////////////////////////
	
	onModuleLoaded: function(mc, mdl) {
		framework.widgets.window.MaintainWindow.superclass.onModuleLoaded.apply(this, arguments);
		
		var me = this,
		
			keyMap = me.getKeyMap(),
			defaultActionKeyMap = mdl.getDefaultActionKeyMap();
		
		defaultActionKeyMap.disable();
		keyMap.addBinding({
			key: [10, 13],
			shift: true,
			fn: me.onSave,
			scope: me
		});
	},
	
	/**
	 * Module中的数据已经加载完毕
	 * @param {MaintainModule} mdl
	 */
	onModuleLoadDataComleted: function(mdl) {
		this.controlToolbar();
	},
	
	/**
	 * @override
	 */
	open: function(params) {
		params = params || {};
		
		this.BUTTON_SAVE.hide();
		this.BUTTON_REMOVE.hide();
		this.BUTTON_SWITCH.hide();
		
		params.action = this.action;
		
		framework.widgets.window.MaintainWindow.superclass.open.call(this, params);
	}
	
});