_package('com.sw.bi.scheduler.job');

_import([
	'com.sw.bi.scheduler.job.JobMaintainModule'
]);

com.sw.bi.scheduler.job.DataXMaintainModule = Ext.extend(com.sw.bi.scheduler.job.JobMaintainModule, {
	minHeight: 1250,

	jobDatasyncConfig: {},
	
	master: function() {
		var master = com.sw.bi.scheduler.job.DataXMaintainModule.superclass.master.call(this),
			items = master.items;
			
		// 执行网关机
		items[3].store = S.create('gatewaysByJobType', 31);
			
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
					fieldLabel: '不使用自定义XML配置文件',
					store: S.create('yesNo'),
					anchor: '20%',
					value: true,
					allowBlank: false,
					listeners: {
						select: this.onUseCustomXmlSelect,
						scope: this
					}
				}, {
					xtype: 'textfield',
					hidden: true,
					name: 'userXml',
					fieldLabel: 'XML配置文件'
				}]
			}, {
				id: 'errorConfig',
				xtype: 'fieldset',
				title: '容错/线程配置',
				labelWidth: 70,
				
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
							xtype: 'combo',
							hiddenName: 'sourceFileType',
							fieldLabel: '来源数据格式',
							store: S.create('hdfsFileType'),
							allowBlank: false,
							anchor: '40%',
							listeners: {
								select: this.onFileTypeSelect,
								scope: this
							}
						}, {
							xtype: 'combo',
							hiddenName: 'sourceCodec',
							fieldLabel: '数据压缩格式',
							store: S.create('codecFormat'),
							anchor: '40%',
							value: 'org.apache.hadoop.io.compress.GzipCodec'
						}, {
							xtype: 'numberfield',
							hidden: true,
							name: 'sourceCommitThreshold',
							fieldLabel: '每次commit数量',
							anchor: '50%',
							value: 2000,
							minValue: 1,
							maxValue: 10000
						}, {
							xtype: 'textarea',
							name: 'sourceDatapath',
							fieldLabel: 'HDFS文件目录',
							allowBlank: false
						}, {
							xtype: 'textfield',
							hidden: true,
							name: 'sourceDelimiter',
							fieldLabel: '字段分隔符'
						}, {
							xtype: 'numberfield',
							hidden: true,
							name: 'sourceColumns',
							fieldLabel: '字段数'
						}
						
						//add by huzhongji 2015年9月11日09:37:04
						,{
							xtype: 'textfield',
							hidden: true,
							name: 'sourceTableName',
							fieldLabel: 'HBase表名'
						},{
							xtype: 'textarea',
							hidden: true,
							name: 'sourceColumnFields',
							fieldLabel: 'HBase列族及列'
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
							hiddenName: 'targetCharset',
							fieldLabel: '目标数据字符集',
							store: S.create('charset'),
							allowBlank: false,
							anchor: '40%'
						}, {
							xtype: 'combo',
							hiddenName: 'targetFileType',
							fieldLabel: '目标数据格式',
							store: S.create('hdfsFileType'),
							allowBlank: false,
							value: 'rcfile',
							anchor: '40%',
							listeners: {
								select: this.onFileTypeSelect,
								scope: this
							}
						}, {
							xtype: 'combo',
							hiddenName: 'targetCodec',
							fieldLabel: '数据压缩格式',
							store: S.create('codecFormat'),
							value: true,
							anchor: '40%'
						}, {
							xtype: 'numberfield',
							name: 'targetCommitThreshold',
							fieldLabel: '每次commit数量',
							anchor: '50%',
							value: 2000,
							minValue: 1,
							maxValue: 10000
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
							fieldLabel: '字段分隔符'
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
						}
						
						//add by huzhongji 2015年9月11日09:37:38
						,{
							xtype: 'textfield',
							hidden: true,
							name: 'targetTableName',
							fieldLabel: 'HBase表名'
						},{
							xtype: 'textarea',
							hidden: true,
							name: 'targetColumnFields',
							fieldLabel: 'HBase列族及列'
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
			
			jobType = DATA_TYPE_TO_JOB_TYPE[sourceDataType + '' + targetDataType],
			fldGateway = mdl.findField('gateway');
			
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
					
				cmbSourceDataType.setValue(parseInt(dataType.charAt(0), 10));
				cmbTargetDataType.setValue(parseInt(dataType.charAt(1), 10));
				
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
			if (config.jobType == 101) {
            	mdl.masterPnl.form.setValues(config);
            }
			delete config.jobId;
			delete config.jobType;
			
			if (!Ext.isEmpty(config.userXml, false)) {
				var cmbNotUseXml = mdl.findField('notUseXml');
				cmbNotUseXml.setValue(false);
				mdl.onUseCustomXmlSelect(cmbNotUseXml);
				
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
		}
		this.jobDatasyncConfig = config || {};
		
		com.sw.bi.scheduler.job.DataXMaintainModule.superclass.onLoadDataComplete.apply(this, arguments);
		
		if (!Ext.isEmpty(config)) {
			if (!Ext.isEmpty(config.sourceFileType, false)) {
				mdl.onFileTypeSelect(mdl.findField('sourceFileType'));
				// mdl.findField('sourceCodec').setValue(Ext.isEmpty(config.sourceCodec, false) ? false : true);
			}
			
			if (!Ext.isEmpty(config.targetFileType, false)) {
				mdl.onFileTypeSelect(mdl.findField('targetFileType'));
				// mdl.findField('targetCodec').setValue(Ext.isEmpty(config.targetCodec, false) ? false : true);
			}
		}
		
		// 作业查看时“来源数据源”字段宽度会变的很窄，暂时只能通过调整窗口大小来解决这个问题
		if (action == 'viewOnly') {
			mdl.moduleWindow.setWidth(mdl.moduleWindow.getWidth() - 5);
		}
	},
	
	onBeforeSave: function(mdl, data) {
		var job = data['job'],
		
			notUseXml = mdl.findField('notUseXml').getValue(),
			initAction = mdl.findField('initAction').getValue(),
			finalyAction = mdl.findField('finalyAction').getValue(),
			
			finalySuccessSql = job.finalySuccessSql,
			finalyFailSql = job.finalyFailSql,
			
			plg = mdl.getModuleValidatePlugin(),
			
			fields = [
				'errorthreshold',
			
				'datasourceBySourceDatasourceId.datasourceId',
				'sourceCommitThreshold',
				'sourceDatapath',
				'sourceDelimiter',
				'sourceColumns',
				
				'datasourceByTargetDatasourceId.datasourceId',
				'targetCommitThreshold',
				'targetDatapath',
				'targetDelimiter',
				'targetColumns'
			];
			
		if (notUseXml && finalyAction && Ext.isEmpty(finalySuccessSql, false) && Ext.isEmpty(finalyFailSql, false)) {
			plg.addError({
				msg: '和 <span style="color:#15428B;font-weight:bold;">"失败后执行SQL"</span> 必须填写一个',
				field: mdl.findField('finalySuccessSql')
			});
			plg.showErrors();
			
			return false;
		}
		
		// 当目标数据类型为HDFS时需要校验“关联表名”和“关联分区名”是否同时填写
		if (job.targetDataType == 4) {
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
		}

		if (notUseXml === true) {
			// 不使用自定义XML配置文件
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
			// 使用自定义XML配置文件
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

		if (/*job.sourceDataType == 4 || */job.sourceDataType == 5) {
			job.sourceDatapath = mdl.appendSeparator(job.sourceDatapath);
		}
		
		if (/*job.targetDataType == 4 || */job.targetDataType == 5) {
			job.targetDatapath = mdl.appendSeparator(job.targetDatapath);
		}
		
		/*job.sourceCodec = job.sourceCodec === 'true' ? 'org.apache.hadoop.io.compress.GzipCodec' : '';
		job.targetCodec = job.targetCodec === 'true' ? 'org.apache.hadoop.io.compress.GzipCodec' : '';*/
		
		return com.sw.bi.scheduler.job.DataXMaintainModule.superclass.onBeforeSave.call(this, mdl, data);
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
				
				'datasourceByInitDatasourceId.datasourceId', 
				'initSql',
				
				'datasourceByFinalyDatasourceId.datasourceId',
				
				'datasourceBySourceDatasourceId.datasourceId',
				'sourceCommitThreshold',
				'sourceDatapath',
				'sourceDelimiter',
				'sourceColumns',
				
				'datasourceByTargetDatasourceId.datasourceId',
				'targetCommitThreshold',
				'targetDatapath',
				'targetDelimiter',
				'targetColumns'
			];

		if (value === true) {
			// 不使用自定义XML配置
			mdl.syncJobTypeByDataType();
			
			cmbUserXml.hide();
			fsError.show();
			fsInit.show();
			fsFinaly.show();
			pnlDatasource.show();
			
		} else {
			// 使用自定义XML配置
			cmbJobType.setValue(101);
			
			cmbUserXml.show();
			fsError.hide();
			fsInit.hide();
			fsFinaly.hide();
			pnlDatasource.hide();
		}
		
		Ext.iterate(configFields, function(name) {			
			var field = mdl.findField(name);

			if (field.isVisible()) {
				field.allowBlank = value !== true ? true : !value;
				console.log(field);
			}
		});
		
		cmbUserXml.allowBlank = value;
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
		
			fields = isSource ? {
				datasource: mdl.findField('datasourceBySourceDatasourceId.datasourceId'),
				commitThreshold: mdl.findField('sourceCommitThreshold'),
				datapath: mdl.findField('sourceDatapath'),
				delimiter: mdl.findField('sourceDelimiter'),
				columns: mdl.findField('sourceColumns'),
				fileType: mdl.findField('sourceFileType'),
				codec: mdl.findField('sourceCodec'),
				
				//add by zhuzhogji 2015年9月11日09:38:31
				tablename: mdl.findField('sourceTableName'),
				columnfields: mdl.findField('sourceColumnFields')
			} : {
				datasource: mdl.findField('datasourceByTargetDatasourceId.datasourceId'),
				commitThreshold: mdl.findField('targetCommitThreshold'),
				datapath: mdl.findField('targetDatapath'),
				delimiter: mdl.findField('targetDelimiter'),
				columns: mdl.findField('targetColumns'),
				fileType: mdl.findField('targetFileType'),
				codec: mdl.findField('targetCodec'),
				escape: mdl.findField('targetEscape'),
				referDbName: mdl.findField('referDbName'),
				referTableName: mdl.findField('referTableName'),
				referPartName: mdl.findField('referPartName'),
				
				//add by zhuzhongji 2015年9月11日09:38:59
				tablename: mdl.findField('targetTableName'),
				columnfields: mdl.findField('targetColumnFields')
			};
			
		mdl.syncJobTypeByDataType();
		
		/////////////////////////////////////////////////////////////

		if (isSource) {
			// 如果以后数据类型下拉框允许修改的话则需要使用下面注释的那行
			// mdl.findField('sourceDatapath').setValue(Ext.isEmpty(config['sourceDatapath']) ? null : config['sourceDatapath']);
			mdl.findField('sourceDatapath').setValue(null);
			mdl.findField('sourceCharset').setValue(DATA_TYPE_DEFAULT_CHARSET[combo.getValue()]);
			mdl.findField('sourceFileType').setValue(null);
			mdl.findField('sourceCodec').setValue(null);
			
		} else {
			// mdl.findField('targetDatapath').setValue(Ext.isEmpty(config['targetDatapath']) ? null : config['targetDatapath']);
			mdl.findField('targetDatapath').setValue(null);
			mdl.findField('targetCharset').setValue(DATA_TYPE_DEFAULT_CHARSET[combo.getValue()]);
			mdl.findField('targetFileType').setValue(null);
			mdl.findField('targetCodec').setValue(null);
		}

		/////////////////////////////////////////////////////////////
		
		Ext.iterate(fields, function(name) {
			var fld = fields[name];
			
			fld.hide();
			fld.allowBlank = true;
		});
		//modify by zhoushasha 2016年5月11日09:39:47
		switch (combo.getValue()) {
			case 0:
			case 1:
			case 2:
			case 7:
			case 9:
			//add by zhuzhongji 2015年9月11日09:39:47
			case 8:
			
				var datasource = fields.datasource,
					commitThreshold = fields.commitThreshold,
					datapath = fields.datapath,
					columns = fields.columns,
					escape = fields.escape;
					
					//add by zhuzhongji 2015年9月11日09:40:21
					tablename = fields.tablename;
					columnfields = fields.columnfields;
			
				datasource.show();
				commitThreshold.show();
				//datapath.show();
				
				datasource.store.load({params: {condition: {
					'userGroupId': USER_GROUP_ID,
					'type-eq': combo.getValue(),
					'active-eq': true
				}}});
				datasource.setValue(null);
				
				//datasource.allowBlank = false;
				commitThreshold.allowBlank = false;
				//datapath.allowBlank = false;
				
				commitThreshold.setValue(2000);
				if (isSource) {
					// modify by zhuzhongji 2015年9月11日09:42:05
					//datapath.vtype = 'whereClause';
					//datapath.setFieldLabel('数据查询SQL');
					datapath.show();
					if(combo.getValue() == 8){
						tablename.show();
						columnfields.show();
						datapath.setFieldLabel('rowkey切分规则');
						datapath.setValue('full_table_scan');
					}else if(combo.getValue() == 9){
						//add by zhoushasha
						tablename.show();
						tablename.setFieldLabel('db和集合(如：db.collection)');
						//modify by zhoushasha 2016/6/3
						tablename.allowBlank=false;
						columnfields.allowBlank=false;
						
						columnfields.show();
						columnfields.setFieldLabel("mongodb的列")
						datapath.allowBlank=true;
						datapath.hide();
					}else{
						datapath.vtype = 'whereClause';
						datapath.setFieldLabel('数据查询SQL');
					}
				} else {
					if (combo.getValue() == 7) {
						escape.show();
						columns.show();
						datapath.show();
						datapath.setFieldLabel('表及字段');
						
						
					} 
					else if(combo.getValue() == 9){
						//add by zhoushasha 2016/05/10 14:23:00
						tablename.show();
						tablename.setFieldLabel('db和集合(如：db.collection)')
						//modify by zhoushasha 2016/6/3
						tablename.allowBlank=false;
						columnfields.allowBlank=false;
						
						columnfields.show();
						columnfields.setFieldLabel("mongodb的列")
						datapath.allowBlank=true;
						datapath.hide()
					}
					//add by zhuzhongji 2015年9月11日09:42:35
					else if (combo.getValue() == 8){
						tablename.show();
						columnfields.show();
						//datapath.setFieldLabel('数据查询路径');
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
					columns = fields.columns,
					delimiter = fields.delimiter,
					fileType = fields.fileType,
					codec = fields.codec;
				
				datasource.show();
				datapath.show();
				datapath.allowBlank = false;
				
				fileType.show();
				fileType.allowBlank = false;
				
				codec.show();
				delimiter.show();
				columns.show();
				
				datasource.store.load({params: {condition: {
					'userGroupId': USER_GROUP_ID,
					'type-eq': combo.getValue(),
					'active-eq': true
				}}});
				datasource.setValue(null);
				
				datasource.allowBlank = false;
				
				datapath.setFieldLabel('FTP文件目录');
				datapath.vtype = null;
			break;
			
			case 4:
				var datapath = fields.datapath,
					columns = fields.columns,
					delimiter = fields.delimiter,
					fileType = fields.fileType,
					codec = fields.codec,
					referDbName = fields.referDbName,
					referTableName = fields.referTableName,
					referPartName = fields.referPartName;
				
				datapath.show();
				datapath.allowBlank = false;
				
				fileType.show();
				fileType.allowBlank = false;
				
				codec.show();
				
				if (!isSource) {
					columns.show();
					referDbName.show();
					referTableName.show();
					referPartName.show();
					
					columns.allowBlank = false;
				}
				
				datapath.setFieldLabel('HDFS文件目录');
				datapath.vtype = null;
			break;
			case 5:
			case 6:
				var delimiter = fields.delimiter,
					columns = fields.columns,
					datapath = fields.datapath;
					
				datapath.show();
				delimiter.show();
				columns.show();
				
				delimiter.allowBlank = false;
				columns.allowBlank = false;
				datapath.allowBlank = false;
				
				datapath.setFieldLabel('文件所在目录');
				datapath.vtype = null;
			break;
		}
	},
	
	onFileTypeSelect: function(combo) {
		var isSource = combo.getName() == 'sourceFileType',
			isRCFile = combo.getValue() == 'rcfile';
		
		if (isSource) {
			if (isRCFile) {
				this.findField('sourceCodec').setValue('org.apache.hadoop.io.compress.GzipCodec');
			}
			this.findField('sourceCodec').setDisabled(isRCFile);
			this.findField('sourceDelimiter').setVisible(!isRCFile);
			this.findField('sourceDelimiter').allowBlank = isRCFile;
		} else {
			if (isRCFile) {
				this.findField('targetCodec').setValue('org.apache.hadoop.io.compress.GzipCodec');
			}
			this.findField('targetCodec').setDisabled(isRCFile);
			this.findField('targetDelimiter').setVisible(!isRCFile);
			this.findField('targetDelimiter').allowBlank = isRCFile;
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
	
	'whereClauseText': 'SQL语句中必须包含where条件'
});

var DATA_TYPE_TO_JOB_TYPE = {
	'00': 61,	// mysql-mysql
	'01': 62,	// mysql-sqlserver
	'02': 60,	// mysql-oracle
	'04': 2,	// mysql-hdfs
	'05': 63,	// mysql-file
	'06': 64,	// mysql-csv
	'07': 65,	// mysql-gp
	'03': 66,	// mysql-ftp
	'08': 67,	// mysql-hbase
	'09':68,    //mysql-mongodb
	
	'10': 71,	// sqlserver-mysql
	'11': 72,	// sqlserver-sqlserver
	'12': 70,	// sqlserver-oracle
	'14': 3,	// sqlserver-hdfs
	'15': 73,	// sqlserver-file
	'16': 74,	// sqlserver-csv
	'17': 75,	// sqlserver-gp
	'13': 76,	// sqlserver-ftp
	'18': 77,	// sqlserver-hbase
	'19':78,  	// sqlserver-mongodb
	
	'20': 81,	// oracle-mysql
	'21': 82,	// oracle-sqlserver
	'22': 80,	// oracle-oracle
	'24': 1,	// oracle-hdfs
	'25': 83,	// oracle-file
	'26': 84, 	// oracle-csv
	'27': 85,	// oracle-gp
	'23': 86,	// oracle-ftp
	'28': 87,	// oracle-hbase
	'29':88, 	// oracle-mongodb
	
	'30': 131,	// ftp-mysql
	'31': 132,	// ftp-sqlserver
	'32': 130,	// ftp-oracle
	'34': 138,	// ftp-hdfs
	'35': 133,	// ftp-file
	'36': 134, 	// ftp-csv
	'37': 135,	// ftp-gp
	'33': 136,	// ftp-ftp
	'38': 137,	// ftp-hbase
	'39': 139,  //ftp-mongodb
	
	'40': 31,	// hdfs-mysql
	'41': 32,	// hdfs-sqlserver
	'42': 30,	// hdfs-oracle
	'44': 34,	// hdfs-hdfs
	'45': 33,	// hdfs-file
	'46': 35, 	// hdfs-csv
	'47': 36,	// hdfs-gp
	'43': 37,	// hdfs-ftp
	'48': 38,	// hdfs-hbase
	'49':39,	//hdfs-mongodb
	
	'50': 51, 	// file-mysql
	'51': 52,	// file-sqlserver
	'52': 50,	// file-oracle
	'54': 4,	// file-hdfs
	'55': 53,	// file-file
	'56': 54,	// file-csv
	'57': 55,	// file-gp
	'53': 56,	// file-ftp
	'58': 57,	// file-hbase
	'59':58,    //file-mongodb
	
	'60': 121,	// csv-mysql
	'61': 122,	// csv-sqlserver
	'62': 120,	// csv-oracle
	'64': 126,	// csv-hdfs
	'65': 123,	// csv-file
	'66': 124,	// csv-csv
	'67': 125,	// csv-gp
	'63': 127,	// csv-ftp
	'68': 128,	// csv-hbase
	'69':129,   //csv-mongodb
	
	'70': 111,	// gp-mysql
	'71': 112,	// gp-sqlserver
	'72': 110,	// gp-oracle
	'74': 116,	// gp-hdfs
	'75': 113,	// gp-file
	'76': 114,	// gp-csv
	'77': 115,	// gp-gp
	'73': 117,	// gp-ftp
	'78': 118,	// gp-hbase
	'79':119,   //gp-mongodb
	
	'80': 141,	// hase-mysql
	'81': 142,	// hase-sqlserver
	'82': 140,	// hase-oracle
	'84': 146,	// hase-hdfs
	'85': 143,	// hase-file
	'86': 144,	// hase-csv
	'87': 145,	// hase-gp
	'83': 147,	// hase-ftp
	'88': 148,	// hase-hbase
	'89':149,	//hbase-mongodb
	
	//add by zhoushasha
	'90':151,  //mongodb-mysql
	'91':152,  //mongodb-sqlserver
	'92':150,  //mongodb-oracle
	'94':156,  //mongodb-hdfs
	'95':153,  //mongodb-file
	'96':154,  //mongodb-csv
	'97':155,  //mongodb-gp
	'93':157,  //mongodb-ftp
	'98':158,  //mongodb-hbase
	'99':159   //mongodb-mongodb
	
};

var DATA_TYPE_DEFAULT_CHARSET = {
	'0': 'utf-8',
	'1': 'utf-8',
	'2': 'utf-8',
	'4': 'utf-8',
	'5': 'gbk',
	'6': 'utf-8',
	'7': 'utf-8'
};