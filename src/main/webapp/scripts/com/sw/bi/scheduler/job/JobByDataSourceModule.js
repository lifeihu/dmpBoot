_package('com.sw.bi.scheduler.job');

com.sw.bi.scheduler.job.JobByDataSourceModule = Ext.extend(framework.core.Module, {
	
	/**
	 * 数据源ID
	 * @type 
	 */
	dataSourceId: null,
	
	center: function() {
		var mdl = this;
		
		return {
			xtype: 'grid',
			allowDefaultButtons: false,
			
			tbar: [{
				text: '复制作业',
				iconCls: 'copy',
				
				menu: {
					items: [{
						online: true,
						text: '复制上线作业',
						iconCls: 'copy'
					}, {
						online: false,
						text: '复制下线作业',
						iconCls: 'copy'
					}],
					
					listeners: {
						render: function(menu) {
							menu.items.each(function(mi) {
								mi.on('render', function(item) {
									var client = new ZeroClipboard(item.el.dom);
									client.on('copy', mdl.copyJobIds.createDelegate(mdl, [item.online === true], true));
								});
							});
						}
					}
				}
			}],
			
			columns: [{
				header: '作业ID',
				dataIndex: 'jobId',
				sortable: true,
				width: 80
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 370,
				sortable: true,
				align: 'left'
			}, {
				xtype: 'customcolumn',
				header: '作业状态',
				dataIndex: 'jobStatus',
				sortable: true,
				width: 70,
				store: S.create('jobStatus')
			}, {
				header: '用途',
				dataIndex: 'dataSourceUse',
				width: 80,
				tooltip: true
			}, {
				header: '业务组',
				dataIndex: 'jobBusinessGroup',
				width: 100,
				sortable: true
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				width: 100,
				sortable: true,
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				sortable: true,
				width: 50,
				store: S.create('jobCycleType')
			}, {
				header: '启动时间',
				dataIndex: 'jobTime',
				width: 60,
				renderer: function(value) {
					if (Ext.isEmpty(value, false)) {
						return null;
					}
					
					return value.indexOf(':') == -1 ? null : value;
				}
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'jobType',
				sortable: true,
				width: 240,
				store: S.create('jobType')
			}, {
				xtype: 'customcolumn',
				header: '优先级',
				dataIndex: 'jobLevel',
				sortable: true,
				width: 100,
				store: S.create('jobLevel')
			}, {
				xtype: 'customcolumn',
				header: '告警类型',
				sortable: true,
				dataIndex: 'alert',
				store: S.create('alertType')
			}],
			
			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'job/getJobsByDataSource',
				fields: [
					'jobId', 'jobName', 'jobBusinessGroup', 'dutyOfficer',
					'cycleType', 'jobTime', 'jobType', 'jobLevel', 'alert',
					'dataSourceUse', 'jobStatus'
				],
				baseParams: {
					dataSourceId: this.dataSourceId
				}
			})
		};
	},
	
	copyJobIds: function(event, online) {
		var jobIds = [],
			store = this.centerPnl.store;
		
		store.each(function(record) {
			var jobStatus = record.get('jobStatus');
			
			if (online === true && jobStatus == 1) {
				jobIds.push(record.get('jobId'));
				
			} else if (online !== true && (jobStatus == 0 || jobStatus == 2)) {
				jobIds.push(record.get('jobId'));
			}
		});

		ZeroClipboard.setData('text/plain', jobIds.length == 0 ? ' ' : jobIds.join(','));
	}
	
});