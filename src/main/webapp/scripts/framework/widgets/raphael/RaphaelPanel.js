_package('framework.widgets.raphael');

_import([
	'framework.widgets.raphael.raphael'
]);

framework.widgets.raphael.RaphaelPanel = Ext.extend(Ext.FormPanel, {
	
	/**
	 * @type Paper 
	 */
	raphael: null,

	onRender: function() {
		framework.widgets.raphael.RaphaelPanel.superclass.onRender.apply(this, arguments);
		
		this.raphael = Raphael(this.body.id, 400, 600);
	}
	
});

Ext.reg('raphaelpanel', framework.widgets.raphael.RaphaelPanel);