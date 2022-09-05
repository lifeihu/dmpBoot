_package('framework.widgets');

_import([
	'framework.widgets.ux.MultiSelect',
	'framework.widgets.ux.ItemSelector'
]);

framework.widgets.AssignPanel = Ext.extend(framework.widgets.MaintainFormPanel, {
	hideLabels: true,
	bodyStyle: 'padding:5px;',
	
	/**
	 * @cfg module
	 */
	
	/**
	 * @requires
	 * @cfg storeLeft
	 * @type Ext.data.Store
	 * @description 左边未分配数据源
	 */
	
	/**
	 * @requires
	 * @cfg storeRight
	 * @type Ext.data.Store
	 * @description 右边已分配数据源
	 */
	
	initComponent: function() {
		this.items = {
			xtype: 'itemselector',
            name: 'assignIds',
            fieldLabel: 'ItemSelector',
	        imagePath: framework.getUrl('/resources/images/ux/'),
	        
	        drawUpIcon: false,
		    drawDownIcon: false,
		    drawTopIcon: false,
		    drawBotIcon: false,
	        
            multiselects: [Ext.apply({
            	legend: '未分配',
                width: 250,
                height: 200,
                displayField: 'roleName',
                valueField: 'roleId'
            }, this.leftConfig), Ext.apply({
            	legend: '已分配',
                width: 250,
                height: 200,
                displayField: 'roleName',
                valueField: 'roleId'
            }, this.rightConfig)]
		};
		
		framework.widgets.AssignPanel.superclass.initComponent.call(this);
	},
	
	doLayout: function() {
		framework.widgets.AssignPanel.superclass.doLayout.apply(this, arguments);
		
		var me = this,
		
			itemSelector = me.getComponent(0),
		
			fromMultiselect = itemSelector.fromMultiselect,
			fromFs = fromMultiselect.fs,
			
			toMultiselect = itemSelector.toMultiselect,
			toFs = toMultiselect.fs,
			
			ct = me.body,
			csize = ct.getViewSize(true),
			
			w = csize.width / 2 - 20,
			h = csize.height - 10;

		itemSelector.el.first().first().first().setSize(csize.width, h);
		
		fromMultiselect.setSize(w, h);
		fromFs.setSize(w, h);
		
		toMultiselect.setSize(w, h);
		toFs.setSize(w, h);
		
		itemSelector.doLayout();
	},
	
	setReadOnly: function(readOnly) {
		this.setDisabled(readOnly);
	},
	
	/**
	 * @hide
	 */
	initDefaultItems: Ext.emptyFn
	
});

Ext.reg('assignpanel', framework.widgets.AssignPanel);