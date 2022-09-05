_package('com.sw.bi.scheduler.hudson');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.hudson.HudsonProjectMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,

	initModule: function() {
		com.sw.bi.scheduler.hudson.HudsonProjectMaintainModule.superclass.initModule.call(this);
		
		this.on({
			// loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'hudsonProject',
			
			items: [{
				xtype: 'hidden', name: 'publishStatus'
			}, {
				xtype: 'hidden', name: 'publishStartTime'
			}, {
				xtype: 'hidden', name: 'publishEndTime'
			}, {
				xtype: 'hidden', name: 'publishLogFile'
			}, {
				columnWidth: 1,
				name: 'svnPath',
				fieldLabel: 'SVN目录',
				listeners: {
					scope: this,
					blur: this.onSvnPathBlur
				}
			}, {
				columnWidth: 1,
				name: 'name',
				fieldLabel: '项目名称',
				emptyText: '可以由SVN目录中自动生成'
			}, {
				columnWidth: 1,
				name: 'localPath',
				fieldLabel: '发布目录',
				emptyText: '可以由SVN目录中自动生成'
			}, {
				columnWidth: 1,
				xtype: 'combo',
				fieldLabel: '责任人',
				hiddenName: 'createBy',
				store: S.create('users'),
				value: USER_ID
			}]
		};
	},
	
	onSvnPathBlur: function(fld) {
		var svnPath = fld.getValue().replace(/^\//, ''),
			name = svnPath.substring(0, svnPath.indexOf('/'));

		this.findField('name').setValue(name);
		this.findField('localPath').setValue(name);
	},
	
	onBeforeSave: function(mdl, data) {
		if (Ext.isEmpty(data['hudsonProject'].createBy, false)) {
			data['hudsonProject'].createBy = USER_ID;
		}
		
		data['hudsonProject'].updateBy = USER_ID;
	}
	
});