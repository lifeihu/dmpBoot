_package('com.sw.bi.scheduler.task');

com.sw.bi.scheduler.task.TaskReferPointModule = Ext.extend(framework.core.Module, {
	
	north: function() {
		return {
			xtype: 'form',
			layout: 'column',
			margins: '0 0 2 0',
			frame: true,
			plain: true,
			border: false,
			
			items: [{
				columnWidth: .2,
				layout: 'form',
				border: false,
				labelWidth: 60,
				items: {
					xtype: 'datefield',
					fieldLabel: '任务日期',
					name: 'startDate',
					allowBlank: false,
					value: new Date()
				}
			}, {
				columnWidth: .15,
				layout: 'form',
				border: false,
				labelWidth: 10,
				items: {
					xtype: 'datefield',
					fieldLabel: '至',
					name: 'endDate',
					labelSeparator: ' ',
					allowBlank: false,
					value: new Date()
				}
			}, {
				columnWidth: .03,
				items: {
					xtype: 'button',
					iconCls: 'ok',
					tooltip: '生成参考点',
					handler: this.addReferPoint,
					scope: this
				}
			}]
		};
	},
	
	center: function() {
		return {};
		/*return {
			xtype: 'grid',

			columns: [new Ext.grid.RowNumberer(), {
				header: '作业ID',
				dataIndex: 'jobId'
			}, {
				header: '作业名称',
				dataIndex: 'taskName',
				width: 400
			}, {
				xtype: 'customcolumn',
				header: '扫描日期',
				dataIndex: 'scanDate',
				dateColumn: true
			}, {
				xtype: 'customcolumn',
				header: '任务日期',
				dataIndex: 'taskDate',
				dateColumn: true
			}],
			
			store: new Ext.data.Store({
				proxy: new Ext.data.MemoryProxy(),
				reader: new Ext.data.JsonReader({
					fields: [
						'jobId', 'taskName', 'taskDate', 'scanDate'
					]
				})
			})
		};*/
	},
	
	addReferPoint: function() {
		var form = this.northPnl.form;
		
		if (!form.isValid()) {
			return;
		}
		
		Ext.Msg.confirm('提示', '是否需要将指定日期范围内的所有任务的父任务加入参考点?', function(btn) {
			if (btn !== 'yes') return;
			
			Ext.Ajax.request({
				url: 'task/addReferPointsByTaskDate',
				params: form.getValues(),
				waitMsg: '正在处理指定任务日期范围任务的参考点,请耐心等候...',
				
				success: function() {
					Ext.Msg.alert('提示', '成功将父作业加入参考点.');
				}
			});
		});
	},
	
	onModuleRender: function(mdl) {
		mdl.setDefaultAction(mdl.addReferPoint, mdl);
		com.sw.bi.scheduler.task.TaskReferPointModule.superclass.onModuleRender.call(this, mdl);
	}
	
});