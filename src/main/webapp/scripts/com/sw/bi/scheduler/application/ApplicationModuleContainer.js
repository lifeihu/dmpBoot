_package('com.sw.bi.scheduler.application');

_import([
	'framework.widgets.container.ModuleContainer'
]);

com.sw.bi.scheduler.application.ApplicationModuleContainer = Ext.extend(framework.widgets.container.ModuleContainer, {
	border: true,
	title: '当前位置: ' + PROJECT_NAME,
	
	autoModule: 'com.sw.bi.scheduler.task.TaskModule', // 'com.sw.bi.scheduler.job.DataXMaintainModule',
	
	initModule: function() {
		var mdl = this;
		
		Ext.apply(mdl, {
			tools: [{
				id: 'refresh',
				// handler: mdl.loadResource.createDelegate(mdl, [mdl.resource], 0),
				handler: mdl.reload.createDelegate(mdl),
				scope: mdl
			}]/*,
			
			tbar: [{
				text: '打印'
			}]*/
		});
		
		com.sw.bi.scheduler.application.ApplicationModuleContainer.superclass.initModule.call(this);
	},
	
	reload: function() {
		this.loadResource(this.resource);
	},
	
	loadResource: function(resource) {
		var mdl = this,
			resource = resource || mdl.resource;

		if (Ext.isEmpty(resource)) 
			return;
		
		var moduleClazz = resource.url,
			parent = resource,
			path = [];

		if (Ext.isEmpty(moduleClazz, false)) 
			return;
			
		mdl.resource = resource;
		
		while (parent) {
			path.push(parent.name);
			parent = parent.parent;
		}
		
		mdl.setTitle('当前位置: ' + path.reverse().join( ' > '));
		
		mdl.loadModule(moduleClazz);
	}
});

Ext.reg('applicationmodulecontainer', com.sw.bi.scheduler.application.ApplicationModuleContainer);