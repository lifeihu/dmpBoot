(function() {
	
	if (typeof top.window.storeDataCache == 'undefined')
		top.window.storeDataCache = new Ext.util.MixedCollection();
	
	var 
		// 数据默认缓存时间(分钟)
		CACHE_TIMEOUT = 60,
	
		// 用于缓存Store数据
		storeDataCache = top.window.storeDataCache,
		
		/**
		 * Store数据缓存类
		 * @param String storeDataKey
		 * @param Ext.data.Store store
		 */
		StoreData = function(storeDataKey, store) {
			this.id = storeDataKey;
			
			// 缓存时间(false: 永不过期, Number: 缓存多少分钟)
			var timeout = store.cacheTimeout;
			
			// 未指明缓存时间则默认分配
			if (Ext.isEmpty(timeout, false) || timeout === true)
				timeout = CACHE_TIMEOUT;
				
			// 将缓存时间转换为毫秒
			if (Ext.isNumber(timeout))
				timeout = timeout * 60 * 1000;
				
			// 缓存截止时间(false: 永不过期, Number: 具体的过期时间)
			var expiration = timeout === false ? false : new Date().getTime() + timeout;
			
			var data = store.data.clone().items,
				totalLength = store.totalLength;
			
			/**
			 * 克隆缓存数据至指定Store
			 * @param Ext.data.Store
			 */
			this.clone = function(store) {
				store.fromCache = true;
				
				store.removeAll();
				store.totalLength = totalLength;
				store.add(data);
				
				framework.debug('storedata(' + this.id + ') from cache.');
			};
			
			/**
			 * 数据缓存是否过期
			 */
			this.overdue = function() {
				if (expiration === false) return false;
				
				var overdue = new Date().getTime() > expiration;
				
				if (overdue === true) {
					// 如果已经过期则删除缓存数据
					removeStoreData(this.id);
				}
				
				return overdue;
			};
		},
		
		/**
		 * 添加Store数据缓存
		 * @param String storeId
		 * @param StoreData storeData
		 */
		addStoreData = function(storeType, storeData) {
			var storeDatas = storeDataCache.get(storeType);
			
			if (Ext.isEmpty(storeDatas)) {
				storeDatas = new Ext.util.MixedCollection();
			}
			storeDatas.add(storeData);
			
			framework.debug(storeType + ' - storedata(' + storeData.id + ') add to cache.');
			
			storeDataCache.add(storeType, storeDatas);
		},
		
		/**
		 * 获得Store数据缓存
		 * @param String storeDataKey
		 */
		getStoreData = function(storeDataKey) {
			var storeType = storeDataKey.substring(0, storeDataKey.indexOf('-')),
				storeDatas = storeDataCache.get(storeType);

			return Ext.isEmpty(storeDatas) ? null : storeDatas.get(storeDataKey);
		},
		
		/**
		 * 删除Store数据缓存
		 * @param String storeDataKey
		 */
		removeStoreData = function(storeDataKey) {
			var storeType = storeDataKey.substring(0, storeDataKey.indexOf('-')),
				storeDatas = storeDataCache.get(storeType);
				
			if (!Ext.isEmpty(storeDatas)) {
				storeDatas.removeKey(storeDataKey);
				framework.debug('storedata(' + storeDataKey + ') remove from cache.');
			}
		},
		
		/**
		 * 清空Store数据缓存
		 * @param String storeType
		 */
		clearStoreData = function(storeType) {
			if (Ext.isEmpty(storeType)) {
				storeDataCache.clear();
				storeDataCache = null;
				
				top.window.storeDataCache = null;
				
			} else {
				if (storeDataCache.containsKey(storeType)) {
					storeDataCache.removeKey(storeType);
					framework.debug(storeType + ' - store data is removed.');
				}
			}
		},
		
		/**
		 * 当Store注销时清空相应的缓存数据
		 */
		beforeDestroy = function() {
			clearStoreData(this.storeType);
		};
	
	S = {};
	Ext.apply(S, {
		/**
		 * 根据stype属性创建Store
		 * @param String/Object config String类型时表示stype, Object类型时表示Store的配置参数
		 * @param params... 参数列表,这些参数在创建时会与配置信息中的paramNames属性一起被默认添加至baseParams属性中
		 */
		create: function(config) {
			if (Ext.isEmpty(config, false)) return null;
			
			if (Ext.isString(config)) {
				// 如果配置参数为字符时则认为是storeId,并通过Ext.StoreMgr从缓存中去获取对应的Store(一般此种情况下获取的都为SimpleStore)
				var store = Ext.StoreMgr.lookup(config);
				if (!Ext.isEmpty(store)) return store;
				
				config = {stype: config};
			}
			
			var me = this,
			
				stype = config.stype,
				
				// 获得指定stype的Store默认配置
				defaultConfing = Ext.apply({}, S.CONFIG[stype]),
				
				fields = config.fields || [],
				defaultFields = defaultConfing.fields || [],
				
				baseParams = config.baseParams || {},
				defaultBaseParams = defaultConfing.baseParams || {},
				
				condition = baseParams.condition,
				defaultCondition = defaultBaseParams.condition || {};
				
			// 合并condition参数
			condition = Ext.apply(defaultCondition, condition);
			
			// 合并baseParams参数
			baseParams = Ext.apply(defaultBaseParams, baseParams);
			baseParams.condition = condition;
				
			// 合并配置信息
			config = Ext.apply(defaultConfing, config);
			config.baseParams = baseParams;
			
			// 合并需要显示的列
			config.fields = defaultFields.concat(fields);

			/////////////////////////////////////////////////////////////
			
			var paramNames = config.paramNames,
				conditionParamNames = config.conditionParamNames,
			
				listeners = config.listeners,
				xtype = !Ext.isEmpty(config.data) && config.data.length > 0 ? 'simplestore' : config.xtype || 'jsonstore';
			
			if (xtype == 'simplestore') {
				config.storeId = config.stype = stype;
				
				// 不用自动销毁
				config.autoDestroy = false;
				
				if (Ext.isEmpty(config.fields) || config.fields.length) {
					config.fields = ['value', 'name'];
				}
					
			} else {
				// 自动销毁
				config.autoDestroy = true;
				
				if (!Ext.isEmpty(stype, false)) {
					config.proxy = new Ext.data.HttpProxy({
						url: config.url,
						
						listeners: {
							beforeload: me.onStoreBeforeLoad,
							scope: me
						}
					});
					
					config.listeners = {
						load: me.onStoreLoad,
						scope: me
					};
				}
			}
			
			var args = Ext.toArray(arguments, 1);
			if (Ext.isArray(paramNames) && paramNames.length > 0 && args.length > 0) {
				var paramName, paramValue, params = {};
				for (var i = 0, len = args.length; i < len; i++) {
					paramName = paramNames[i];
					paramValue = args[i];

					params[paramName] = paramValue;
				}
				
				Ext.apply(config.baseParams, params);
			}
			
			if (Ext.isArray(conditionParamNames) && conditionParamNames.length > 0 && args.length > 0) {
				var paramName, paramValue, params = {};
				for (var i = 0, len = args.length; i < len; i++) {
					paramName = conditionParamNames[i];
					paramValue = args[i];

					params[paramName] = paramValue;
				}
				
				Ext.apply(config.baseParams.condition, params);
			}

			if (!Ext.isDefined(config.autoLoad)) {
				config.autoLoad = true;
			}
			
			var store = Ext.create(config, xtype);
			
			// TODO 用于调试
			if (Ext.isEmpty(store.stype, false)) {
				framework.debug('create store(' + store.proxy.url + '), but not support cache.');
			} else if (store instanceof Ext.data.JsonStore) {
				framework.debug('create store(' + store.stype + ', ' + store.proxy.url + ') and support cache.');
			}
			
			if (!Ext.isEmpty(store.proxy)) {
				store.proxy.store = store;
			}
			
			// store.destroy = store.destroy.createInterceptor(beforeDestroy, store);
			
			if (!Ext.isEmpty(listeners)) {
				store.on(listeners);
			}
			
			/*if (autoLoad !== false) {
				store.load.defer(10, store, [typeof autoLoad == 'object' ? autoLoad : undefined]);
			}*/
			
			return store;
		},
		
		/**
		 * 将Store加载数据时的参数(params、baseParams)和参数值排序后再加上Store中的fields作为缓存数据的key
		 * @param {Ext.data.Store} store
		 * @param {Object} params
		 */
		keyString: function(store, params) {
			var storeType = store.stype,
			
				p = Ext.apply({}, params),
				condition = p.condition;
				
			if (Ext.isString(condition)) {
				condition = Ext.decode(condition);
			}
			delete p.condition;
			
			Ext.apply(p, condition);
			
			////////////////////////////////////
			
			// 将所有参数属性进行排序后加上对应的值组织成Key
			var props = [];
			Ext.iterate(p, function(prop) {
				if (prop != 'refresh')
					props.push(prop);
			});
			props = props.sort();
			
			var keyString = [storeType];
			Ext.iterate(props, function(prop) {
				keyString.push(prop, p[prop]);
			});
			
			// 将组织好的Key再加上Store中的fields重新组织新的Key
			var fields = store.reader.recordType.prototype.fields,
				flds = [];
			fields.each(function(fld) {
				flds.push(fld.name);
			});
			
			return keyString.concat(flds.sort()).join('-');
		},
		
		clearStoreData: clearStoreData,
		
		///////////////////////////////////////////////////////////
		
		onStoreBeforeLoad: function(proxy, params) {
			var store = proxy.store,
				
				storeDataKey = this.keyString(store, params);

			if (params['refresh'] === true) {
				removeStoreData(storeDataKey);
			}
			
			// store.storeDataKey = storeDataKey;
			var storeData = getStoreData(storeDataKey)

			if (!Ext.isEmpty(storeData) && !storeData.overdue()) {
				storeData.clone(store);

				(function() {
					store.fireEvent('load', store, store.getRange(), {params: params});
				}).defer(10);
				
				return false;
			}
		},
		
		onStoreLoad: function(store, records, options) {
			var storeType = store.stype,
				storeDataKey = this.keyString(store, options.params); // store.storeDataKey || this.keyString(store, store.lastOptions.params);

			if (!store.fromCache) {
				addStoreData(storeType, new StoreData(storeDataKey, store));
			}
			delete store.fromCache;
			// delete store.storeDataKey;
		}
	});
})();