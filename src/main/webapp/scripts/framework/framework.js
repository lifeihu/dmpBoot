framework = (function() {
	if (typeof top.window.fileContentMap == 'undefined')
		top.window.fileContentMap = {};
		
	if (typeof top.window.moduleMap == 'undefined')
		top.window.moduleMap = {};
	
	var scripts = document.getElementsByTagName('script'),
		scriptDom = scripts[scripts.length - 1],
	    
		/**
		 * 脚本内容缓存
		 */
		fileContentMap = top.window.fileContentMap,
	
		/**
		 * 缓存不同framework下的Module
		 */
		moduleMap = top.window.moduleMap,
		
		/**
		 * 可根据frameworkId区分不同window
		 */
		frameworkId = 'fmk-' + new Date().getTime(),
		
		/**
		 * 类文件在某个framework中是否解析的映射关系
		 */
		frameworkInstanceClassNameMap = {},
		
		// 框架路径
		//frameworkPath = scriptDom.src.replace('framework.js', '').replace(/\\/g, '/') + '../', 
		//去掉url中的jsessionid
	    frameworkPathIndex = scriptDom.src.indexOf('framework.js'),
	    frameworkPath = scriptDom.src.substring(0, frameworkPathIndex).replace(/\\/g, '/') + '../',
		// 项目路径
		basePath = frameworkPath + '../',
	    
	    /**
	     * 是否允许自动加载Module类(默认: true)
	     * @type Boolean
	     */
	    allowAutoLoad,
	    
	    /**
	     * 是否允许编译脚本(默认: false)
	     */
	    allowPacker,
	    
	    /**
	     * 日志等级(1:DEBUG, 2:INFO, 3:ERROR)
	     */
	    logLevel = 1,
	    
	    /**
	     * 项目所有的基本库
	     */
		libs = {
			packer: ['lib.packer'],
			
			ext: [
				'framework.core.ext.ext-base',
				// 'framework.core.ext.ext-all-debug',
				'framework.core.ext.ext-all'
			], 
		    
		    framework: [
		    	'framework.core.Notice',
				'framework.core.ext.ext-patch',
				
		    	'framework.widgets.clipboard.ZeroClipboard',
		    	'framework.widgets.data.StoreMgr',
		    	// 'com.sw.bi.scheduler.store-lib',
		    	
		    	'framework.core.Module',
		    	'framework.widgets.grid.CustomColumn'
			]
		},
		
		/**
		 * 脚本编译器
		 * @params Packer packer
		 */
		packer,
		
		/**
		 * 需要被渲染至当前页面的Module
		 * @type {framework.core.Module}
		 */
		module,
		
		delegate = function(method) {
			return function() {
				return method.apply(window, arguments);
			};
		},
		
		/**
		 * 将传入的所有参数转换成一个Key标识
		 */
		keyString = function() {
			var args = [];
			for (var i = 0, len = arguments.length; i < len; i++)
				args[i] = arguments[i];
			return args.join('-');
		},
		
		/**
		 * 注册Module
		 * @param module
		 */
		registerModule = function(module) {
			if (Ext.isEmpty(module)) return;
			
			if (Ext.isEmpty(module.moduleId, false))
				module.moduleId = module.id;
			
			var frameworkModuleMap = moduleMap[frameworkId] || {},
				moduleId = Ext.isEmpty(module.moduleId, false) ? module.id : module.moduleId;

			if (!frameworkModuleMap.hasOwnProperty(moduleId)) {
				frameworkModuleMap[moduleId] = module;
				// alert(frameworkId + ' ' + moduleId);
				moduleMap[frameworkId] = frameworkModuleMap;
			}
		},
		
		/**
		 * 获得Module
		 * @param moduleId
		 * @param framework 需要指定在哪个框架中查找Module
		 */
		findModule = function(moduleId, framework) {
			if (Ext.isEmpty(moduleId, false)) return null;

			var frameworkId = !Ext.isEmpty(framework) ? framework.FRAMEWORK_ID : frameworkId,
				frameworkModuleMap = moduleMap[frameworkId] || {};

			return frameworkModuleMap[moduleId];
		},
		
		/**
		 * 删除Module
		 * @param Module/String module
		 */
		removeModule = function(module) {
			if (Ext.isEmpty(module)) return;
			
			var frameworkModuleMap = moduleMap[frameworkId],
				moduleId = Ext.isString(module) ? module : Ext.isEmpty(module.moduleId, false) ? module.id : module.moduleId;
				
			delete frameworkModuleMap[moduleId];
		},
		
		/**
		 * 创建XHR
		 */
		connection = function() {
	    	return typeof XMLHttpRequest !== 'undefined' ? new XMLHttpRequest() : new ActiveXObject('Microsoft.XMLHTTP')
	    },
		
		/**
		 * 初始化脚本标签<script>中定义的选项参数
		 */
		initScriptElementOptions = function() {
	    	var optAutoLoad, optPacker;
	    	
			optAutoLoad = scriptDom.getAttribute('autoload');
			allowAutoLoad = optAutoLoad !== 'false';
			
			optPacker = scriptDom.getAttribute('packer');
			allowPacker = optPacker === 'true';
		},
	    
	    /**
	     * 解析脚本内容
	     */
	    instance = function(content, className, callback, scope) {
			// 确保同一window中同一个类文件只需要解析一次
			if (!isInstaceOnFramework(frameworkId, className)) {
				if (packer != null) {
					var rule = PackerRule.getRule(className);
					
					/*alert(className + 
						'\nregexp: ' + rule.regexp +
						'\npacker: ' + rule.packer +
						'\nbase62: ' + rule.base62 +
						'\nshrink: ' + rule.shrink);*/
					
					if (rule.packer)
						content = packer.pack(content, rule.base62, rule.shrink);
				}
				
				new Function(content + "\n//@ sourceURL=" + className)();//new Function()动态执行代码; @ sourceURL 调试异步加载页面里包含的js文件
				//console.log(Ext)
				var instanceClassNames = frameworkInstanceClassNameMap[frameworkId] || {};
				if (!instanceClassNames.hasOwnProperty(className)) {
					instanceClassNames[className] = true;
					frameworkInstanceClassNameMap[frameworkId] = instanceClassNames;
				}
			}
			
	    	if (callback != null) {
	    		callback.call(scope);
	    	}
	    },
	    
	    /**
	     * 在指定的framework中类文件是否被解析
	     * @param String frameworkId
	     * @param String className
	     */
	    isInstaceOnFramework = function(frameworkId, className) {
	    	var instanceClassNames = frameworkInstanceClassNameMap[frameworkId] || {};
	    	return instanceClassNames[className] === true;
	    },
		
		/**
		 * 获得需要加载的脚本路径
		 * @param String className
		 */
		getScriptPath = function(className) {
			 return frameworkPath + className.replace(/\./g, '/') + '.js';
		},
		
		/**
		 * 清理异步加载时的脚本标签
		 * @param HTMLElement<script>
		 */
		cleanupScriptElement = function(script) {
			script.onload = null;
			script.onerror = null;
			script.onreadystatechange = null;
		},
		
		/**
		 * 同步请求
		 */
		syncRequest = function(config) {
			if (config == null) {
				return null;
			}
			
			if (typeof config == 'string') {
				config = {url: config};
			}
			
			var url = config.url;
			if (url == null || url.length == 0) {
				return;
			}
			var status, result, 
				xhr = connection(),
				
				method = config.method || 'GET',
				root = config.root,
				decode = (root && root.length > 0) || config.decode === true,
				params = config.params;
				// url = framework.getUrl(config.url, config.params);
				
			if (params) {
				var p = [];
				for (var key in params) {
					p.push(key + '=' + params[key]);
				}
				
				var pos = url.indexOf('?'),
					url = url + (pos == -1 ? '?' : pos == url.length - 1 ? '' : '&');
					
				url += p.join('&');
			}

			xhr.open(method, url, false);
			xhr.send(null);
			
			status = (xhr.status === 1223) ? 204 : xhr.status;
			if (status >= 200 && status < 300)
				result = xhr.responseText;

			if (result && result.length > 0 && decode) {
				try {
					result = Ext.decode(result);
				} catch (e) {
					result = null;
				}
				
				if (!Ext.isEmpty(result, false) && !Ext.isEmpty(root, false)) {
					result = result[root];
				}
			}
			
			xhr = null;
			
			return result;
		},
		
		/**
		 * 加载脚本文件
		 * @param Array<String>/String className
		 * @param Function callback
		 * @param Object scope
		 * @param Boolean synchronous
		 */
		loadScriptFile = function(className, callback, scope, synchronous) {
			if (className == null) return;
			if (typeof className == 'object' && className.length != undefined) {
				for (var i = 0, len = className.length; i < len; i++)
					loadScriptFile(className[i], callback, scope, synchronous);
				return;
			}
			
			var filePath = getScriptPath(className);
	
			// 同步加载
			if (synchronous !== false) {
				var content = fileContentMap[className];
				if (typeof content == 'string' && content != null && content.length > 0) {
					instance(content, className, callback, scope);
					
					return;
				}
				
				var content = syncRequest(filePath);
				instance(content, className, callback, scope);
				fileContentMap[className] = content;
			// 异步加载
			} else {
				var head = typeof document !== 'undefined' && (document.head || document.getElementsByTagName('head')[0]),
					script = document.createElement('script'),
					
					onLoad = function() {
						cleanupScriptElement(script);
						callback.call(scope);
					};
				
				script.type = 'text/javascript';
				script.src = filePath;
				script.onload = onLoad;
				script.onreadystatechange = function() {
					if (this.readyState === 'loaded' || this.readyState === 'complete') 
	                    onLoad();
				};
				
				head.appendChild(script);
			}
		},
		
		/**
		 * 输出日志信息
		 * @param String msg
		 * @param String level(debug, info, error)
		 */
		log = function(msg, level) {
			if (Ext.isIE6 || typeof console == 'undefined') return;
			
			var prefix = new Date().format('G:i:s');
			
			if (level == 'debug')
				prefix += ' DEBUG';
				
			else if (level == 'error') {
				prefix += ' ERROR';
				
				if (!Ext.isString(msg)) {
					var e = msg;
					msg = !Ext.isEmpty(e.stack) ? e.stack : e.message;
				}
				
			} else
				prefix += ' INFO';
				
			console.log(prefix + ' - ' + msg);
		},
		
		/**
		 * 自动渲染Module
		 */
		autoLoadModule = function() {
			var runer = new Ext.util.TaskRunner(),
		
				run = function() {
					if (!Ext.isEmpty(module, false)) {
						try {
							var moduleId;
							
							if (typeof module == 'string') {
								moduleId = module;
								module = eval(module);
							}
	
							module = new module({
								renderTo: Ext.get('ext-container') || Ext.getBody()
							});
				
							// 获得Module的ID
							if (!Ext.isEmpty(moduleId, false))
								module.moduleId = moduleId;
							/*if (Ext.isEmpty(mdl.id, false))
								mdl.id = Ext.isEmpty(moduleId) ? Ext.id(mdl, 'pf-module-') : moduleId;*/
							registerModule(module);
							module.render();
							
							// module = null;
							return false;
							
						} catch (e) {
							log(e, 'error');
							return false;
						}
					}
				};
				
			runer.start({
				run: run,
				interval: 1000,
				repeat: 5
			});
		},
		
		destroy = function() {
			if (Ext.isObject(module)) {
				module.destroy();
			}
			
			if (!Ext.isEmpty(moduleMap))
				delete moduleMap[frameworkId];
			
			if (!Ext.isEmpty(frameworkInstanceClassNameMap))
				delete frameworkInstanceClassNameMap[frameworkId];

			if (this == top.window) {
				top.window.fileContentMap = null;
				top.window.moduleMap = null;
				
				moduleMap = null;
				fileContentMap = null;
				frameworkInstanceClassNameMap = null;
				
				S.clearStoreData();
				S =  null;
			}
			
			this.framework = null;
		},
		
		/**
		 * 整个框架的执行方法
		 */
		run = function() {
			
			// 加载项目所需库
			loadScriptFile(libs.framework);
			
			ZeroClipboard.config({
				swfPath: frameworkPath + 'framework/widgets/clipboard/ZeroClipboard.swf'
			});
			
			if (allowAutoLoad === true)
				Ext.onReady(autoLoadModule);
				
			// 窗口关闭的销毁操作
			Ext.lib.Event.on(window, 'unload', destroy);
		};
		
	/**
	 * @method _import
	 * @see loadScriptFile
	 */
	_import = delegate(loadScriptFile);

	initScriptElementOptions();
	
	// 加载Ext核心库
	loadScriptFile(libs.ext);
	
	Ext.QuickTips.init();
	
	/**
	 * @method _package
	 */
	_package = Ext.ns;

	return {
		VERSION: '4.0.20110528',
		
		FRAMEWORK_ID: frameworkId,
		FRAMEWORK_PATH: frameworkPath,
		APPLICATION_PATH: basePath,
		
		/**
		 * 初始化框架
		 */
		initFramework: function() {
			window.framework = framework;
			
			if (allowPacker) {
				loadScriptFile('lib.packer-rule');
				loadScriptFile(libs.packer, function() {
					// 创建脚本编译器
					packer = new Packer();
					
					run();
				}, null, false);
				
			} else {
				run();
			}
		},
		
		/**
		 * 将文件内容加入缓存
		 * @param className
		 * @param content
		 */
		setFileContent: function(fileName, content) {
			if (!fileContentMap.hasOwnProperty(fileName))
				fileContentMap[fileName] = content;
		},
		
		/**
		 * 在缓存中获得文件内容
		 * @param fileName
		 */
		getFileContent: function(fileName) {
			return fileContentMap[fileName];
		},
		
		/**
		 * 将传入的所有参数转为Key标识
		 */
		keyString: keyString,
		
		/**
		 * 同步请求
		 * @param filePath
		 */
		syncRequest: syncRequest,
		
		/**
		 * 获得完整的URL地址
		 * @param String url
		 * @param Object params
		 */
		getUrl: function(url, params) {
			if (Ext.isEmpty(url, false)) return null;

			if (url.indexOf(basePath) == -1) {
				if (url.charAt(0) == '/')
					url = url.substring(1);
					
				url = basePath + url;
			}
			
			if (!Ext.isEmpty(params)) {
				var pos = url.indexOf('?'),
					url = url + (pos == -1 ? '?' : pos == url.length - 1 ? '' : '&');
					
				url += Ext.urlEncode(params);
			}
			
			return url;
		},
		
		getManageUrl: function(url, params) {
			if (Ext.isEmpty(url, false)) return null;

			if (url.charAt(0) == '/') {
				url = url.substring(1);
			}
			
			if (url.substring(0, 6) != 'manage') {
				url = 'manage/' + url;
			}

			return this.getUrl(url, params);
		},
		
		/**
		 * 准备需要被渲染的Module
		 */
		readyModule: function(mdl) {
			module = mdl;
		},
		
		registerModule: registerModule,
		unregisterModule: removeModule,
		findModule: findModule,
		
		getActiveModule: function() {
			alert('active: ' + module);
			return module;
		},
		
		/**
		 * 创建窗口
		 * @param Object winCfg
		 * @param String winClazz
		 */
		createWindow: function(winCfg, winClazz) {
			winClazz = eval(winClazz);
			return new winClazz(winCfg);
		},
		
		/**
		 * 信息日志
		 */
		info: log,
		
		/**
		 * 调试日志
		 */
		debug: function(msg) { log(msg, 'debug'); },
		
		/**
		 * 错误日志
		 */
		err: function(e) { log(e, 'error'); },
		
		/**
		 * 兼容以前的
		 * @deprecated
		 */
		module: function(mdl) {
			module = mdl;
		}
	};
})();

// 初始化框架的操作必须在外面执行，否则自动加载的框架库类中无法使用framework对象
framework.initFramework();

Ext.override(Function, {
	/**
	 * @param {Function} fnc
	 * @param {Object} obj
	 * @param {Array} args
	 */
	timer: function(fnc, obj, args) {
		var me = this,
			timer = null,
			fn = me.createDelegate(obj, args);
		
		var i = 0;
		(function() {
			if (i < 100 && !fnc || fnc.apply(obj || me)) {
				clearTimeout(timer);
				timer = null;
				
				fn();
			} else {
				timer = setTimeout(arguments.callee, 0);
			}
		})();
	}
});