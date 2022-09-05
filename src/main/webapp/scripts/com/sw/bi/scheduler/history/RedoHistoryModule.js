_package('com.sw.bi.scheduler.history');

_import([
	'framework.modules.SearchGridModule'
]);

com.sw.bi.scheduler.history.RedoHistoryModule = Ext.extend(framework.modules.SearchGridModule, {
	model: 'redoAndSupplyHistory',
	
	actionViewConfig: false,
	actionUpdateConfig: false,
	
	searcher: function() {
		return {
			items: [{
				xtype: 'hidden', name: 'isMaster-eq', value: true
			}, {
				xtype: 'hidden', name: 'flag-le', value: 1
			}, {
				columnWidth: .15,
				xtype: 'combo',
				fieldLabel: '最近七天',
				store: S.create('sevenDays'),
				listeners: {
					select: function(combo) {
						var date = combo.getValue();
						if (date == null) {
							return;
						}
						
						this.northPnl.form.findField('operateDate-ge').setValue(date);
						this.northPnl.form.findField('operateDate-le').setValue(date);
						this.loadData(0);
					},
					scope: this
				}
			}, {
				columnWidth: .15,
				xtype: 'datefield',
				name: 'operateDate-ge',
				fieldLabel: '操作日期',
				value: new Date().add(Date.DAY, -7),
				allowBlank: false
			}, {
				columnWidth: .12,
				xtype: 'datefield',
				name: 'operateDate-le',
				fieldLabel: '至',
				value: new Date(),
				allowBlank: false
			}, {
				columnWidth: .12,
				xtype: 'combo',
				hiddenName: 'taskStatus',
				fieldLabel: '任务状态',
				store: S.create('taskStatus')
			}, {
				columnWidth: .15,
				name: 'jobId-in',
				fieldLabel: '作业ID',
				vtype: 'mutilnum'
			}, {
				columnWidth: .12,
				name: 'jobName',
				fieldLabel: '作业名称'
			}, {
				columnWidth: .12,
				xtype: 'combo',
				hiddenName: 'dutyMan-eq',
				fieldLabel: '责任人',
				store: S.create('users')
			}/*, {
				columnWidth: .12,
				xtype: 'combo',
				hiddenName: 'taskStatus-eq',
				fieldLabel: '任务状态',
				store: S.create('taskStatus')
			}*/]
		};
	},
	
	detailer: function() {
		return {
			allowDefaultButtons: false,
			
			/*actions: [{
				iconCls: 'maintain',
				tooltip: '明细',
				handler: this.viewDetail,
				scope: this
			}],*/
			
			columns: [{
				header: '操作批号',
				dataIndex: 'operateNo',
				width: 150
			}, {
				xtype: 'customcolumn',
				iconCls: 'maintain',
				handler: this.viewDetail,
				scope: this,
				renderer: function() { return null; }
			}, {
				header: '作业ID',
				dataIndex: 'jobId',
				width: 80
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 400,
				renderer: function(value, meta, record) {
					var cycleType = record.get('cycleType'),
						settingTime = record.get('settingTime');

					if (!Ext.isEmpty(settingTime, false)) {
						meta.attr += ' ext:qtip="<b>预设时间</b>: ' + settingTime + '"';
						
						if (cycleType == JOB_CYCLE_TYPE.HOUR || cycleType == JOB_CYCLE_TYPE.MINUTE) {
							var hm = settingTime.substring(settingTime.indexOf(' ') + 1, settingTime.lastIndexOf(':'));
							value += '(' + hm + ')';
						}
					}
					
					return value;
				}
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyMan',
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '任务状态',
				dataIndex: 'taskStatus',
				store: S.create('taskStatus')
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '操作日期',
				dataIndex: 'operateDate',
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '操作人',
				dataIndex: 'operateMan',
				store: S.create('users')
			}, {
				header: '操作人用户组',
				dataIndex: 'operateUserGroup.name'
			}, {
				xtype: 'customcolumn',
				header: '创建时间',
				dataIndex: 'createTime',
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '开始时间',
				dataIndex: 'beginTime',
				dateTimeColumn: true
			}, {
				xtype: 'customcolumn',
				header: '结束时间',
				dataIndex: 'endTime',
				dateTimeColumn: true
			}, {
				header: '时长',
				width: 40,
				renderer: function(value, meta, record) {
					var beginTime = record.get('beginTime'),
						endTime = record.get('endTime');

					if (!Ext.isEmpty(beginTime, false) && !Ext.isEmpty(endTime, false)) {
						return parseInt((Date.parseDate(endTime, 'Y-n-j H:i:s').getTime() - Date.parseDate(beginTime, 'Y-n-j H:i:s').getTime()) / 1000 / 60, 10);
					}
					
					return null;
				}
			}],
			
			store: {
				url: 'redoAndSupplyHistory/redoMasters',
				fields: ['cycleType', 'settingTime', 'operateUserGroup.userGroupId']
			}
		};
	},
	
	viewDetail: function(record) {
		this.createWindow({
			
			module: {
				module: 'com.sw.bi.scheduler.history.RedoHistoryListModule',
				condition: {
					operateNo: record.get('operateNo')
				}
			}
		}, 'framework.widgets.window.ModuleWindow').open();
	}
});