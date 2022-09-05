_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.NewDataXMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	minHeight: 1350,

	jobDatasyncConfig: {},
	
	master: function() {
		var master = com.sw.bi.scheduler.job.NewDataXMaintainModule.superclass.master.call(this),
			items = master.items;
			
		// 执行网关机
		items[3].store = S.create('gatewaysByJobType', 31);
		 console.log(items[3].store);	
		// 程序路径
		items[4].hidden = true;
		items[4].allowBlank = true;
			
		// 责任人
		items[12].columnWidth = .25;
		items[12].anchor = '98%';
		
		// 优先级
		items[13].columnWidth = .25;
		items[13].anchor = '97%';
		items[13].labelWidth = 40;
		
		// 告警
		items[14].columnWidth = .5;
		items[14].anchor = '49%';
		items[14].labelWidth = 30;
		
		// 描述
		items[items.length - 1].height = 80;
		
		items.push({
			columnWidth: 1,
			xtype: 'fieldset',
			title: '同步作业配置',
			anchor: '99%',
			
			items: [{
				xtype: 'fieldset',
				title: '自定义配置文件',
				labelWidth: 150,
				
				items: [{
					xtype: 'combo',
					hiddenName: 'notUseXml',
					fieldLabel: '请选择配置方式',
					store: S.create('ways'),
					anchor: '20%',
					value: 0,
					allowBlank: false,
					listeners: {
						select: this.onUseCustomXmlSelect,
						scope: this
					}
				}, {
					xtype: 'textfield',
					hidden: true,
					name: 'userXml',
					fieldLabel: 'Json配置文件'
				}]
			}, {
				id: 'errorConfig',
				xtype: 'fieldset',
				title: '容错/线程配置',
				labelWidth: 80,
				
				items: [{
					xtype: 'combo',
					hiddenName: 'threadNumber',
					fieldLabel: '使用线程数',
					allowBlank: false,
					anchor: '20%',
					store: new Ext.data.SimpleStore({
						fields: ['value', 'name'],
						data: [
							[1, 1], [2, 2], [3, 3], [4, 4], [5, 5],
							[6, 6], [7, 7], [8, 8], [9, 9], [10, 10]
						]
					}),
					value: 10
				}, {
					xtype: 'numberfield',
					name: 'errorthreshold',
					fieldLabel: '容错率',
					allowBlank: false,
					anchor: '20%'
				}, 
				// added by zhengdandan 2018年12月7日
				{
					xtype: 'numberfield',
					name: 'speedBytes',
					fieldLabel: '传输速度(byte/s)',
					allowBlank: false,
					anchor: '20%'
				}, {
					xtype: 'numberfield',
					name: 'errorLimitrecords',
					fieldLabel: '容错行数限制',
					allowBlank: false,
					anchor: '20%'
				}]
				
			}, {
				id: 'initConfig',
				xtype: 'fieldset',
				title: '配置初始化动作',
				labelWidth: 80,
				
				items: [{
					xtype: 'combo',
					hiddenName: 'initAction',
					fieldLabel: '初始化动作',
					store: S.create('yesNo'),
					anchor: '15%',
					value: false,
					allowBlank: false,
					listeners: {
						select: this.onInitActionSelect,
						scope: this
					}
				}, {
					xtype: 'combo',
					hidden: true,
					hiddenName: 'datasourceByInitDatasourceId.datasourceId',
					fieldLabel: '初始化数据源',
					// store: S.create('datasources', DATASOURCE_TYPE.DATABASE),
					store: S.create('databaseDatasources'),
					anchor: '40%'
				}, {
					xtype: 'textarea',
					hidden: true,
					name: 'initSql',
					fieldLabel: '初始化SQL',
					vtype: 'whereClause'
				}]
			}, {
				id: 'finalyConfig',
				xtype: 'fieldset',
				title: '配置清理动作',
				labelWidth: 90,
				
				items: [{
					xtype: 'combo',
					hiddenName: 'finalyAction',
					fieldLabel: '清理动作',
					store: S.create('yesNo'),
					anchor: '15%',
					value: false,
					allowBlank: false,
					listeners: {
						select: this.onFinalyActionSelect,
						scope: this
					}
				}, {
					xtype: 'combo',
					hidden: true,
					hiddenName: 'datasourceByFinalyDatasourceId.datasourceId',
					fieldLabel: '清理数据源',
					// store: S.create('datasources', DATASOURCE_TYPE.DATABASE),
					store: S.create('databaseDatasources'),
					anchor: '40%'
				}, {
					xtype: 'textarea',
					hidden: true,
					name: 'finalySuccessSql',
					fieldLabel: '成功后执行SQL',
					vtype: 'whereClause'
				}, {
					xtype: 'textarea',
					hidden: true,
					name: 'finalyFailSql',
					fieldLabel: '失败后执行SQL',
					vtype: 'whereClause'
				}]
			}, {
				id: 'datasourceConfig',
				xtype: 'panel',
				layout: 'column',
				border: false,
				
				items: [{
					columnWidth: .5,
					border: false,
					style: 'margin-right: 5px;',
					
					items: [{
						id: 'sourceConfig',
						xtype: 'fieldset',
						title: '来源数据配置',
						labelWidth: 95,
						
						items: [{
							xtype: 'combo',
							hiddenName: 'sourceDataType',
							fieldLabel: '来源数据类型',
							store: S.create('dataType'),
							allowBlank: false,
							anchor: '40%',
							value: 4,
							listeners: {
								select: this.onDataTypeSelect,
								scope: this
							}
						}, {
							xtype: 'combo',
							hidden: true,
							hiddenName: 'datasourceBySourceDatasourceId.datasourceId',
							fieldLabel: '来源数据源',
							store: S.create('datasources', DATASOURCE_TYPE.MYSQL),
							anchor: '50%',
							allowBlank: false
						}, {
							xtype: 'combo',
							hiddenName: 'sourceCharset',
							fieldLabel: '来源数据字符集',
							store: S.create('charset'),
							allowBlank: false,
							anchor: '40%'
						}, {
							xtype: 'textarea',
							name: 'sourceDatapath',
							fieldLabel: 'HDFS文件目录',
							allowBlank: false
						}, {
							xtype: 'textfield',
							name: 'sourceDelimiter',
							fieldLabel: '字段分隔符',
							value: ',',
							allowBlank: false
						}, {
							xtype: 'numberfield',
							hidden: true,
							name: 'sourceColumns',
							fieldLabel: '字段数'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'sourceTableName',
							fieldLabel: 'HBase表名'
						}, {
							xtype: 'textarea',
							hidden: false,
							allowBlank: false,
							name: 'sourceColumnFields',
							fieldLabel: 'HDFS的列',
							emptyText: '格式如：{"index": 0,"type": "long"},{"index": 1,"type": "boolean"}'
						}, {
							xtype: 'textarea',
							name: 'sourceCustomerParameter',
							fieldLabel: '客户自定义参数',
							vtype: 'jsonType',
							emptyText: '该项用于替换或追加parameters里的元素，必须为json格式'
						}
						]
					}]
				}, {
					columnWidth: .5,
					border: false,
					
					items: [{
						id: 'targetConfig',
						xtype: 'fieldset',
						title: '目标数据配置',
						labelWidth: 95,
						
						items: [{
							xtype: 'combo',
							hiddenName: 'targetDataType',
							fieldLabel: '目标数据类型',
							store: S.create('dataType'),
							allowBlank: false,
							anchor: '40%',
							listeners: {
								select: this.onDataTypeSelect,
								scope: this
							}
						}, {
							xtype: 'combo',
							hiddenName: 'datasourceByTargetDatasourceId.datasourceId',
							fieldLabel: '目标数据源',
							store: S.create('datasources', DATASOURCE_TYPE.MYSQL),
							anchor: '50%',
							allowBlank: false
						}, {
							xtype: 'combo',
							hidden: true,
							hiddenName: 'targetCharset',
							fieldLabel: '目标数据字符集',
							store: S.create('charset'),
							allowBlank: false,
							anchor: '40%'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'targetEscape',
							fieldLabel: 'Escape'
						}, {
							xtype: 'textarea',
							name: 'targetDatapath',
							fieldLabel: '数据导入SQL'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'targetDelimiter',
							fieldLabel: '字段分隔符',
							allowBlank: false
						}, {
							xtype: 'numberfield',
							hidden: true,
							name: 'targetColumns',
							fieldLabel: '字段数'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'referDbName',
							fieldLabel: '关联数据库'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'referTableName',
							fieldLabel: '关联表名'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'referPartName',
							fieldLabel: '关联分区名'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'targetTableName',
							fieldLabel: '表名'
						}, {
							xtype: 'textarea',
							hidden: true,
							name: 'targetColumnFields',
							fieldLabel: 'HBase列族及列',
							emptyText: '格式如："id","name"'
						}, {
							xtype: 'textarea',
							name: 'targetCustomerParameter',
							fieldLabel: '客户自定义参数',
							vtype: 'jsonType',
							emptyText: '该项用于替换或追加parameters里的元素，必须为json格式'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'specialConfig'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'test1',
							value: 'value1',
							cls: 'special'
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'test2',
							value: 'value2',
							cls: 'special'
						}
						]
					}]
				}]
			}]
		});
		
		return master;
	},
	
	syncJobTypeByDataType: function() {
		var mdl = this,
		
			sourceDataType = mdl.findField('sourceDataType').getValue(),
			targetDataType = mdl.findField('targetDataType').getValue(),
			
			
			jobType = DATA_TYPE_TO_JOB_TYPE[sourceDataType.toString(16) + '' + targetDataType.toString(16)],
			fldGateway = mdl.findField('gateway');
		
		console.log("syncJobTypeByDataType")
		console.log(sourceDataType)
		console.log(targetDataType)
		
		mdl.findField('jobType').setValue(jobType);
			
		if (!Ext.isEmpty(jobType, false)) {
			var executeGateway = fldGateway.getValue();
			fldGateway.setValue(null);
			
			// 如果执行网关机之前有值，则在根据类型加载完后需要将值设回去
			if (!Ext.isEmpty(executeGateway, false)) {
				fldGateway.store.on({
					load: function() {
						fldGateway.setValue(executeGateway);
					}
				});
			}
			
			fldGateway.store.load({params: {
				jobType: jobType
			}});
		} else {
			Ext.Msg.alert('提示', '不支持该类型的DataX作业.');
		}
	},
	
	syncDataTypeByJobType: function() {
		var mdl = this,
			
			jobType = mdl.findField('jobType').getValue();

		Ext.iterate(DATA_TYPE_TO_JOB_TYPE, function(dataType) {
			if (DATA_TYPE_TO_JOB_TYPE[dataType] == jobType) {
				var cmbSourceDataType = mdl.findField('sourceDataType'),
					cmbTargetDataType = mdl.findField('targetDataType');
					
				cmbSourceDataType.setValue(parseInt(dataType.charAt(0), 16));
				cmbTargetDataType.setValue(parseInt(dataType.charAt(1), 16));
				
				mdl.onDataTypeSelect(cmbSourceDataType);
				mdl.onDataTypeSelect(cmbTargetDataType);
			}
		})
	},
	
	/**
	 * 给指定路径最后加上'/'
	 * @param {} path
	 */
	appendSeparator: function(path) {
		if (!Ext.isEmpty(path, false) && path.charAt(path.length - 1) != '/') {
			return path + '/';
		}
		
		return path;
	},
	
	/////////////////////////////////////////////////////////////////
	
	onLoadDataComplete: function(mdl, data) {
		var job = data['job'],
			action = mdl.moduleWindow ? mdl.moduleWindow.action : null;		
		
		if (action == null || action == 'create') {
			mdl.findField('jobType').setValue(null);
			
			mdl.syncJobTypeByDataType();
			
		} else {
			if (action == 'updateOnly') {
				mdl.findField('notUseXml').setReadOnly(true);
				mdl.findField('sourceDataType').setReadOnly(true);
				mdl.findField('targetDataType').setReadOnly(true);
			}
			
			mdl.syncDataTypeByJobType();
		}
		
		var config = data['jobDatasyncConfig'];
		if (!Ext.isEmpty(config)) {
			delete config.jobId;
			// delete config.jobType;
			if (config.jobType === 103 || config.jobType === 104) {
				var cmbNotUseXml = mdl.findField('notUseXml');
				if (config.jobType === 103) {
				    cmbNotUseXml.setValue(1);
				} else if (config.jobType === 104){
					cmbNotUseXml.setValue(2);
				}
				mdl.onUseCustomXmlSelect(cmbNotUseXml);
				mdl.masterPnl.form.setValues(config);
			} else {
				if (!Ext.isEmpty(config.initSql, false)) {
					var cmbInitAction = mdl.findField('initAction');
					cmbInitAction.setValue(true);
					mdl.onInitActionSelect(cmbInitAction);
				} 
	
				if (!Ext.isEmpty(config.finalySuccessSql, false)) {
					var cmbFinalyAction = mdl.findField('finalyAction');
					cmbFinalyAction.setValue(true);
					mdl.onFinalyActionSelect(cmbFinalyAction);
				}
			}
            if(mdl.findField('datasourceBySourceDatasourceId.datasourceId')) {
            	mdl.findField('datasourceBySourceDatasourceId.datasourceId').store.on('load', function(){
            		mdl.masterPnl.form.setValues(config);
    			})	
            }
            if(mdl.findField('datasourceByTargetDatasourceId.datasourceId')) {
            	mdl.findField('datasourceByTargetDatasourceId.datasourceId').store.on('load', function(){
            		mdl.masterPnl.form.setValues(config);
    			})	
            }
			
			//mdl.masterPnl.form.setValues(config);
			
			//setTimeout(function() {
			//	mdl.masterPnl.form.setValues(config);
			//}, 500);
			
		}
		this.jobDatasyncConfig = config || {};
		
		com.sw.bi.scheduler.job.NewDataXMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
		// 作业查看时“来源数据源”字段宽度会变的很窄，暂时只能通过调整窗口大小来解决这个问题
		if (action == 'viewOnly') {
			mdl.moduleWindow.setWidth(mdl.moduleWindow.getWidth() - 5);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
		    specialConfig = '',
		    
			notUseXml = mdl.findField('notUseXml').getValue(),
			initAction = mdl.findField('initAction').getValue(),
			finalyAction = mdl.findField('finalyAction').getValue(),
			
			finalySuccessSql = job.finalySuccessSql,
			finalyFailSql = job.finalyFailSql,
			
			plg = mdl.getModuleValidatePlugin(),
			
			fields = [
				'errorthreshold',
			
				'datasourceBySourceDatasourceId.datasourceId',
				'sourceDatapath',
				'sourceDelimiter',
				'sourceColumns',
				
				'datasourceByTargetDatasourceId.datasourceId',
				'targetDatapath',
				'targetDelimiter',
				'targetColumns'
			];
		// specialConfig
		/*Ext.iterate(mdl.centerPnl.form.items.items, function(item) {
			if (item.initialConfig.cls == 'special') {
			    specialConfig += item.name + ':' + item.value + ','
			}
		});*/
		// console.log(specialConfig)
		// mdl.findField('specialConfig').setValue(specialConfig);
		if (notUseXml && finalyAction && Ext.isEmpty(finalySuccessSql, false) && Ext.isEmpty(finalyFailSql, false)) {
			plg.addError({
				msg: '和 <span style="color:#15428B;font-weight:bold;">"失败后执行SQL"</span> 必须填写一个',
				field: mdl.findField('finalySuccessSql')
			});
			plg.showErrors();
			
			return false;
		}
		
		// 当目标数据类型为HDFS时需要校验“关联表名”和“关联分区名”是否同时填写
		/*if (job.targetDataType == 4) {
			var isDbEmpty = Ext.isEmpty(job.referDbName, false),
				isTableEmpty = Ext.isEmpty(job.referTableName, false),
				isPartEmpty = Ext.isEmpty(job.referPartName, false);
				
			if (isTableEmpty && !isPartEmpty) {
				plg.addError({
					msg: '必须填写',
					field: mdl.findField('referTableName')
				});
				plg.showErrors();
				return false;
			}
			
			if (!isTableEmpty && isPartEmpty) {
				plg.addError({
					msg: '必须填写',
					field: mdl.findField('referPartName')
				});
				plg.showErrors();
				return false;
			}
			
			if ((!isTableEmpty || !isPartEmpty) && isDbEmpty) {
				plg.addError({
					msg: '必须填写',
					field: mdl.findField('referDbName')
				});
				plg.showErrors();
				return false;
			}
		}*/

		if (notUseXml === 0) {
			// 通用配置
			job.userXml = null;
			Ext.iterate(fields, function(name) {
				var fld = mdl.findField(name);

				if (!fld.isVisible()) {
					if (name.indexOf('.datasourceId') > -1) {
						delete job[name];
					} else {
						job[name] = null;
					}
				}
			});
			
		} else {
			// json/shell
			initAction = false;
			finalyAction = false;
			
			Ext.iterate(fields, function(name) {
				if (name.indexOf('.datasourceId') > -1) {
					delete job[name];
				} else {
					job[name] = null;
				}
			});
		}
		
		if (initAction !== true) {
			delete job['datasourceByInitDatasourceId.datasourceId'];
			job.initSql = null;
		} 
		
		if (finalyAction !== true) {
			delete job['datasourceByFinalyDatasourceId.datasourceId'];
			job.finalySuccessSql = null;
			job.finalyFailSql = null;
		}
		
        //来源数据类型为File时路径后面加'/'
		/*if (job.sourceDataType == 4 || job.sourceDataType == 5) {
			job.sourceDatapath = mdl.appendSeparator(job.sourceDatapath);
		}
		
		if (job.targetDataType == 4 || job.targetDataType == 5) {
			job.targetDatapath = mdl.appendSeparator(job.targetDatapath);
		}*/
		
		/*job.sourceCodec = job.sourceCodec === 'true' ? 'org.apache.hadoop.io.compress.GzipCodec' : '';
		job.targetCodec = job.targetCodec === 'true' ? 'org.apache.hadoop.io.compress.GzipCodec' : '';*/
		
		return com.sw.bi.scheduler.job.NewDataXMaintainModule.superclass.onBeforeSave.call(this, mdl, data);
	},
	
	onUseCustomXmlSelect: function(combo) {
		var mdl = this,
			value = combo.getValue(),
		
			cmbJobType = mdl.findField('jobType'),
			cmbUserXml = mdl.findField('userXml'),
			
			fsError = Ext.getCmp('errorConfig'),
			fsInit = Ext.getCmp('initConfig'),
			fsFinaly = Ext.getCmp('finalyConfig'),
			pnlDatasource = Ext.getCmp('datasourceConfig'),
			
			configFields = [
				'userXml',
				
				'errorthreshold',
				'speedBytes',
				'errorLimitrecords',
				'datasourceByInitDatasourceId.datasourceId', 
				'initSql',
				
				'datasourceByFinalyDatasourceId.datasourceId',
				
				'datasourceBySourceDatasourceId.datasourceId',
				'sourceDatapath',
				'sourceDelimiter',
				'sourceColumns',
				'sourceColumnFields',
				'sourceCharset',
				'sourceTableName',
	
				'datasourceByTargetDatasourceId.datasourceId',
				'targetDatapath',
				'targetDelimiter',
				'targetColumns',
				'targetColumnFields',
				'targetCharset',
				'targetTableName'
			];
		
		if (value === 0) {
			// 通用配置
			mdl.syncJobTypeByDataType();
			cmbUserXml.hide();
			fsError.show();
			fsInit.show();
			fsFinaly.show();
			pnlDatasource.show();
			cmbUserXml.setValue('');
			
		} else if (value === 1) {
			// json配置
			cmbJobType.setValue(103);
			
			cmbUserXml.show();
			cmbUserXml.setFieldLabel('自定义配置文件');
			fsError.hide();
			fsInit.hide();
			fsFinaly.hide();
			pnlDatasource.hide();
			
		} else {
			// shell-->python
			
			cmbJobType.setValue(104);
			
			fsError.hide();
			fsInit.hide();
			fsFinaly.hide();
			pnlDatasource.hide();
			
			// added by zhengdandan 2018年12月12日
			cmbUserXml.show();
			cmbUserXml.setFieldLabel('shell脚本调用python');

		}
		
		// 保证切换模式时各模式之间互不影响
		Ext.iterate(configFields, function(name) {			
			var field = mdl.findField(name);

			if (field.isVisible()) {
				field.allowBlank = value !== 0 ? true : false;
			}
		});
		
		cmbUserXml.allowBlank = value === 0 ? true : false;
	},
		
	onInitActionSelect: function(combo) {
		var mdl = this,
		
			cmbInitDatasource = mdl.findField('datasourceByInitDatasourceId.datasourceId'),
			fldInitSql = mdl.findField('initSql');

		if (combo.getValue()) {
			cmbInitDatasource.show();
			fldInitSql.show();
			
			cmbInitDatasource.allowBlank = false;
			fldInitSql.allowBlank = false;
			
		} else {
			cmbInitDatasource.hide();
			fldInitSql.hide();
			
			cmbInitDatasource.allowBlank = true;
			fldInitSql.allowBlank = true;
		}
	},
	
	onFinalyActionSelect: function(combo) {
		var mdl = this,
		
			cmbFinalyDatasource = mdl.findField('datasourceByFinalyDatasourceId.datasourceId'),
			fldFinalySuccessSql = mdl.findField('finalySuccessSql'),
			fldFinalyFailSql = mdl.findField('finalyFailSql');

		if (combo.getValue()) {
			cmbFinalyDatasource.show();
			fldFinalySuccessSql.show();
			fldFinalyFailSql.show();
			
			cmbFinalyDatasource.allowBlank = false;
			
		} else {
			cmbFinalyDatasource.hide();
			fldFinalySuccessSql.hide();
			fldFinalyFailSql.hide();
			
			cmbFinalyDatasource.allowBlank = true;
		}
	},
	
	onDataTypeSelect: function(combo) {
		var mdl = this,
			
			isSource = combo.getName() == 'sourceDataType',
			config = mdl.jobDatasyncConfig,
		    
			// isFTP = combo.getValue() == 3,
			
			fields = isSource ? {
				datasource: mdl.findField('datasourceBySourceDatasourceId.datasourceId'),
				datapath: mdl.findField('sourceDatapath'),
				delimiter: mdl.findField('sourceDelimiter'),
				columns: mdl.findField('sourceColumns'),
				//add by zhuzhogji 2015年9月11日09:38:31
				tablename: mdl.findField('sourceTableName'),
				columnfields: mdl.findField('sourceColumnFields'),
				// added by zhengdandan 2018年11月30日
				// fieldDelimiter: mdl.findField('sourceFieldDelimiter')
				charset: mdl.findField('sourceCharset')
			} : {
				datasource: mdl.findField('datasourceByTargetDatasourceId.datasourceId'),
				datapath: mdl.findField('targetDatapath'),
				delimiter: mdl.findField('targetDelimiter'),
				columns: mdl.findField('targetColumns'),
				escape: mdl.findField('targetEscape'),
				referDbName: mdl.findField('referDbName'),
				referTableName: mdl.findField('referTableName'),
				referPartName: mdl.findField('referPartName'),
				
				//add by zhuzhongji 2015年9月11日09:38:59
				tablename: mdl.findField('targetTableName'),
				columnfields: mdl.findField('targetColumnFields'),
				
				//added by zhengdandan 2018年11月30日
				Columns: mdl.findField('targetColumns'),
				// fieldDelimiter: mdl.findField('targetFieldDelimiter'),
				charset: mdl.findField('targetCharset')
			};
			
		mdl.syncJobTypeByDataType();
		
		/////////////////////////////////////////////////////////////

		if (isSource) {
			// 如果以后数据类型下拉框允许修改的话则需要使用下面注释的那行
			// mdl.findField('sourceDatapath').setValue(Ext.isEmpty(config['sourceDatapath']) ? null : config['sourceDatapath']);
			mdl.findField('sourceDatapath').setValue(null);
			mdl.findField('sourceCharset').setValue(DATA_TYPE_DEFAULT_CHARSET[combo.getValue()]);
			
		} else {
			// mdl.findField('targetDatapath').setValue(Ext.isEmpty(config['targetDatapath']) ? null : config['targetDatapath']);
			mdl.findField('targetDatapath').setValue(null);
			mdl.findField('targetCharset').setValue(DATA_TYPE_DEFAULT_CHARSET[combo.getValue()]);
		}

		/////////////////////////////////////////////////////////////
		
		Ext.iterate(fields, function(name) {
			var fld = fields[name];
			fld.hide();
			fld.allowBlank = true;
			if(fld.xtype !== 'combo') {
				fld.setValue('');
			}
		});
		console.log(combo.getValue())
		//modify by zhoushasha 2016年5月11日09:39:47
		switch (combo.getValue()) {
			case 0:
			case 1:
			case 2:
			case 7:
			case 9:
			//add by zhuzhongji 2015年9月11日09:39:47
			case 8:
      case 10:
				var datasource = fields.datasource,
					datapath = fields.datapath,
					columns = fields.columns,
					escape = fields.escape,
				
					//add by zhuzhongji 2015年9月11日09:40:21
					tablename = fields.tablename,
					columnfields = fields.columnfields,
				    charset = fields.charset;
				charset.show();
				charset.allowBlank = false;
				
				datasource.show();
				//datapath.show();
				
				datasource.store.load({params: {condition: {
					'userGroupId': USER_GROUP_ID,
					'type-eq': combo.getValue(),
					'active-eq': true
				}}});
				datasource.setValue(null);
				
				//datasource.allowBlank = false;
				//datapath.allowBlank = false;
				
				if (isSource) {
					// modify by zhuzhongji 2015年9月11日09:42:05
					datapath.vtype = 'whereClause';
					datapath.setFieldLabel('数据查询SQL');
					datapath.show();
					if(combo.getValue() == 0) {
//						datapath.hide();
//						datapath.allowBlank = true;
//						charset.hide();
//						charset.allowBlank = true;
//						columnfields.show();
//						columnfields.allowBlank = false;
//						columnfields.setFieldLabel('MySQL的列');
//						columnfields.emptyText = '格式如："id","name"';
//						columnfields.reset();
//						tablename.show();
//						tablename.allowBlank = false;
//						tablename.setFieldLabel('表名');
					}else if(combo.getValue() == 7) {
						columnfields.show();
						columnfields.allowBlank = false;
						columnfields.setFieldLabel('pg的列');
						columnfields.emptyText = '格式如："id","name"';
						columnfields.reset();
						tablename.show();
						tablename.allowBlank = false;
						tablename.setFieldLabel('表名');
						charset.hide();
						charset.allowBlank = true;
						datapath.hide();
						datapath.allowBlank = true;
					}else if(combo.getValue() == 8){
						tablename.show();
						columnfields.show();
						datapath.setFieldLabel('rowkey切分规则');
						datapath.setValue('full_table_scan');
					}else if(combo.getValue() == 9){
						charset = fields.charset;
						charset.hide();
						charset.allowBlank = true;

						tablename.show();
						tablename.setFieldLabel('db和集合(如：db.collection)');
						tablename.allowBlank=false;
						
						columnfields.allowBlank=false;
						columnfields.show();
						columnfields.setFieldLabel('db的列');
						columnfields.emptyText = '格式如：{"name": "pool_type","type": "string"},{"name": "frontcat_id","type": "Array","spliter": ""}';
						columnfields.reset();
						datapath.allowBlank=true;
						datapath.hide();
					}else{
						datapath.vtype = 'whereClause';
						datapath.setFieldLabel('数据查询SQL');
					}
				} else {
					if (combo.getValue() == 0 ) {
						var columnfields = fields.columnfields;
						
						charset.hide();
						charset.allowBlank = true;
						
						tablename.show();
						tablename.setFieldLabel("表名");
						
						columnfields.show();
						columnfields.setFieldLabel("MySQL的列");
						columnfields.allowBlank = false;
						columnfields.emptyText = '格式如："id","name"';
						columnfields.reset();
						tablename.allowBlank = false;
					} else if (combo.getValue() == 3) {
						
					}
					else if (combo.getValue() == 7) {
						tablename.setFieldLabel('表名');
						tablename.show();
						columnfields.show();
						tablename.allowBlank = false;
						columnfields.allowBlank = false;
						columnfields.setFieldLabel('列');
						columnfields.emptyText = '格式如："id","name"';
						columnfields.reset();
						charset.hide();
						charset.allowBlank = true;
					} 
					else if(combo.getValue() == 9){
						charset = fields.charset;
					    charset.hide();
					    charset.allowBlank = true;
	
						//add by zhoushasha 2016/05/10 14:23:00
						tablename.show();
						tablename.setFieldLabel('db和集合(如：db.collection)');
						//modify by zhoushasha 2016/6/3
						tablename.allowBlank=false;
						columnfields.allowBlank=false;
						
						columnfields.show();
						columnfields.setFieldLabel('db的列');
						columnfields.allowBlank = false;
						columnfields.emptyText = '格式如：{"name": "pool_type","type": "string"},{"name": "frontcat_id","type": "Array","spliter": ""}';
						columnfields.reset();
						datapath.allowBlank=true;
						datapath.hide();
					}
					//add by zhuzhongji 2015年9月11日09:42:35
					else if (combo.getValue() == 8){
						tablename.show();
						columnfields.show();
						//datapath.setFieldLabel('数据查询路径');
					}
					else if (combo.getValue() == 10 ) {
						var columnfields = fields.columnfields;
						
						charset.hide();
						charset.allowBlank = true;
						
						tablename.show();
						tablename.setFieldLabel("表名");
						
						columnfields.show();
						columnfields.setFieldLabel("Sundb的列");
						columnfields.allowBlank = false;
						columnfields.emptyText = '格式如："id","name"';
						columnfields.reset();
						tablename.allowBlank = false;
					}
					else {
						datapath.show();
						datapath.setFieldLabel('数据导入SQL');
						
					}
				}
			break;
			
			//add by zhuzhongji 2015-9-11 09:43:07
			case 3:
				var datasource = fields.datasource,
					datapath = fields.datapath,
					delimiter = fields.delimiter,
				    columnfields = fields.columnfields

				var charset = fields.charset;
				charset.show();
				charset.allowBlank = false;
				
				datasource.show();
				datapath.show();
				datapath.allowBlank = false;
				
				delimiter.show();
				delimiter.allowBlank = false;
				// columns.show();
				
				datasource.store.load({params: {condition: {
					'userGroupId': USER_GROUP_ID,
					'type-eq': combo.getValue(),
					'active-eq': true
				}}
				});
				datasource.setValue(null);
				
				datasource.allowBlank = false;
				
				datapath.setFieldLabel('FTP文件目录');
				datapath.vtype = null;
				if(isSource) {
					columnfields.allowBlank=false;
					columnfields.show();
					columnfields.setFieldLabel("FTP的列");
					columnfields.emptyText = '格式如：{"index": 0,"type": "long"},{"index": 1,"type": "date","format": "yyyy.MM.dd"}';
					columnfields.reset();
				}
			break;
			
			case 4:
				var datapath = fields.datapath,
					columns = fields.columns,
					delimiter = fields.delimiter,
				//	referDbName = fields.referDbName,
				//	referTableName = fields.referTableName,
				//	referPartName = fields.referPartName,
				// added by zhengdandan 2018年11月30日
				    columnfields = fields.columnfields;
                    columnfields.allowBlank=false;
					columnfields.show();
					columnfields.setFieldLabel("HDFS的列");
					columnfields.setValue('');
					if (isSource) {
					    columnfields.emptyText = '格式如：{"index": 0,"type": "long"},{"index": 1,"type": "boolean"}';
					    delimiter.setValue(',');
					}  else {
						columnfields.emptyText = '格式如：{"name": "col1","type": "TINYINT"},{"name": "col2","type": "SMALLINT"}';
					}
					
					columnfields.reset();
				
					var charset = fields.charset;
					charset.show();
					charset.allowBlank = false;
					
				// fieldDelimiter.show();
				// fieldDelimiter = false;
				datapath.show();
				datapath.allowBlank = false;
				
				datapath.setFieldLabel('HDFS文件目录');
				datapath.vtype = null;
				
				delimiter.show();
				delimiter.allowBlank = false;
			break;
			case 5:
			case 6:
				var datapath = fields.datapath;
				datapath.show();
				datapath.allowBlank = false;
				
				datapath.setFieldLabel('文件所在目录');
				datapath.vtype = null;
				if (combo.getValue() == 5) {
					var delimiter = fields.delimiter;
					delimiter.show();
					delimiter.setValue(',');
					delimiter.allowBlank = false;
				}
				if (isSource) {
					var columnfields = fields.columnfields,
					charset = fields.charset;
					
					columnfields.show();
					columnfields.allowBlank = false;
					columnfields.emptyText = '格式如：{"index": 0,"type": "long"},{"index": 1,"type": "date","format": "yyyy.MM.dd"}';
					columnfields.reset();
					
					charset.show();
					charset.allowBlank = false;
					charset.setValue('UTF-8');
					
					if (combo.getValue() == 5) {
						columnfields.setFieldLabel('File的列');	
					} else {
						columnfields.setFieldLabel('CSV的列');
					}
				}
			break;
		}
	}
});

