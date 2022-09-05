_package('framework.widgets.plugins');

_import([
	'framework.widgets.MaintainWindow',
	'framework.widgets.container.ModuleContainerWindow'
]);

framework.widgets.plugins.MaintainComboPlugin = Ext.extend(Ext.util.Observable, {
	maintainTriggerClass: 'x-form-maintain-trigger',
	
	/**
	 * @cfg maintain
	 * @type {Object}
	 * @description 维护窗口配置信息
	 */
	
	/**
	 * @property combo
	 * @param {Ext.form.ComboBox} combo
	 */
	
	/**
	 * @property moduleWindow
	 * @type {framework.widgets.container.ModuleContainerWindow/framework.widgets.MaintainWindow} 
	 */
	
	/**
	 * @cfg completeEvent
	 * @type String
	 * @description 维护完成后触发的事件，在调用的Module中必须实现该参数指定的事件
	 */
	completeEvent: 'savecomplete',
	
	constructor: function(config) {
		Ext.apply(this, config);
		framework.widgets.plugins.MaintainComboPlugin.superclass.constructor.apply(this, arguments);
	},
	
	init: function(combo) {
		this.combo = combo;

		if (!Ext.isEmpty(this.adRefId, false)) {
			this.maintain = {
				title: '基础代码',
				url: '/as/ad/admin/editAdRefList.html?adRefId=' + this.adRefId,
				module: 'as.ad.ref.AdRefListMaintainModule',
				width: 550,
				height: 500
			};
		}
		
		this.initModuleWindow();
		
		this.combo.triggerConfig = {
            tag:'span', cls:'x-form-twin-triggers', cn:[
            {tag: "img", src: Ext.BLANK_IMAGE_URL, cls: "x-form-trigger " + this.combo.triggerClass},
            {tag: "img", src: Ext.BLANK_IMAGE_URL, cls: "x-form-trigger " + this.maintainTriggerClass}
        ]};
        
        this.combo.addEvents(
			/**
			 * @event savecomplete
			 * @param {framework.modules.MaintainModule} mdl
			 * @param {Object} data
			 */
			this.completeEvent
		);
        
        this.combo.on(this.completeEvent, this.onComplete, this);
        this.combo.initTrigger = this.initTrigger.createDelegate(this, [this.combo]);
	},
	
	/**
	 * 初始化弹出窗口
	 */
	initModuleWindow: function() {
		var maintain = this.combo.maintain || this.maintain,
			url = maintain.url,
			module = maintain.module,
			completeEvent = this.completeEvent;
		
		if (Ext.isEmpty(url, false)) {
			if (Ext.isString(module))
				module = {module: module};
				
			this.moduleWindow = new framework.widgets.container.ModuleContainerWindow(maintain);
			this.moduleWindow.on('moduleload', function(mcw, mdl) {
				if (!mdl) return;
				this.combo.relayEvents(mdl, [completeEvent]);
			}, this, {single: true});
		} else {
			this.moduleWindow = new parent.framework.widgets.MaintainWindow(maintain);
			this.combo.relayEvents(this.moduleWindow, [completeEvent]);
		}
	},
	
	initTrigger: function(combo) {
		var trgs = combo.trigger.select('.x-form-trigger', true);
		trgs.each(function(trg) {
			if (trg.hasClass(this.maintainTriggerClass)) {
				combo.mon(trg, 'click', this['onMaintainClick'], this, {preventDefault:true});
			} else {
				combo.mon(trg, 'click', this.combo['onTriggerClick'], combo, {preventDefault:true});
			}
			
			trg.addClassOnOver('x-form-trigger-over');
            trg.addClassOnClick('x-form-trigger-click');
		}, this);
	},
	
	onMaintainClick: function() {
		if (this.combo.disabled) return;
		
		var mw = this.moduleWindow;
			
		this.combo.collapse();
		if (mw.create)
			mw.create();
		else
			mw.show();
	},
	
	onComplete: function(mdl, data) {
		if (!this.combo.store) return;

		S.clearCacheData(this.combo.store.type);
		
		this.combo.mode = 'remote';
		this.combo.lastQuery = null;
		this.combo.store.on('load', function() { 
			this.combo.mode = 'local';
		}, this.combo, {single: true});
	}
});
Ext.preg('maintaincomboplugin', framework.widgets.plugins.MaintainComboPlugin);