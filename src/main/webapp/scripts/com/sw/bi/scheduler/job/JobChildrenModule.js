_package('com.sw.bi.scheduler.job');

com.sw.bi.scheduler.job.JobChildrenModule = Ext.extend(framework.core.Module, {
	
	jobId: null,
	
	center: function() {
		return {
			xtype: 'grid',
			border: true,
			title: '子作业',
			
			columns: [{
				header: '作业ID',
				dataIndex: 'jobId',
				width: 80
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				align: 'left',
				width: 300
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
				header: '用户组',
				dataIndex: 'userGroup.name'
			}, {
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				sortable: true,
				width: 50,
				store: S.create('jobCycleType')
			}, {
				xtype: 'customcolumn',
				header: '类型',
				dataIndex: 'jobType',
				sortable: true,
				width: 240,
				store: S.create('jobType')
			}],
			
			store: new Ext.data.JsonStore({
				url: 'job/children',
				fields: ['jobId', 'jobName', 'jobBusinessGroup', 'dutyOfficer', 'userGroup.name', 'cycleType', 'jobType'],
				baseParams: {
					jobId: this.jobId,
					depth: 1
				}
			})
		};
	},
	
	south: function() {
		return {
			layout: 'border',
			border: false,
			height: 350,
			
			items: [{
				region: 'north',
				height: 175,
				border: false,
				
				items: {
					xtype: 'grid',
					border: true,
					height: 300,
					title: '前置作业',
					
					columns: [{
						header: '作业ID',
						dataIndex: 'jobId',
						width: 80
					}, {
						header: '作业名称',
						dataIndex: 'jobName',
						align: 'left',
						width: 300
					}, {
						xtype: 'customcolumn',
						header: '作业状态',
						dataIndex: 'jobStatus',
						width: 60,
						store: S.create('jobStatus')
					}, {
						header: '业务组',
						dataIndex: 'jobBusinessGroup',
						width: 100,
						sortable: true
					}, {
						xtype: 'customcolumn',
						header: '责任人',
						dataIndex: 'dutyOfficer',
						width: 80,
						sortable: true,
						store: S.create('users')
					}, {
						header: '用户组',
						dataIndex: 'userGroup.name'
					}, {
						xtype: 'customcolumn',
						header: '周期',
						dataIndex: 'cycleType',
						sortable: true,
						width: 50,
						store: S.create('jobCycleType')
					}, {
						xtype: 'customcolumn',
						header: '类型',
						dataIndex: 'jobType',
						sortable: true,
						width: 240,
						store: S.create('jobType')
					}],
					
					store: this.storeFront = new Ext.data.JsonStore({
						url: 'job/getFrontJobs',
						fields: ['jobId', 'jobName', 'jobBusinessGroup', 'dutyOfficer', 'userGroup.name', 'cycleType', 'jobType', 'jobStatus'],
						baseParams: {
							rearJobId: this.jobId
						}
					})
				}
			}, {
				region: 'center',
				border: false,
				
				items: {
					xtype: 'grid',
					border: true,
					height: 300,
					title: '后置作业',
					
					columns: [{
						header: '作业ID',
						dataIndex: 'jobId',
						width: 80
					}, {
						header: '作业名称',
						dataIndex: 'jobName',
						align: 'left',
						width: 300
					}, {
						xtype: 'customcolumn',
						header: '作业状态',
						dataIndex: 'jobStatus',
						width: 60,
						store: S.create('jobStatus')
					}, {
						header: '业务组',
						dataIndex: 'jobBusinessGroup',
						width: 100,
						sortable: true
					}, {
						xtype: 'customcolumn',
						header: '责任人',
						dataIndex: 'dutyOfficer',
						width: 80,
						sortable: true,
						store: S.create('users')
					}, {
						header: '用户组',
						dataIndex: 'userGroup.name'
					}, {
						xtype: 'customcolumn',
						header: '周期',
						dataIndex: 'cycleType',
						sortable: true,
						width: 50,
						store: S.create('jobCycleType')
					}, {
						xtype: 'customcolumn',
						header: '类型',
						dataIndex: 'jobType',
						sortable: true,
						width: 240,
						store: S.create('jobType')
					}],
					
					store: this.storeRear = new Ext.data.JsonStore({
						url: 'job/getRearJobs',
						fields: ['jobId', 'jobName', 'jobBusinessGroup', 'dutyOfficer', 'userGroup.name', 'cycleType', 'jobType', 'jobStatus'],
						baseParams: {
							rearJobId: this.jobId
						}
					})
				}
			}]
		};
	},
	
	loadData: function() {
		this.centerPnl.store.load();
		this.storeFront.load();
		this.storeRear.load();
	}
	
});