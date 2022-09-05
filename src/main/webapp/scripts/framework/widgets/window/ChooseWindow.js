_package('framework.widgets.window');

_import([
	'framework.widgets.window.ModuleWindow'
]);

framework.widgets.window.ChooseWindow = Ext.extend(framework.widgets.window.ModuleWindow, {
	initComponent: function() {
		var me = this;
		
		me.tbar = [
			me.BUTTON_OK = new Ext.Button({
				text: '确定',
				iconCls: "ok",
				handler: me.select,
				scope: me
			})
		];
		
		me.addEvents(
			/**
			 * @event select
			 * @param ChooseWindow win
			 * @param Array<Object> selections
			 */
			'select'
		);
		
		framework.widgets.window.ChooseWindow.superclass.initComponent.call(me);
	},
	
	select: function() {
		var me = this,
			mdl = me.getModule(),
			
			selections = [];
			
		if (!Ext.isEmpty(mdl)) {
			if (Ext.isDefined(mdl.getSelections)) {
				selections = mdl.getSelections();
			} else {
				var pnl = mdl.centerPnl;
				if (pnl instanceof Ext.grid.GridPanel) {
					selections = pnl.getSelectionModel().getSelections();
				}
			}
			
			for (var i = 0; i < selections.length; i++) {
				var data = selections[i];
				if (data instanceof Ext.data.Record) {
					selections[i] = data.data;
				}
			}
		}
		
		me.fireEvent('select', me, selections);
		
		me.close();
	}
});