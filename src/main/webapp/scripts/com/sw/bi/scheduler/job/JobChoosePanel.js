_package('com.sw.bi.scheduler.job');

com.sw.bi.scheduler.job.JobChoosePanel = Ext.extend(Ext.Panel, {
	frame: false,
	layout: 'border',
	height: 220,
	
	/**
	 * 查询条件
	 * @type 
	 */
	condition: {},
	
	/**
	 * @property job
	 * @type Object
	 */
	job: null,
	
	/**
	 * @cfg 是否只允许查询虚拟作业
	 * @type Boolean
	 */
	onlyVirtual: false,
	
	initComponent: function() {
		var condition = {
			'chooseParent-eq': true ,// 标记此次查询为父任务选取的查询,后台需要根据此标记处理相应的逻辑
			//add by zhoushasha 2016/5/5 分组查看
			'userGroupId':USER_GROUP_ID
		};

		/*if (this.onlyVirtual === true) {
			condition['jobType-eq'] = 100;
		}*/
		
		var store = new Ext.data.JsonStore({
			autoLoad: false,
			url: 'job/paging',
			totalProperty: "total",
			root: "paginationResults",
			remoteSort: true,
			fields: ['jobId', 'cycleType', 'jobName', 'jobType', 'dayN', 'jobLevel', 'jobStatus', 'jobTime'],
			
			listeners: {
				beforeload: this.onBeforeLoad,
				scope: this
			},
			
			baseParams: {condition: condition}
		});
		
		this.items = [{
			region: 'west',
			border: false,
			title: '父作业',
			width: '40%',
			layout: 'anchor',
			
			items: {
				xtype: 'grid',
				anchor: '99.8% 100%',
				
				columns: [{
					xtype: 'customcolumn',
					header: '作业优先级',
					dataIndex: 'jobLevel',
					width: 150,
					store: S.create('jobLevel'),
					renderer: function(newValue, value, meta, record) {
						if (record.get('lower') === true) {
							meta.style += 'color:red;';
						}
						
						return newValue;
					}
				}, {
					header: '作业ID',
					dataIndex: 'jobId',
					width: 60
				}, {
					header: '作业名称',
					dataIndex: 'jobName',
					width: 400,
					align: 'left'
				}, {
					xtype: 'customcolumn',
					header: '作业周期',
					dataIndex: 'cycleType',
					width: 60,
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
					header: '作业类型',
					dataIndex: 'jobType',
					store: S.create('jobType'),
					width: 260
				}, {
					xtype: 'customcolumn',
					header: '作业状态',
					dataIndex: 'jobStatus',
					store: S.create('jobStatus'),
					width: 60
				}],
				
				store: new Ext.data.Store({
					proxy: new Ext.data.MemoryProxy([]),
					reader: new Ext.data.JsonReader({
						fields: ['jobId', 'jobName', 'cycleType', 'jobType', 'dayN', 'jobLevel', 'lower', 'jobStatus', 'jobTime']
					})
				}),
				
				listeners: {
					rowdblclick: this.onParentJobDblClick,
					scope: this
				}
			}
		}, {
			region: 'center',
			border: false,
			title: '作业搜索',
			layout: 'anchor',
			items: [{
				xtype: 'textfield',
				name: 'keyword',
				anchor: '100%',
				emptyText: '作业名称',
				listeners: {
					blur: this.loadData,
					scope: this
				}
			}, {
				xtype: 'grid',
				anchor: '100% 89%',
				
				columns: [{
					header: '作业ID',
					dataIndex: 'jobId',
					width: 60
				}, {
					header: '作业名称',
					dataIndex: 'jobName',
					width: 400
				}, {
					xtype: 'customcolumn',
					header: '作业周期',
					dataIndex: 'cycleType',
					width: 60,
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
					header: '作业类型',
					dataIndex: 'jobType',
					store: S.create('jobType'),
					width: 240
				}, {
					xtype: 'customcolumn',
					header: '作业优先级',
					dataIndex: 'jobLevel',
					store: S.create('jobLevel')
				}, {
					xtype: 'customcolumn',
					header: '作业状态',
					dataIndex: 'jobStatus',
					store: S.create('jobStatus'),
					width: 60
				}],
				
				bbar: {
					xtype: 'paging',
					store: store,
					pageSize: 5
				},
			
				store: store,
				
				listeners: {
					rowdblclick: this.onSearchResultDblClick,
					scope: this
				}
			}]
		}];
		
		com.sw.bi.scheduler.job.JobChoosePanel.superclass.initComponent.call(this);
	},
	
	loadData: function() {
		this.grdSearchResult.store.load({params: {
			start: 0, 
			limit: 5
		}});
	},
	
	getValues: function() {
		var records = this.grdParentJob.store.getRange(),
			results = [];
		
		Ext.iterate(records, function(record) {
			results.push(record.data);
		});
		
		return results;
	},
	
	setValues: function(values) {
		var store = this.grdParentJob.store;
		
		store.loadData(values);
	},
	
	setReadOnly: function(readOnly) {
		this.setDisabled(false);
	},
	
	/////////////////////////////////////////////////////////////////////////////
	
	onRender: function() {
		this.grdParentJob = this.getComponent(0).getComponent(0);
		this.fldKeyword = this.getComponent(1).getComponent(0);
		this.grdSearchResult = this.getComponent(1).getComponent(1);
		
		com.sw.bi.scheduler.job.JobChoosePanel.superclass.onRender.apply(this, arguments);
	},
	
	onBeforeLoad: function(store, options) {
		var keyword = this.fldKeyword.getValue();
		
		/*if (Ext.isEmpty(keyword, false)) {
			return false;
		}*/

		var job = this.job,
			condition = Ext.apply(this.condition || {}, {
				'jobName': keyword,
				'jobId': job ? job.jobId : null
			});
		
		// 修改已上线作业时父作业只允许选取已经上线的作业
		if (job && !Ext.isEmpty(job.jobId, false) && job.jobStatus == JOB_STATUS.ONLINE) {
			condition['jobStatus-eq'] = JOB_STATUS.ONLINE;
		}
		
		if (this.onlyVirtual === true) {
			condition['jobType-eq'] = 100;
		} else {
			condition['jobType-eq'] = null;
		}
		
		store.baseParams.condition = Ext.apply(store.baseParams.condition, condition);
	},
	
	onSearchResultDblClick: function(grid, rowIndex, e) {		
		var mdlJobMaintain = this.ownerCt.ownerCt.ownerCt.ownerCt.ownerCt.ownerCt,
		
			store = this.grdParentJob.store,
			record = grid.getSelectionModel().getSelected(),
			jobId = record.get('jobId');

		if (Ext.isEmpty(store.queryUnique('jobId', jobId))) {
			store.add(new store.recordType(record.data, jobId));
		}
		
		mdlJobMaintain.validateParentJobLevel();
	},
	
	onParentJobDblClick: function(grid, rowIndex, e) {
		var record = grid.getSelectionModel().getSelected();
		grid.store.remove(record);
	}
});

Ext.reg('jobchoose', com.sw.bi.scheduler.job.JobChoosePanel);