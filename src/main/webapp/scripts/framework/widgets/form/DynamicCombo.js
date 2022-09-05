_package('framework.widgets.form')

framework.widgets.form.DynamicCombo = Ext.extend(Ext.form.ComboBox, {
	minChars: 1,
	anyMatch: true,
	mode: 'remote',
	pageSize: 10,
	listWidth: 250,
	triggerAction: 'all',
	queryParam: 'name',
	editable: true,
	
	/**
	 * 是否匹配到记录
	 * @type Boolean
	 */
	matched: false,
	
	/**
	 * @cfg constraint
	 * @type Boolean
	 * @description 在未匹配到记录时是不是强制清空输入结果
	 */
	constraint: true,
	
	initComponent: function() {
		framework.widgets.form.DynamicCombo.superclass.initComponent.apply(this, arguments);
		
		this.on('select', this.onSelected, this);
	}, 
	
	onBlur: function() {
		framework.widgets.form.DynamicCombo.superclass.onBlur.call(this);
		
		if (!this.matched) {
			if (this.constraint) this.setValue(this.value);
			else this.value = this.getRawValue();
		}
		
		this.matched = false;
	},
	
	onKeyDown: function() {
		framework.widgets.form.DynamicCombo.superclass.onKeyDown.call(this);
		
		this.matched = false;
	},
	
	onSelected: function(combo, rec) {
		this.matched = true;
	}
});

Ext.reg('dynamiccombo', framework.widgets.form.DynamicCombo);