_package('com.sw.bi.scheduler.gateway');

_import([
	'framework.modules.MaintainModule',
	'framework.widgets.form.MultiCombo'
]);

com.sw.bi.scheduler.gateway.GatewayMaintainModule = Ext.extend(framework.modules.MaintainModule, {
	minHeight: 0,
	
	initModule: function() {
		com.sw.bi.scheduler.gateway.GatewayMaintainModule.superclass.initModule.call(this);
		
		this.on({
			loaddatacomplete: this.onLoadDataComplete,
			scope: this
		});
	},
	
	master: function() {
		return {
			model: 'gateway',
			
			items: [{
				columnWidth: .7,
				name: 'name',
				fieldLabel: '名称',
				allowBlank: false,
				maxLength: 50
			}, {
				columnWidth: .3,
				xtype: 'combo',
				hiddenName: 'master',
				fieldLabel: '主网关',
				allowBlank: false,
				value: false,
				store: S.create('yesNo')
			}, {
				columnWidth: .7,
				name: 'ip',
				fieldLabel: 'IP',
				allowBlank: false,
				maxLength: 50
			}, {
				columnWidth: .3,
				xtype: 'numberfield',
				name: 'port',
				fieldLabel: '端口',
				allowBlank: false
			}, {
				columnWidth: .5,
				name: 'userName',
				fieldLabel: '登录用户',
				allowBlank: false,
				maxLength: 50
			}, {
				columnWidth: .5,
				name: 'password',
				fieldLabel: '密码',
				allowBlank: false,
				maxLength: 50
			}, {
				columnWidth: .3,
				xtype: 'combo',
				hiddenName: 'status',
				fieldLabel: '状态',
				allowBlank: false,
				store: S.create('gatewayStatus')
			}, {
				columnWidth: .7,
				xtype: 'multicombo',
				hiddenName: 'tailNumber',
				fieldLabel: '执行尾号',
				allowBlank: false,
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						['0', '0'],
						['1', '1'],
						['2', '2'],
						['3', '3'],
						['4', '4'],
						['5', '5'],
						['6', '6'],
						['7', '7'],
						['8', '8'],
						['9', '9']
					]
				})
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'schedulerWay',
				fieldLabel: '网关机调度方式',
				labelWidth: 120,
				allowBlank: false,
				readOnly: true,
				store: S.create('schedulerWay')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'roundWay',
				fieldLabel: '网关机轮循方式',
				labelWidth: 120,
				allowBlank: false,
				store: S.create('roundWays')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'referToJobLevel',
				fieldLabel: '选取任务优先级',
				labelWidth: 120,
				allowBlank: false,
				store: S.create('referJobLevel')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'taskRunningPriority',
				fieldLabel: '参考点选取',
				labelWidth: 120,
				allowBlank: false,
				store: S.create('taskRunningPriority')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'referPointRandom',
				fieldLabel: '参考点选取是否随机',
				labelWidth: 120,
				allowBlank: false,
				store: S.create('referPointRandom')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'taskFailReturnTimes',
				fieldLabel: '任务出错重跑次数',
				labelWidth: 120,
				allowBlank: false,
				store: new Ext.data.SimpleStore({
					fields: ['value', 'name'],
					data: [
						[0, 0],
						[1, 1],
						[2, 2],
						[3, 3],
						[4, 4],
						[5, 5],
						[6, 6],
						[7, 7],
						[8, 8],
						[9, 9],
						[10, 10]
					]
				})
			}, {
				columnWidth: 1,
				xtype: 'multicombo',
				hiddenName: 'jobType',
				fieldLabel: '允许执行的作业类型',
				labelWidth: 120,
				allowBlank: false,
				store: S.create('jobType')
			}, {
				columnWidth: 1,
				xtype: 'combo',
				hiddenName: 'useWhiteList',
				fieldLabel: '是否启用白名单',
				labelWidth: 120,
				allowBlank: false,
				store: S.create('yesNo')
			}, {
				columnWidth: 1,
				name: 'jobWhiteList',
				fieldLabel: '白名单作业ID',
				labelWidth: 120,
				readOnly: true,
				vtype: 'mutilnum'
			}, {
				columnWidth: 1,
				name: 'taskCountExceptJobs',
				fieldLabel: '统计执行任务数需要排除的作业ID',
				labelWidth: 190,
				vtype: 'mutilnum'
			}, {
				columnWidth: 1,
				name: 'taskRunningMax',
				fieldLabel: '调度系统同时运行的最大任务数',
				labelWidth: 190,
				// anchor: '50%',
				allowBlank: false
			}, {
				columnWidth: 1,
				name: 'waitUpdateStatusTaskCount',
				fieldLabel: '调度系统选取的最大参考点数',
				labelWidth: 190,
				// anchor: '50%',
				allowBlank: false
			}, {
				columnWidth: 1,
				name: 'disableSupplyHours',
				fieldLabel: '模拟方式禁止补数据时间点',
				labelWidth: 190,
				vtype: 'mutilnum'
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'hiveJdbc',
				fieldLabel: 'Hive JDBC',
				height: 60,
				maxLength: 200
			}, {
				columnWidth: 1,
				xtype: 'textarea',
				name: 'description',
				fieldLabel: '描述',
				height: 60,
				maxLength: 200
			}]
		};
	},
	
	onLoadDataComplete: function(mdl, data) {
		var gateway = data['gateway'];

		if (Ext.isEmpty(gateway)) {
			return;
		}
		
		var isReadOnly = !Ext.isEmpty(gateway.gatewayId, false);
		
		mdl.findField('name').setReadOnly(isReadOnly);
		mdl.findField('ip').setReadOnly(isReadOnly);
	}
	
});