Ext.apply(Ext.form.VTypes, {
	'whereClause': function(v) {
		if (Ext.isEmpty(v, false)) {
			return true
		}
		
		var v1 = v.toLowerCase();
		
		if (/^truncate\s/.test(v1)) {
			return true;
		}
		
		return v1.indexOf(' where ') > -1;
	},
	
	'jsonType': function(v) {
		try {
			if (typeof JSON.parse(v) == 'object') {
				return true
			};
		} catch (e) {
			console.log(e);
		}
	},
	
	'whereClauseText': 'SQL语句中必须包含where条件',
	'jsonTypeText': '自定义参数必须为json格式'
});

var DATA_TYPE_TO_JOB_TYPE = {
	'00': 2030,	// mysql-mysql
	'01': 2031,	// mysql-sqlserver
	'02': 2032,	// mysql-oracle
	'03': 2033,	// mysql-ftp
	'04': 2034,	// mysql-hdfs
	'05': 2035,	// mysql-file
	'06': 2036,	// mysql-csv
	'07': 2037,	// mysql-gp
	'08': 2038,	// mysql-hbase
	'09': 2039,    //mysql-mongodb
	'0a': 2040,    //mysql-sundb

	
	
	'10': 2130,	// sqlserver-mysql
	'11': 2131,	// sqlserver-sqlserver
	'12': 2132,	// sqlserver-oracle
	'13': 2133,	// sqlserver-ftp
	'14': 2134,	// sqlserver-hdfs
	'15': 2135,	// sqlserver-file
	'16': 2136,	// sqlserver-csv
	'17': 2137,	// sqlserver-gp
	'18': 2138,	// sqlserver-hbase
	'19': 2139,  	// sqlserver-mongodb
	'1a': 2140,    //sqlserver-sundb
	
	'20': 2230,	// oracle-mysql
	'21': 2231,	// oracle-sqlserver
	'22': 2232,	// oracle-oracle
	'23': 2233,	// oracle-ftp
	'24': 2234,	// oracle-hdfs
	'25': 2235,	// oracle-file
	'26': 2236, 	// oracle-csv
	'27': 2237,	// oracle-gp
	'28': 2238,	// oracle-hbase
	'29': 2239, 	// oracle-mongodb
	'2a': 2240,    //oracle-sundb
	
	
	'30': 2330,	// ftp-mysql
	'31': 2331,	// ftp-sqlserver
	'32': 2332,	// ftp-oracle
	'33': 2333,	// ftp-ftp
	'34': 2334,	// ftp-hdfs
	'35': 2335,	// ftp-file
	'36': 2336, 	// ftp-csv
	'37': 2337,	// ftp-gp
	'38': 2338,	// ftp-hbase
	'39': 2339,  //ftp-mongodb
	'3a': 2340,    //ftp-sundb
	
	'40': 2430,	// hdfs-mysql
	'41': 2431,	// hdfs-sqlserver
	'42': 2432,	// hdfs-oracle
	'43': 2433,	// hdfs-ftp
	'44': 2434,	// hdfs-hdfs
	'45': 2435,	// hdfs-file
	'46': 2436, 	// hdfs-csv
	'47': 2437,	// hdfs-gp
	'48': 2438,	// hdfs-hbase
	'49': 2439,	//hdfs-mongodb
	'4a': 2440,    //hdfs-sundb
	
	'50': 2530, 	// file-mysql
	'51': 2531,	// file-sqlserver
	'52': 2532,	// file-oracle
	'53': 2533,	// file-ftp
	'54': 2534,	// file-hdfs
	'55': 2535,	// file-file
	'56': 2536,	// file-csv
	'57': 2537,	// file-gp
	'58': 2538,	// file-hbase
	'59': 2539,    //file-mongodb
	'5a': 2540,    //file-sundb
	
	'60': 2630,	// csv-mysql
	'61': 2631,	// csv-sqlserver
	'62': 2632,	// csv-oracle
	'63': 2633,	// csv-ftp
	'64': 2634,	// csv-hdfs
	'65': 2635,	// csv-file
	'66': 2636,	// csv-csv
	'67': 2637,	// csv-gp
	'68': 2638,	// csv-hbase
	'69': 2639,   //csv-mongodb
	'6a': 2640,    //csv-sundb
	
	'70': 2730,	// gp-mysql
	'71': 2731,	// gp-sqlserver
	'72': 2732,	// gp-oracle
	'73': 2733,	// gp-ftp
	'74': 2734,	// gp-hdfs
	'75': 2735,	// gp-file
	'76': 2736,	// gp-csv
	'77': 2737,	// gp-gp
	'78': 2738,	// gp-hbase
	'79': 2739,   //gp-mongodb
	'7a': 2740,    //gp-sundb
	
	'80': 2830,	// hase-mysql
	'81': 2831,	// hase-sqlserver
	'82': 2832,	// hase-oracle
	'83': 2833,	// hase-ftp
	'84': 2834,	// hase-hdfs
	'85': 2835,	// hase-file
	'86': 2836,	// hase-csv
	'87': 2837,	// hase-gp
	'88': 2838,	// hase-hbase
	'89': 2839,	//hbase-mongodb
	'8a': 2840,    //hbase-sundb
	
	//add by zhoushasha
	'90': 2930,  //mongodb-mysql
	'91': 2931,  //mongodb-sqlserver
	'92': 2932,  //mongodb-oracle
	'93': 2933,  //mongodb-ftp
	'94': 2934,  //mongodb-hdfs
	'95': 2935,  //mongodb-file
	'96': 2936,  //mongodb-csv
	'97': 2937,  //mongodb-gp
	'98': 2938,  //mongodb-hbase
	'99': 2939,   //mongodb-mongodb
	'9a': 2940,    //mongodb-sundb
	
	'a0': 3030,  //sundb-mysql
	'a1': 3031,  //sundb-sqlserver
	'a2': 3032,  //sundb-oracle
	'a3': 3033,  //sundb-ftp
	'a4': 3034,  //sundb-hdfs
	'a5': 3035,  //sundb-file
	'a6': 3036,  //sundb-csv
	'a7': 3037,  //sundb-gp
	'a8': 3038,  //sundb-hbase
	'a9': 3039,   //sundb-mongodb
	'aa': 3040   //sundb-sundb
	
	
};

var DATA_TYPE_DEFAULT_CHARSET = {
	'0': 'UTF-8',
	'1': 'UTF-8',
	'2': 'UTF-8',
	'3': 'UTF-8',
	'4': 'UTF-8',
	'5': 'GBK',
	'6': 'UTF-8',
	'7': 'UTF-8'
};