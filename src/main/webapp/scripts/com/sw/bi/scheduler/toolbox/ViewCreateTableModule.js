_package('com.sw.bi.scheduler.toolbox');

com.sw.bi.scheduler.toolbox.ViewCreateTableModule = Ext.extend(framework.core.Module, {
	
	north: function() {
		return {
			xtype: 'form',
			frame: true,
			border: false,
			layout: 'column',
			
			items: [{
				columnWidth: .3,
				layout: 'form',
				border: false,
				labelWidth: 40,
				
				items: {
					xtype: 'textfield',
					name: 'dbName',
					fieldLabel: '数据库',
					allowBlank: false
				}
			}, {
				columnWidth: .3,
				layout: 'form',
				border: false,
				labelWidth: 35,
				
				items: {
					xtype: 'textfield',
					name: 'tableName',
					fieldLabel: '表名',
					allowBlank: false
				}
			}, {
				columnWidth: .03,
				items: {
					xtype: 'button',
					iconCls: 'search',
					tooltip: '查询',
					handler: this.loadData,
					scope: this
				}
			}]
		};
	},
	
	center: function() {
		return {
			xtype: 'panel',
			autoScroll: true,
			bodyStyle: 'padding:5px;'
		};
	},
	
	loadData: function() {
		var form = this.northPnl.form;
		
		if (!form.isValid()) {
			return;
		}
		
		var body = this.centerPnl.body
			loadMask = new Ext.LoadMask(body, {msg: '正在加载建表语句,请耐心等候...'});
			
		loadMask.show();	
		
		Ext.Ajax.request({
			url: 'toolbox/viewCreateTable',
			params: {
				dbName: form.findField('dbName').getValue(),
				tableName: form.findField('tableName').getValue()
			},
			
			success: function(response) {
				var sql = null;
				if (!Ext.isEmpty(response.responseText, false)) {
					sql = Ext.decode(response.responseText);
				}
				
				body.update(Ext.isEmpty(sql, false) ? null : sql.replace(/\n/g, '<br>').replace(/\s/g, '&nbsp;'));
				
				loadMask.hide();
			},
			
			failure: function() {
				loadMask.hide();
			}
		});
			
		
		
		/*(function() {
			var sql = framework.syncRequest({
				url: 'toolbox/viewCreateTable?tableName=' + form.findField('tableName').getValue(),
				decode: true
			});
			
			body.update(sql.replace(/\n/g, '<br>').replace(/\s/g, '&nbsp;').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;'));
			loadMask.hide();
		}).defer(1);*/
	}
});