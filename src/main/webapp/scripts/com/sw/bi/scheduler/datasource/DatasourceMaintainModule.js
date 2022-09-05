_package('com.sw.bi.scheduler.datasource');

_import([
	'framework.modules.MaintainModule'
]);

com.sw.bi.scheduler.datasource.DatasourceMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	/**
	 * 连接字符串模板
	 * @type 
	 */
	connectionTemplate: {
		0: new Ext.XTemplate('jdbc:mysql://{ip}:{port}/{databaseName}?yearIsDateType=false&amp;useUnicode=true&amp;characterEncoding={charset}'),
		1: new Ext.XTemplate('jdbc:sqlserver://{ip}:{port};databaseName={databaseName}'),
		2: new Ext.XTemplate('jdbc:oracle:thin:@{ip}:{port}:{databaseName}'),
		7: new Ext.XTemplate('jdbc:postgresql://{ip}:{port}/{databaseName}'),
		//add by zhoushasha 2016/05/10 17:45:10
		9: new Ext.XTemplate('mongodb://{username}:{password}@{ip}:{port}/{databaseName}'),
		// add by lfh 2022/08/25
		10: new Ext.XTemplate('jdbc:goldilocks://{ip}:{port}/{databaseName}'),
	},
	
	initModule: function() {
		this.tbuttons = [{
			text: '测试数据源',
			handler: this.testDatasource,
			scope: this
		}];
		
		com.sw.bi.scheduler.datasource.DatasourceMaintainModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			beforesave: this.onBeforeSave,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'datasource',
			
			items: [{
				xtype: 'hidden', name: 'active', value: true
			}, {
				columnWidth: .5,
				xtype: 'combo',
				hiddenName: 'type',
				fieldLabel: '类型',
				allowBlank: false,
				labelWidth: 55,
				store: S.create('datasourceType'),
				
				listeners: {
					select: this.joinConnectionString,
					scope: this
				}
			}, {
				columnWidth: .5,
				xtype: 'combo',
				hiddenName: 'viewType',
				fieldLabel: '查看类型',
				allowBlank: false,
				store: S.create('datasourceViewType')
			}, {
				columnWidth: 1,
				name: 'name',
				fieldLabel: '名称',
				allowBlank: false,
				labelWidth: 55,
				anchor: '100%'
			}, {
				columnWidth: .7,
				name: 'ip',
				fieldLabel: '服务器',
				labelWidth: 55,
				
				listeners: {
					blur: this.joinConnectionString,
					scope: this
				}
			}, {
				columnWidth: .3,
				name: 'port',
				fieldLabel: '端口',
				labelWidth: 40,
				anchor: '100%',
				
				listeners: {
					blur: this.joinConnectionString,
					scope: this
				}
			}, {
				columnWidth: .5,
				name: 'databaseName',
				fieldLabel: '数据库名',
				labelWidth: 55,
				
				listeners: {
					blur: this.joinConnectionString,
					scope: this
				}
			}, {
				xtype: 'combo',
				columnWidth: .5,
				hiddenName: 'charset',
				labelWidth: 45,
				fieldLabel: '字符集',
				anchor: '100%',
				store: S.create('charset'),
				listeners: {
					blur: this.joinConnectionString,
					scope: this
				}
			}, {
				columnWidth: .5,
				name: 'username',
				fieldLabel: '用户名',
				labelWidth: 55
			}, {
				columnWidth: .5,
				name: 'password',
				fieldLabel: '密码',
				labelWidth: 45,
				anchor: '100%'
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'connectionString',
				fieldLabel: '连接串',
				labelWidth: 55,
				height: 50,
				anchor: '100%',
				readOnly: true
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'description',
				fieldLabel: '描述',
				anchor: '100%',
				labelWidth: 55,
				height: 180
			}]
		}
	},
	
	joinConnectionString: function() {
		var mdl = this,
		
			fldCharset = mdl.findField('charset'),
			fldIp = mdl.findField('ip'),
			fldPort = mdl.findField('port'),
			fldDatabaseName = mdl.findField('databaseName'),
			fldConn = mdl.findField('connectionString'),
			//add by zhoushasha   2016/05/10 17:45:10
			fldUsername=mdl.findField('username'),
			tpl = mdl.connectionTemplate[mdl.findField('type').getValue()];
			
		if (Ext.isEmpty(tpl)) {
			fldCharset.hide();
			fldDatabaseName.hide();
			fldConn.hide();
			
			fldConn.setValue(null);
			
		} else {
			fldCharset.show();
			fldDatabaseName.show();
			fldConn.show();
			
			fldConn.setValue(tpl.apply({
				charset: fldCharset.getValue(),
				ip: fldIp.getValue(),
				port: fldPort.getValue(),
				//add by zhoushasha  2016/05/10 17:45:10
				username:fldUsername.getValue(),
				password:'{password}',//mongodb连接不采用连接串，但保留
				databaseName: fldDatabaseName.getValue()
			}));
		}
	},
	
	testDatasource: function() {
		var form = this.masterPnl.form;
		if (!form.isValid()) {
			return;
		}
		
		Ext.Ajax.request({
			timeout: 1000 * 60 * 20,
			url: 'datasource/test',
			params: {
				'datasource': Ext.encode(form.getValues())
			},
			waitMsg: '正在连接数据源,请耐心等候...',
			success: function(response) {
				Ext.Msg.alert('提示', Ext.decode(response.responseText));
			}
		});
	},
	
	onLoadDataComplete: function(mdl, data) {
		var action = mdl.moduleWindow ? mdl.moduleWindow.action : null;
		
		if (action != null && action != 'create') {
			mdl.findField('type').setReadOnly(true);
			mdl.findField('password').hide();
		}
	},
	
	onBeforeSave: function(mdl, data) {
		if (Ext.isEmpty(data['datasource'].createBy, false)) {
			data['datasource'].createBy = USER_ID;
		}
		
		data['datasource'].updateBy = USER_ID;
	}
});