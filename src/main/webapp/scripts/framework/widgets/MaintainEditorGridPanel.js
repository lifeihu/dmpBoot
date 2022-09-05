_package('framework.widgets');

_import([
	'framework.widgets.EditorGridPanel',
	'framework.widgets.plugins.MaintainPanelPlugin'
]);

framework.widgets.MaintainEditorGridPanel = Ext.extend(framework.widgets.EditorGridPanel, {
	
	initComponent: function() {
		var me = this,
			masterPnl = me.module.masterPnl;
		
		me.plugins = [].concat(me.plugins || []).concat(Ext.ComponentMgr.createPlugin({ptype: 'maintainpanelplugin'}));
		
		if (Ext.isEmpty(me.removeUrl, false) && !Ext.isEmpty(masterPnl)) {
			// 自动创建删除明细的URL
			me.removeUrl = /*masterPnl.model + '/' + */me.model + '/remove';
		}
		
		framework.widgets.MaintainEditorGridPanel.superclass.initComponent.call(this);
	},
	
	afterRender: function() {
		framework.widgets.MaintainEditorGridPanel.superclass.afterRender.apply(this, arguments);
		
		var grid = this;
		
		(function() {
			Ext.destroy(grid.header);
			grid.header = null;
		}).defer(1);
	},
	
	doLayout: function() {
		var grid = this,
			ct = grid.container;

		if (ct) {
			grid.setSize(ct.getSize(true));
		}

		framework.widgets.MaintainEditorGridPanel.superclass.doLayout.apply(this, arguments);
	}
});

Ext.reg('maintaineditorgrid', framework.widgets.MaintainEditorGridPanel);