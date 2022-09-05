_package('com.sw.bi.scheduler.hudson');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.hudson.HudsonProjectModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'hudsonProject',
	
	maintain: {
		title: 'Hudson项目维护',
		width: 500,
		height: 200,
		
		module: 'com.sw.bi.scheduler.hudson.HudsonProjectMaintainModule'
	},

	searcher: function() {
		return {
			items: [{
				xtype: 'hidden', name: 'userGroupId', value: USER_GROUP_ID
			}, {
				name: 'name',
				fieldLabel: '项目名称'
			}]
		};
	},
	
	detailer: function() {
		return {
			actions: [{
				iconCls: 'publish',
				tooltip: '发布',
				handler: this.publish,
				scope: this
			}, {
				iconCls: 'log',
				tooltip: '查看发布日志',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (Ext.isEmpty(record.get('publishLogFile'), false)) {
						cc.setCellLink(row, col, false);
					}
					
					return newValue;
				},
				handler: this.viewLog,
				scope: this
			}],
			
			columns: [{
				header: '项目名称',
				dataIndex: 'name',
				width: 150
			}, {
				header: 'SVN目录',
				dataIndex: 'svnPath',
				align: 'left',
				width: 200
			}, {
				header: '发布目录',
				dataIndex: 'localPath',
				width: 150
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'createBy',
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '发布状态',
				dataIndex: 'publishStatus',
				store: S.create('hudsonPublishStatus')
			}, {
				xtype: 'customcolumn',
				header: '最近发布开始时间',
				dataIndex: 'publishStartTime',
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '最近发布结束时间',
				dataIndex: 'publishEndTime',
				dateTimeColumn: true
			}],
			
			store: {
				fields: ['hudsonProjectId', 'publishLogFile'],
				sortInfo: {
					field: 'publishStartTime',
					direction: 'DESC'
				}
			}
		};
	},
	
	publish: function(record) {
		var mdl = this;
		
		Ext.Msg.confirm('提示', '是否需要发布 "' + record.get('name') + '" 项目?', function(btn) {
			if (btn !== 'yes') return;
			
			Ext.Ajax.request({
				timeout: 300000000,
				url: 'hudsonProject/publish',
				// waitMsg: '正在发布 "' + record.get('name') + '", 请耐心等候...',
				params: {
					hudsonProjectId: record.get('hudsonProjectId')
				}/*,
				success: function(response) {
					if (Ext.isEmpty(response.responseText, false)) {
						record.set('state', '更新失败.')
					} else {
						var result = Ext.decode(response.responseText);
						record.set('state', '更新成功, 最新版本: ' + result);
					}
				}*/
			});
			
			mdl.loadData.defer(500, mdl, [0]);
		});
	},
	
	viewLog: function(record) {
		framework.createWindow({
			title: '发布日志查看',
			iconCls: 'log',
			
			module: {
				module: 'com.sw.bi.scheduler.hudson.HudsonPublishLogModule',
				logFile: record.get('publishLogFile')
			}
			
		}, 'framework.widgets.window.ModuleWindow').open();
	}
	
});