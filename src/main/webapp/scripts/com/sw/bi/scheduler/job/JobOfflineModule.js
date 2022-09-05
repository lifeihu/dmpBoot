_package('com.sw.bi.scheduler.job');

_import([
	'framework.widgets.form.DateTimeField'
]);

com.sw.bi.scheduler.job.JobOfflineModule = Ext.extend(framework.core.Module, {
	
	/**
	 * @property job
	 * @type Number
	 */
	job: null,
	
	initModule: function() {
		this.buttons = [{
			text: '下线',
			iconCls: 'ok',
			handler: this.offline,
			scope: this
		}];
		
		this.addEvents(
			'success'
		);
		
		com.sw.bi.scheduler.job.JobOfflineModule.superclass.initModule.call(this);
	},
	
	north: function() {
		return {
			xtype: 'form',
			labelWidth: 60,
			
			items: [{
				xtype: 'hidden', name: 'downMan', value: USER_ID
			}, {
				xtype: 'textfield',
				name: 'jobName',
				fieldLabel: '下线作业',
				value: this.job.jobName,
				readOnly: true
			}, {
				xtype: 'textfield',
				name: 'downManName',
				fieldLabel: '下线人',
				value: USER_NAME,
				readOnly: true
			}, {
				xtype: 'datetimefield',
				name: 'downTime',
				fieldLabel: '下线时间',
				value: new Date(),
				readOnly: true
			}, {
				xtype: 'textarea',
				name: 'downReason',
				fieldLabel: '下线原因',
				height: 180,
				allowBlank: false
			}]
		};
	},
	
	center: function() {
		return {
			xtype: 'grid',
			title: '"' + this.job.jobName + '" 作业具有以下子作业',
			
			columns: [new Ext.grid.RowNumberer({width: 35}), {
				header: '作业ID',
				dataIndex: 'jobId'
			}, {
				header: '作业名称',
				dataIndex: 'jobName',
				width: 300
			}, {
				xtype: 'customcolumn',
				header: '责任人',
				dataIndex: 'dutyOfficer',
				width: 100,
				sortable: true,
				store: S.create('users')
			}, {
				xtype: 'customcolumn',
				header: '手机',
				dataIndex: 'dutyOfficer',
				width: 100,
				sortable: true,
				store: S.create('users'),
				renderer: function(newValue, value, metaData, record, row, col, store, grid, cc) {
					var user = cc.store.queryUnique('userId', value);
					if (user != null) {
						return user.get('mobilePhone');
					}
					
					return null;
				}
			}, {
				xtype: 'customcolumn',
				header: '用户组',
				dataIndex: 'userGroup.name'
			}],
			
			store: new Ext.data.JsonStore({
				autoLoad: true,
				url: 'job/children',
				fields: ['jobId', 'jobName', 'dutyOfficer', 'userGroup.name', 'userGroup'],
				baseParams: {jobId: this.job.jobId}
			})
		};
	},
	
	offline: function() {
		var mdl = this,
			
			jobId = mdl.job.jobId,
			jobName = mdl.job.jobName,
		
			form = mdl.northPnl.form,
			store = mdl.centerPnl.store;
			
		if (!form.isValid()) {
			return;
		}
		
		var msg = '';
		if (store.getCount() > 0) {
			msg = '"' + jobName + '" 作业具有 ' + store.getCount() + ' 个子作业,是否一起下线?';
		} else {
			msg = '是否下线 "' + jobName + '" 作业?';
		}
		
		Ext.Msg.confirm('提示', msg, function(btn) {
			if (btn != 'yes') return;
			
			var result = form.getValues(),
				records = store.getRange(),
				
				jobIds = [jobId];
				
			Ext.iterate(records, function(record) {
				jobIds.push(record.get('jobId'));
			});
			result.jobIds = jobIds;
			
			delete result.jobName;
			delete result.downManName;

			Ext.Ajax.request({
				url: 'job/offline',
				params: result,
				waitMsg: '正在下线作业,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '"' + jobName + '" 作业已成功下线!', function() {
						mdl.fireEvent('success');
						mdl.moduleWindow.close();
					});
				},
				
				failure: function() {
					Ext.Msg.hide();
				}
			});
		});
	}
});