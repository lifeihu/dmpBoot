_package('com.sw.bi.scheduler.statistic');

_import([
	'com.sw.bi.scheduler.job.JobModule'
]);

com.sw.bi.scheduler.statistic.JobCycleTypeModule = Ext.extend(framework.core.Module, {
	
	center: function() {
		return {
			xtype: 'grid',
			height: 190,
			
			columns: [{
				xtype: 'customcolumn',
				header: '周期',
				dataIndex: 'cycleType',
				store: S.create('jobCycleType'),
				renderer: function(newValue, value) {
					return Ext.isEmpty(value, false) ? '总计' : newValue;
				}
			}, {
				xtype: 'customcolumn',
				header: 'DataX作业',
				dataIndex: 'dataxCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('dataxCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['dataxCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: 'HiveSQL作业',
				dataIndex: 'hiveCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('hiveCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['hiveCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: 'Mapreduce作业',
				dataIndex: 'mapreduceCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('mapreduceCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['mapreduceCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: 'Shell脚本作业',
				dataIndex: 'shellCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('shellCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['shellCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: 'FTP作业',
				dataIndex: 'ftpCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('ftpCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['ftpCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '存储过程',
				dataIndex: 'procedureCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('procedureCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['procedureCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '虚拟作业',
				dataIndex: 'virtualCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('virtualCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['virtualCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '邮件发送作业',
				dataIndex: 'mailCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('mailCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['mailCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '报表质量监控作业',
				dataIndex: 'reportCount',
				width: 120,
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('reportCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['reportCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '依赖检验作业',
				dataIndex: 'dependencyCount',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('dependencyCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['dependencyCount'], true),
				scope: this
			}, {
				xtype: 'customcolumn',
				header: '小计',
				dataIndex: 'total',
				renderer: function(newValue, value, meta, record, row, col, store, grid, cc) {
					if (record.get('totalCount') == 0) {
						cc.setCellLink(row, col, false);
					}
					return newValue;
				},
				handler: this.onJobTypeClick.createDelegate(this, ['totalCount'], true),
				scope: this
			}],
			
			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'statistic/statisticJobCycleType',
				fields: ['cycleType', 'dataxCount', 'hiveCount', 'mapreduceCount', 'shellCount', 'ftpCount', 'procedureCount', 'virtualCount', 'mailCount', 'reportCount', 'dependencyCount', 'total']
			})
		};
	},
	
	south: function() {
		var mdl = this;
		
		return new com.sw.bi.scheduler.job.JobModule({
			autoLoadData: false,
			onModuleRender: mdl.onJobModuleRender.createDelegate(mdl)
		});
	},
	
	doLayout: function() {
		com.sw.bi.scheduler.statistic.JobCycleTypeModule.superclass.doLayout.apply(this, arguments);
		
		var mdl = this,
			centerHeight = mdl.centerPnl.height;

		if (Ext.isNumber(centerHeight)) {
			var height = mdl.getHeight(true);
			mdl.southPnl.setHeight(height - centerHeight);
		}
	},
	
	onModuleRender: function(mdl) {
		var frmSearch = mdl.southPnl.northPnl.form,
		
			fldCycleType = frmSearch.findField('cycleType-eq'),
			fldJobType = frmSearch.findField('jobType-in'),
			fldJobStatus = frmSearch.findField('jobStatus-eq');
			
		fldCycleType.setReadOnly(true);
		fldJobType.setReadOnly(true);
		fldJobStatus.setReadOnly(true);
	},
	
	onJobModuleRender: function(mdl) {
		com.sw.bi.scheduler.job.JobModule.superclass.onModuleRender.call(this, mdl);

		if (this.autoLoadData === false) {
			mdl.autoLoadData = null;
		}
	},
	
	onJobTypeClick: function(record, cc, grid, row, col, e, jobType) {
		var mdl = this,
			mdlJob = mdl.southPnl,
			
			jobTypes = [];
		
		if (jobType == 'dataxCount') {
			jobTypes.push(30, 31, 32, 33, 34, 50, 51, 52, 53, 4, 60, 61, 62, 63, 2, 70, 71, 72, 73, 3, 80, 81, 82, 83, 1, 101);
		} else if (jobType == 'hiveCount') {
			jobTypes.push(20);
		} else if (jobType == 'mapreduceCount') {
			jobTypes.push(21);
		} else if (jobType == 'shellCount') {
			jobTypes.push(40);
		} else if (jobType == 'ftpCount') {
			jobTypes.push(5, 6, 7);
		} else if (jobType == 'procedureCount') {
			jobTypes.push(42);
		} else if (jobType == 'virtualCount') {
			jobTypes.push(100);
		} else if (jobType == 'mailCount') {
			jobTypes.push(90);
		} else if (jobType == 'reportCount') {
			jobTypes.push(91);
		} else if (jobType == 'dependencyCount') {
			jobTypes.push(92, 93);
		}
		
		mdlJob.setCondition({
			'jobId-in': '',
			'jobName': '',
			'jobBusinessGroup': '',
			'dutyOfficer-eq': '',
			'alert': '',
			'jobLevel-in': '',
			
			'cycleType-eq': record.get('cycleType'),
			'jobType-in': jobTypes.join(','),
			'jobStatus-eq': JOB_STATUS.ONLINE
			
		});
		
		mdlJob.loadData(0);
	}
	
});