_package('com.sw.bi.scheduler.task');

_import([
	'framework.widgets.raphael.TaskShape'
]);

com.sw.bi.scheduler.task.TaskRelationChartModule = Ext.extend(framework.core.Module, {
	minHeight: 0,
	
	/**
	 * @property module
	 * @type com.sw.bi.scheduler.task.TaskModule
	 */
	module: null,
	
	/**
	 * @type Paper 
	 */
	canvas: null,
	
	/**
	 * 各任务框垂直间距
	 * @type Number
	 */
	vertialSpacing: 85,
	
	/**
	 * 各任务框水平间距
	 * @type Number
	 */
	horizontalSpacing: 20,
	
	/**
	 * 画布信息
	 * @type Object
	 */
	viewBox: {},
	
	/**
	 * 画布是否允许拖动
	 * @type Boolean
	 */
	dragable: true,
	
	/**
	 * 所有任务
	 * @private
	 * @type Object
	 */
	allTasks: null,
	
	/**
	 * 所有周期是小时/分钟的任务
	 * @type 
	 */
	allMergeTasks: null,
	
	center: function() {
		return {
			xtype: 'panel',
			
			tbar: [{
				xtype: 'button',
				iconCls: 'zoom-in',
				tooltip: '缩小',
				handler: this.zoomIn,
				scope: this
			}, {
				xtype: 'slider',
				width: 200,
				minValue: 30,
				maxValue: 200,
				increment: 10,
				value: 100,
				plugins: new Ext.slider.Tip({
					getText: function(thumb){
			            return String.format('<b>缩放级别: {0}%</b>', thumb.value);
			        }
				}),
				listeners: {
					change: function(slider, value, thumb) {
						this.zoom(value / 100);
					},
					scope: this
				}
			}, {
				xtype: 'button',
				iconCls: 'zoom-out',
				tooltip: '放大',
				handler: this.zoomOut,
				scope: this
			}/*, {
				xtype: 'button',
				iconCls: 'zoom-fit',
				tooltip: '合适大小',
				handler: this.zoomFit,
				scope: this
			}*/]
		};
	},
	
	/**
	 * 添加根任务
	 * @param {Object} task
	 */
	addRootTask: function(task) {
		var me = this;

		me.allTasks = {};
		me.allMergeTasks = {};
			
		CanvasMgr.clear();
		
		var bsize = me.centerPnl.body.getSize(true),
			
			viewBox = me.setViewBox({
				x: 0, 
				y: 0,
			
				width: bsize.width,
				height: bsize.height
			}, true);
			
		me.root = me.createTaskShape(Ext.apply(task, {
			root: true,
			
			x: viewBox.rx,
			y: viewBox.ry
		}));
				
		Ext.Msg.wait('正在加载相应的父子任务,请耐心等候...', '提示');
		
		(function() {
			me.addParentTask(me.root.id, 3);
			me.addChildrenTask(me.root.id, 3);
			
			Ext.Msg.hide();
		}).defer(500);
	},
	
	addParentTask: function(taskId, depth) {
		var mdl = this,
			
			relations = framework.syncRequest({
				url: 'task/parents?taskId=' + taskId + '&depth=' + depth,
				decode: true
			}),
			
			viewBox = mdl.viewBox,
			
			tasks = relations[taskId],
			result = mdl.preprocess(tasks, relations, 0);

		viewBox.maxWidth = result.width;
		viewBox.parentDepth = result.depth;
		
		mdl.createRelationTasks(tasks, mdl.root, relations, false);
	},
	
	/**
	 * 添加指定层的子任务
	 * @param {Number} taskId
	 * @param {Number} depth
	 */
	addChildrenTask: function(taskId, depth) {
		var mdl = this,
			
			relations = framework.syncRequest({
				url: 'task/children?taskId=' + taskId + '&depth=' + depth + '&allowFetchParent=true',
				decode: true
			}),
			
			viewBox = mdl.viewBox,
			
			tasks = relations[taskId],
			result = mdl.preprocess(tasks, relations, 0);
			
		viewBox.maxWidth = Math.max(viewBox.maxWidth, result.width);
		viewBox.childrenDepth = result.depth;
		
		var level = viewBox.parentDepth + viewBox.childrenDepth + 1;
		viewBox.maxHeight = (TASK_SHAPE_SIZE.HEIGHT + mdl.vertialSpacing) * level - mdl.vertialSpacing;
		
		// 画子任务
		mdl.createRelationTasks(tasks, mdl.root, relations, true);
		
		// 画子任务中未画的父任务(即一个子任务有多个父任务)
		var parents = [],
			height = 0,
			
			horizontalSpacing = mdl.horizontalSpacing,
			vertialSpacing = mdl.vertialSpacing,
			
			yAxisCount = {};
			
		Ext.iterate(result.parents, function(parentTaskId) {
			var token = result.parents[parentTaskId],
				parent = token[0];
				
			if (mdl.allMergeTasks[parent.jobId]) {
				return;
			}
				
			var parentShape = CanvasMgr.getTask(parent.taskId),
				childShape = CanvasMgr.getTask(token[1]);
			
			if (Ext.isEmpty(parentShape)) {
				var text = CanvasMgr.processText(parent.name),
				
					y = childShape.getBox().y - vertialSpacing,
					count = yAxisCount[y] || 0,
					x = count * text.width + (count + 1) * horizontalSpacing;

				parent.x = x;
				parent.y = y;
				parent.w = text.width;
				parent.h = text.height;
				parent.child = childShape;

				height += text.height;
				parents.push(parent);
				
				yAxisCount[y] = count + 1;
				
			} else {
				// 父子任务都已经创建则直接添加关联关系
				parentShape.relation(childShape);
			}
		});

		if (parents.length > 0) {
			var sx = viewBox.cx + result.width / 2;

			Ext.iterate(parents, function(parent) {
				parent.sx = sx; // sx;
				parent.sy = 0; // sy;
				mdl.createTaskShape(parent);
			});
			
			var maxCount = 0;
			Ext.iterate(yAxisCount, function(y) {
				maxCount = Math.max(yAxisCount[y], maxCount);
			});
			mdl.viewBox.maxWidth += maxCount * TASK_SHAPE_SIZE.WIDTH + (maxCount - 1) * horizontalSpacing;
		}
	},
	
	/**
	 * 创建关联作业
	 * @param {Array} tasks
	 * @param {TaskShape} refer
	 * @param {Map} relations
	 * @param {Boolean} isChildren
	 */
	createRelationTasks: function (tasks, refer, relations, isChildren) {
		if (tasks == null || tasks.length == 0) return;
		
		var mdl = this,
			box = refer.getBox();
			
		var cacheRefer = mdl.allTasks[refer.id],
			
			vertialSpacing = mdl.vertialSpacing,
			maxWidth = 0,
			sx = 0,
			sy = box.y;
			
		if (cacheRefer == null) {
			Ext.iterate(tasks, function(task) {
				task = mdl.allTasks[task.taskId];
				maxWidth += task.width;
			});
		} else {
			maxWidth = cacheRefer.width;
		}

		sx = box.x - maxWidth / 2 + box.width / 2;

		Ext.iterate(tasks, function(task) {
			var shape = CanvasMgr.getTask(task.taskId);
			
			if (Ext.isEmpty(shape)) {
				task = mdl.allTasks[task.taskId];
				
				var width = task.width; // 所有子任务宽度合计
				
				task.sx = sx;
				task.sy = sy;
				task.x = width / 2 - task.w / 2;
				task.y = vertialSpacing * (isChildren ? 1 : -1);
				
				sx += width;
			}
			
			task[isChildren ? 'parent' : 'child'] = refer;
			
			var t = mdl.createTaskShape(task);
			
			mdl.createRelationTasks(relations[t.id], t, relations, isChildren);
		});
	},
	
	/**
	 * 级联计算每个任务的最大宽度、最大深度
	 * @param {Array<Task>} tasks
	 * @param {Object<Long, Task>} relations
	 * @param Array<Task> parents
	 * @return {}
	 */
	preprocess: function(tasks, relations, depth) {
		var mdl = this,
		
			maxWidth = 0, // 每个任务的最大宽度(需要通过遍历其所有子任务计算得到)
			maxDepth = 0, // 最大层数
			
			parents = {},
			horizontalSpacing = mdl.horizontalSpacing;

		if (tasks && tasks.length > 0) {
			depth += 1;
			maxDepth = Math.max(depth, maxDepth);
		}
			
		Ext.iterate(tasks, function(task) {
			var t = mdl.allTasks[task.taskId];
			if (!Ext.isEmpty(t)) {
				// 当前任务已经存在则忽略
				return;
			}
			
			var text = CanvasMgr.processText(task.name || task.jobName),
				
				w = text.width + horizontalSpacing,
				h = text.height,
				
				c = relations[task.taskId],
				p = task.parents || [];
				
			if (Ext.isEmpty(task.width)) {
				task.width = 0;
			}
				
			if (c) {
				// 当前任务在缓存中不存在时则需要递归计算其子节点的宽度
				var result = mdl.preprocess(c, relations, depth);
				
				maxWidth += Math.max(result.width, w);
				maxDepth = Math.max(maxDepth, result.depth);
				
				task.width = Math.max(result.width, w);
				parents = Ext.apply(parents, result.parents);
				
			} else {
				maxWidth += w;
				task.width = w;
			}
			
			task.w = text.width;
			task.h = h;
			
			mdl.allTasks[task.taskId] = task;
			
			Ext.iterate(p, function(parent) {
				var parentTaskId = parent.taskId;
				parents[framework.keyString(parent.taskId, task.taskId)] = [parent, task.taskId];
			});
		});
		
		return {
			width: maxWidth,
			depth: maxDepth,
			parents: parents
		};
	},
	
	/**
	 * 创建任务框
	 * @param {Object} shapeConfig
	 * 		root: Boolean 是否根作业
	 * 		sx: Number 开始X坐标
	 * 		sy: Number 开始Y坐标
	 * 		parent: TaskShape 父任务
	 * 		child: TaskShape 子任务
	 */
	createTaskShape: function(shapeConfig) {
		var me = this, 
			canvas = me.canvas,
		
			isRoot = shapeConfig.root === true,
			text = CanvasMgr.processText(shapeConfig.name || shapeConfig.jobName),
			
			sx = shapeConfig.sx || 0,
			sy = shapeConfig.sy || 0,
			
			parent = shapeConfig.parent,
			child = shapeConfig.child;

		shapeConfig.x = sx + (shapeConfig.x || 0);
		shapeConfig.y = sy + (shapeConfig.y || 0);

		if (Ext.isEmpty(shapeConfig.w, false)) {
			shapeConfig.w = text.width;
		}
		
		if (Ext.isEmpty(shapeConfig.h, false)) {
			shapeConfig.h = text.height;
		}
		
		shapeConfig.id = shapeConfig.id || shapeConfig.taskId;
		// shapeConfig.name = text.text;
		shapeConfig.text = text.text;
		shapeConfig.jobId = shapeConfig.jobId;
		
		var dutyOfficer = shapeConfig.dutyOfficer;
		if (typeof dutyOfficer == 'number') {
			var user = me.module.storeUser.queryUnique('userId', shapeConfig.dutyOfficer);
			shapeConfig.dutyOfficer = user ? user.get('realName') : null;
		}
		shapeConfig.userGroupName = shapeConfig.userGroup == null ? null : shapeConfig.userGroup.name;
		
		delete shapeConfig.root;
		delete shapeConfig.sx;
		delete shapeConfig.sy;
		delete shapeConfig.parent;
		delete shapeConfig.child;
			
		if (shapeConfig.cycleType == JOB_CYCLE_TYPE.HOUR || shapeConfig.cycleType == JOB_CYCLE_TYPE.MINUTE) {
			me.allMergeTasks[shapeConfig.jobId] = shapeConfig;
		}
		
		var t = CanvasMgr.getTask(shapeConfig.id);
		if (t == null) {
			// 得到绘制的任务中最小X坐标，用于缩放到合适大小的功能
			me.viewBox.minX = Math.min(me.viewBox.minX, shapeConfig.x);
			
			t = canvas.task(shapeConfig);

			if (isRoot) {
				t.rect.attr({'stroke-dasharray': '-'});
				// t.text.attr({'font-weight': 'bold', 'font-size': '13px'});
			}
			
			t.on({
				dblclick: {
					fn: me.addRootTask.createDelegate(me, [shapeConfig]),
					single: true
				},
				contextmenu: me.onContextMenu.createDelegate(me),
				scope: me
			});
			
			CanvasMgr.registeTask(t);
			
		} else {
			
		}
		
		if (!Ext.isEmpty(parent)) {
			parent.relation(t);
		} 

		if (!Ext.isEmpty(child)) {
			t.relation(child);
		}
		
		return t;
	},
	
	/**
	 * 缩放
	 * @param {Object/Number} config
	 */
	zoom: function(config) {
		if (Ext.isNumber(config)) {
			config = {zoomRatio: config};
		}
		
		var mdl = this,
		
			zoomRatio = config.zoomRatio,
			bsize = mdl.centerPnl.body.getSize(true),
			bw = bsize.width,
			bh = bsize.height;
			
		if (zoomRatio == 1) {
			mdl.setViewBox(bsize);
			return;
		}
		
		var vb = mdl.viewBox,
		
			// 可视区域的宽度和高度
			bw = bsize.width,
			bh = bsize.height,
			
			// 绘制的任务树的宽度和高度
			mw = vb.maxWidth,
			mh = vb.maxHeight,
			
			// 按指定比例缩放后的宽度
			sw = zoomRatio > 1 ? bw / zoomRatio : bw * zoomRatio,
			// 缩放前与缩放后的宽度比
			sratio = bw / sw,
			// 根据宽度比计算缩放后的高度
			sh = zoomRatio > 1 ? bh / sratio : bh * sratio;

		mdl.setViewBox({
			width: sw,
			height: sh
		});
	},
	
	/**
	 * 缩小
	 */
	zoomIn: function() {
		var mdl = this,
			slider = mdl.centerPnl.getTopToolbar().getComponent(1);

		(function() {
			slider.setValue(0, Math.max(slider.getValue() - 10, slider.minValue), true);
		}).defer(100);
	},
	
	/**
	 * 放大
	 */
	zoomOut: function() {
		var mdl = this,
			slider = mdl.centerPnl.getTopToolbar().getComponent(1);
			
		(function() {
			slider.setValue(0, Math.min(slider.getValue() + 10, slider.maxValue), true);
		}).defer(100);
	},
	
	/**
	 * 合适大小
	 */
	zoomFit: function() {
		var mdl = this,
		
			bsize = mdl.centerPnl.body.getSize(true),
			bw = bsize.width,
			bh = bsize.height,
			
			vb = mdl.viewBox,
			mw = vb.maxWidth,
			mh = vb.maxHeight,
			
			sw = 0,
			sh = 0;
		
		sratio = mw / bw;
		sw = mw * sratio;
		sh = mh / sratio;

		mdl.setViewBox({
			// TODO 对于存在孤立父任务的情况暂时不能完美计算出x坐标
			x: vb.minX + mdl.horizontalSpacing / 2, // vb.cx - (mw / 2) - mdl.horizontalSpacing,
			y: vb.y - bh, // - (mh / 2) - mdl.vertialSpacing,
			
			width: mw,
			height: mh
		});
	},
	
	/**
	 * 获得当前缩放比例
	 * @return {}
	 */
	getZoomRatio: function() {
		return this.centerPnl.getTopToolbar().getComponent(1).getValue() / 100;
	},
	
	/**
	 * 设置画布可视区域
	 * @param {Object} viewBox
	 * @param {Boolean} clear 是否清理一些临时变量
	 * @return {}
	 */
	setViewBox: function(viewBox, clear) {
		var mdl = this,
			vb = mdl.viewBox;
		
		vb = Ext.apply(vb, viewBox);
		
		// 右下角坐标
		vb.x2 = vb.x + vb.width;
		vb.y2 = vb.y + vb.height;
		
		// 根任务的坐标
		vb.rx = vb.x + (vb.width - TASK_SHAPE_SIZE.WIDTH) / 2;
		vb.ry = vb.y + (vb.height - TASK_SHAPE_SIZE.HEIGHT) / 2;
		
		// 中心点坐标
		vb.cx = vb.x + vb.width / 2;
		vb.cy = vb.y + vb.height / 2;

		if (clear === true) {
			vb.lastMoveX = null;
			vb.lastMoveY = null;
			vb.maxWidth = null;
			vb.maxHeight = null;
			vb.maxDepth = null;
			vb.minX = 0;
		}

		mdl.canvas.setViewBox(vb.x, vb.y, vb.width, vb.height);
		
		return vb;
	},
	
	////////////////////////////////////////////////////////////////////////
	
	onModuleRender: function(mdl) {
		com.sw.bi.scheduler.task.TaskRelationChartModule.superclass.onModuleRender.call(this, mdl);
		
		var body = mdl.centerPnl.body,
			bodySize = body.getSize(true);
			
		body.setStyle({
			'-moz-user-select': 'none',
			'-khtml-user-select': 'none'
		});
		body.dom.unselectable = 'on';
		
		// 初始化画布

		mdl.canvas = Raphael(body.id, bodySize.width, bodySize.height);
		CanvasMgr.init(mdl.canvas);
		
		mdl.setViewBox({
			x: 0,
			y: 0,
			
			width: bodySize.width,
			height: bodySize.height
		});
		
		// 初始化事件
		
		mdl.on({
			bodyresize: mdl.onModuleResize,
			scope: mdl
		});
		
		body.on({
			mousedown: mdl.onCanvasMouseDown,
			mouseup: mdl.onCanvasMouseUp,
			mousemove: mdl.onCanvasMove,
			scope: mdl
		});
	},
	
	onModuleResize: function() {
		com.sw.bi.scheduler.task.TaskRelationChartModule.superclass.onModuleResize.apply(this, arguments);
		
		var mdl = this,
			body = mdl.centerPnl.body;
		
		(function() {
			bodySize = body.getSize(true);

			mdl.canvas.setSize(bodySize.width, bodySize.height);
			mdl.zoom(mdl.getZoomRatio());
		}).defer(100);
	},
	
	onCanvasMove: function(e, target) {
		if (!this.dragable) return;
		
		var mdl = this,
			viewBox = mdl.viewBox;
			
		if (!viewBox.lastTarget) return;

		if (e.button !== 0)
			return;
		
		var mdl = this,
			viewBox = mdl.viewBox,
		
			x = e.getPageX(),
			y = e.getPageY(),
			
			lx = viewBox.lastMoveX || x,
			ly = viewBox.lastMoveY || y,
			
			mw = viewBox.maxWidth || 0,
			mh = viewBox.maxHeight || 0;

		if (Math.abs(x - lx) > 1) {
			viewBox.x += Math.abs(mw / viewBox.width * 30) * (x > lx ? -1 : 1);
		}
		
		if (Math.abs(y - ly) > 2) {
			viewBox.y += Math.abs(mh / viewBox.height * 30) * (y > ly ? -1 : 1);
		}

		viewBox.lastMoveX = x;
		viewBox.lastMoveY = y;
		
		mdl.setViewBox(viewBox);
	},
	
	onCanvasMouseDown: function(e, target) {
		var mdl = this;
		
		if (target.tagName == 'svg' || target.tagName == 'DIV') {
			mdl.viewBox['lastTarget'] = target;
		}
	},
	
	onCanvasMouseUp: function(e, target) {
		var mdl = this,
			vb = mdl.viewBox;
				
		vb['lastMoveX'] = null;
		vb['lastMoveY'] = null;
		vb['lastTarget'] = null;
	},
	
	onContextMenu: function(taskShape, e, target) {
		var mdl = this,
			menu = mdl.module.taskActionMenu;

		// taskShape.select(true);

		menu.currentTask = taskShape.getValue();
		menu.from = taskShape;
		
		menu.showAt(e.getXY());
	}
	
});

Ext.reg('taskrelationchart', com.sw.bi.scheduler.task.TaskRelationChartModule);