_package('framework.widgets');

_import([
	'framework.widgets.plugins.MaintainPanelPlugin'
]);

framework.widgets.MaintainFormPanel = MaintainFormPanel = Ext.extend(Ext.FormPanel, {
	border: true,
	
	/**
	 * @property module
	 * @type framework.core.Module
	 */
	
	initComponent: function() {
		var me = this;
		me.plugins = [].concat(me.plugins || []).concat(Ext.ComponentMgr.createPlugin({ptype: 'maintainpanelplugin'}));
		
		me.initDefaultItems();
		
		framework.widgets.MaintainFormPanel.superclass.initComponent.call(this);
	},
	
	initDefaultItems: function() {
		var me = this,
			items = me.items;

		items = [{
			xtype: 'hidden',
			name: me.module.getModelId(me.model)
		}, {
			xtype: 'hidden',
			name: 'createTime',
			value: new Date().format('Y-m-d H:i:s')
		}, {
			xtype: 'hidden',
			name: 'updateTime',
			value: new Date().format('Y-m-d H:i:s')
		}].concat(items);
		
		for (var i = 0, len = items.length; i < len; i++) {
			var item = items[i],
			
				// fieldLabel = item.fieldLabel,
				hideLabels = item.hideLabels,
				columnWidth = item.columnWidth || .2,
				labelWidth = item.labelWidth || 60/*!fieldLabel ? 60 : fieldLabel.length * 15*/;
			delete item.columnWidth;
			delete item.labelWidth;

			if (item.xtype == 'hidden')
				continue;
		
			items[i] = {
				columnWidth: columnWidth,
				layout: 'form',
				border: false,
				labelWidth: labelWidth,
				hideLabels: hideLabels === true,
				
				items: Ext.apply({
					xtype: 'textfield'
				}, item)
			};
		}
		
		me.items = {
			layout: 'column',
			border: false,
			items: [].concat(items)
		};
	},
	
	/**
	 * 获得主键名称
	 * @return {String}
	 */
	getKeyColumn: function() {
		var me = this,
		
			keyColumn = me.keyColumn,
			model = me.model;
		
		if (Ext.isEmpty(keyColumn, false))
			me.keyColumn = keyColumn = Ext.isEmpty(model, false) ? null : model + 'Id';

		return keyColumn;
	},
	
	/**
	 * 获得主键值
	 * @return 
	 */
	getKeyValue: function() {
		return this.form.findField(this.getKeyColumn()).getValue();
		// return this.getData()[this.getKeyColumn()];
	},
	
	getMonitoringValidateField: function() {
		return this.form.items;
	},
	
	validate: function() {
		return this.form.isValid();
	},
	
	setReadOnly: function(readonly) {
		this.readonly = readonly;
		
		this.form.items.each(function(field) {
			if (field.initialConfig.disabled === true)
				field.disable();
			else
				field.setReadOnly(readonly);
		});
	},
	
	startLoad: function(data) {
		this.form.setValues(data);
	},
	
	getData: function() {
		return this.form.getValues();
	}
});

Ext.reg('maintainform', framework.widgets.MaintainFormPanel);