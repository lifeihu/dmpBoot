_package('com.sw.bi.scheduler.application');

_import([
	'com.sw.bi.scheduler.application.ApplicationModuleContainer',
	'framework.widgets.window.MaintainWindow'
]);

com.sw.bi.scheduler.application.ApplicationModule = Ext.extend(framework.core.Module, {
	border: true,
	
	initModule: function() {
		this.tplHeader = new Ext.XTemplate(
			'<div class="application-header">',
				'<div class="application-name">{app}</div>',
				'<div class="login-info">欢迎您, <b>{user}</b><br>{loginDate}</div>',
			'</div>'
		);

		this.tbar = this.createApplictionMenus();
		
		com.sw.bi.scheduler.application.ApplicationModule.superclass.initModule.call(this);
	},
	
	/*onModuleRender: function() {
		// 禁用右键
		this.el.on('contextmenu', function(e) {e.stopEvent();});
		com.sw.bi.scheduler.application.ApplicationModule.superclass.onModuleRender.apply(this, arguments);
	},*/
	
	/*north: function() {
		return {
			frame: false,
			border: true,
			
			split: true,
			collapseMode: 'mini',
			collapsed: false,

			height: 80,
			maxHeight: 80,
			
			html: 'Header'
		};
	},*/
	
	center: function() {
		return {
			xtype: 'applicationmodulecontainer'
		};
	},
	
	/*south: function() {
		return {
			frame: false,
			border: true,
			
			split: true,
			collapseMode: 'mini',
			collapsed: false,
			
			height: 40,
			html: 'Footer'
		};
	},*/
	
	/**
	 * 创建项目菜单
	 */
	createApplictionMenus: function() {
		var mdl = this,
			items = [],
			menus = framework.syncRequest('permission/menuTree?userId=' + USER_ID);
		
			/**
			 * 创建工具按钮的子菜单
			 */
			createChildrenMenu = function(parent) {
				if (Ext.isEmpty(parent)) return null;
				
				var items = parent.children;
				if (Ext.isEmpty(items) || items.length == 0) return null;
				
				var mis = [];
				Ext.iterate(items, function(item) {
					item.parent = parent;
					
					mis.push({
						text: item.name,
						iconCls: item.iconCls,
						menu: createChildrenMenu(item),
						
						handler: mdl.onMenuClick.createDelegate(mdl, [item], true),
						scope: mdl
					});
				});
				
				return new Ext.menu.Menu({items: mis});
			};

		if (!Ext.isEmpty(menus, false)) {
			menus = Ext.decode(menus);
			
			Ext.iterate(menus, function(menu) {
				var childMenu = createChildrenMenu(menu);

				items.push({
					iconCls: menu.iconCls,
					scale: 'medium',
					iconAlign: 'top',
					arrowAlign: 'bottom',
					text: menu.name,
					menu: childMenu,
					
					handler: !Ext.isEmpty(childMenu) ? null : mdl.onMenuClick.createDelegate(mdl, [menu], true),
					scope: mdl
				});
			});
		} else {
			items.push('您无权访问本系统!');
			
		}
		
		///////////////////////////////////
		
		items.push(
			'->', 
			
			mdl.tplHeader.apply({
				app: PROJECT_NAME,
				user: USER_NAME + '(' + USER_GROUP_NAME + ')',
				loginDate: new Date().format('Y年n月j日 星期l')
				
			}), '-', {
				text: 'ETL工具箱',
				iconCls: 'menu-toolbox24',
				scale: 'medium',
				iconAlign: 'top',
				arrowAlign: 'bottom',
				
				menu: {
					xtype: 'menu',
					items: [{
						text: '查询建表语句',
						handler: mdl.onMenuClick.createDelegate(mdl, [{
							name: 'ETL工具箱 > 查询建表语句',
							url: 'com.sw.bi.scheduler.toolbox.ViewCreateTableModule'
						}], true),
						scope: mdl
					}, {
						iconCls: 'hudson',
						text: 'Hudson同步',
						handler: mdl.onMenuClick.createDelegate(mdl, [{
							name: 'ETL工具箱 > Hudson同步',
							// url: 'com.sw.bi.scheduler.toolbox.SyncHudsonModule'
							url: 'com.sw.bi.scheduler.hudson.HudsonProjectModule'
						}], true)
					}]
				}
				
			}, {
				text: 'Hadoop平台',
				iconCls: 'menu-hadoop24',
				scale: 'medium',
				iconAlign: 'top',
				arrowAlign: 'bottom',
				
				menu: {
					xtype: 'menu',
					items: [{
						text: '作业运行界面',
						url: 'http://172.16.15.33:50030/jobtracker.jsp'
					}, {
						text: 'tasktracker列表',
						url: 'http://172.16.15.33:50030/machines.jsp?type=active'
					}, {
						text: 'datanode列表',
						url: 'http://172.16.15.33:50070/dfsnodelist.jsp?whatNodes=LIVE'
					}, {
						text: 'cluster summary',
						url: 'http://172.16.15.33:50070/dfshealth.jsp'
					}, {
						text: 'fair scheduler',
						url: 'http://172.16.15.33:50030/scheduler'
					}, {
						text: 'ganglia界面',
						url: 'http://172.16.15.120/ganglia/'
					}, {
						text: 'load查看',
						url: 'http://172.16.15.120/ganglia/?p=2&c=shunwang-hadoop-cluster&h=&hc=4&p=1'
					}],
					
					listeners: {
						click: function(menu, item) {
							window.open(item.url);
							/*framework.createWindow({
								useIframeContainer: true,
								
								title: item.text
							}, 'framework.widgets.window.ModuleWindow').open({
								url: item.url
							});*/
						}
					}
				}
			}, '-', {
				text:  '修改密码',
				iconCls: 'menu-password24',
				scale: 'medium',
				iconAlign: 'top',
				arrowAlign: 'bottom',
				handler: mdl.changePassword,
				scope: mdl
				
			}, {
				text: '退出系统',
				//split: true,
				iconCls: 'menu-exit24',
				scale: 'medium',
				iconAlign: 'top',
				arrowAlign: 'bottom',
				
				listeners: {
					click: function() { location.href = mdl.getUrl('/j_spring_security_logout'); },
					single: true
				}
			}
		);
		
		return items; // new Ext.ButtonGroup({items: items});
	},
	
	changePassword: function() {
		this.createWindow({
			title: '修改密码',
			
			width: 300,
			height: 120,
			minHeight: 120,
			
			module: 'com.sw.bi.scheduler.user.UserPasswordMaintainModule'
		}, 'framework.widgets.window.MaintainWindow').create();
	},
	
	/////////////////////////////////////
	
	onMenuClick: function(item, e, resource) {
		if (!Ext.isEmpty(item.menu)) return false;

		this.centerPnl.loadResource(resource);
	}
});

framework.readyModule('com.sw.bi.scheduler.application.ApplicationModule');