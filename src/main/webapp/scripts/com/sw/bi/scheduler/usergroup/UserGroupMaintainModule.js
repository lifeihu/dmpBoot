_package('com.sw.bi.scheduler.usergroup');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.usergroup.UserGroupMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,

	master: function() {
		var parentId = null;
		if (this.action == 'create') {
			parentId = this.node == null || this.node.id == 'root' ? null : this.node.id;
		} else {
			parentId = this.node.parentNode.id;
		}
		
		return {
			model: 'userGroup',
			
			items: [{
				xtype: 'hidden', name: 'parentId', value: parentId
			}, {
				xtype: 'hidden', name: 'sortNo'
			}, {
				xtype: 'hidden', name: 'active', value: true
			}, {
				columnWidth: 1,
				name: 'parent.name',
				fieldLabel: '上级用户组',
				labelWidth: 70,
				readOnly: true,
				value: this.action == 'create' ? this.node.text : this.node.parentNode.text
			}, {
				columnWidth: 1,
				name: 'name',
				fieldLabel: '名称',
				allowBlank: false,
				labelWidth: 70,
				maxLength: 100
			}, {
				columnWidth: 1,
				name: 'hiveDatabase',
				fieldLabel: 'Hive数据库',
				labelWidth: 70,
				maxLength: 200
			}, {
				columnWidth: 1,
				name: 'hdfsPath',
				fieldLabel: '数据目录',
				labelWidth: 70,
				maxLength: 1000,
				value: this.action == 'create' ? this.node.attributes.hdfsPath : this.node.parentNode.attributes.hdfsPath
			}, {
				xtype: 'combo',
				columnWidth: 1,
				hiddenName: 'administrator',
				fieldLabel: '超级用户组',
				labelWidth: 70,
				allowBlank: false,
				store: S.create('yesNo'),
				value: false
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'description',
				fieldLabel: '描述',
				labelWidth: 70,
				height: 100,
				maxLength: 1000
			}]
		};
	}
	
});