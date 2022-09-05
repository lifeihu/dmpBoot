_package('framework.widgets.raphael');

_import([
	'framework.widgets.raphael.raphael'
]);

/**
 * 任务图形
 * @class framework.widgets.raphael.TaskShape
 * @extends Ext.util.Observable
 */
framework.widgets.raphael.TaskShape = Ext.extend(Ext.util.Observable, {
	
	/**
	 * 任务框X坐标
	 * @type Number
	 */
	x: 0,
	
	/**
	 * 任务框Y坐标
	 * @type Number
	 */
	y: 0,
	
	/**
	 * 任务框宽度
	 * @type Number
	 */
	w: null,
	
	/**
	 * 任务框高度
	 * @type Number
	 */
	h: null,
	
	/**
	 * 任务框圆角半径
	 * @type Number
	 */
	r: 8,
	
	/**
	 * 该任务是否合并了小时/分钟任务
	 * @type Boolean
	 */
	merge: false,
	
	/**
	 * 任务ID
	 * @type 
	 */
	taskId: null,
	
	/**
	 * 任务名称
	 * @type String 
	 */
	name: '',
	
	/**
	 * 作业ID
	 * @type 
	 */
	jobId: null,
	
	/**
	 * 作业类型
	 * @type 
	 */
	jobType: null,
	
	/**
	 * 任务状态
	 * @type Number
	 */
	taskStatus: 0,
	
	/**
	 * 任务日期
	 * @type 
	 */
	taskDate: null,
	
	/**
	 * 责任人
	 * @type 
	 */
	dutyOfficer: null,
	
	/**
	 * 用户组
	 * @type 
	 */
	userGroupName: null,
	
	/**
	 * 任务开始时间
	 * @type 
	 */
	taskBeginTime: null,
	
	/**
	 * 任务结束时间
	 * @type 
	 */
	taskEndTime: null,
	
	/**
	 * 最后一次执行情况
	 * @type 
	 */
	lastActionId: null,
	
	/**
	 * 任务矩形框
	 * @type Raphael.Element
	 */
	rect: null,
	
	/**
	 * 任务文本
	 * @type Raphael.Element
	 */
	text: null,
	
	/**
	 * 所有关系
	 * @type 
	 */
	connectors: {},
	
	/**
	 * 父任务集
	 * @type Array<framework.widgets.raphael.TaskShape>
	 */
	parents: null,
	
	/**
	 * 子任务集
	 * @type Array<framework.widgets.raphael.TaskShape>
	 */
	children: null,
	
	/**
	 * 是否隐藏该任务
	 * @type Boolean
	 */
	hidden: false,
	
	/**
	 * 是否展开子任务
	 * @type Boolean
	 */
	childrenExpanded: true,
	
	/**
	 * 是否展开父任务
	 * @type Boolean
	 */
	parentExpanded: true,

	/**
	 * 是否选中关联关系(true: 选中关系任务, false: 只选中该任务, null: 未选中)
	 * @type Boolean
	 */
	relationSelected: null,
	
	constructor: function(config) {
		if (Ext.isEmpty(this.id, false)) {
			this.id = Ext.id(this, 'task-shape');
		}
		
		framework.widgets.raphael.TaskShape.superclass.constructor.apply(this, arguments);
		
		Ext.apply(this, Ext.apply({
			parents: [],
			children: [],
			w: TASK_SHAPE_SIZE.WIDTH,
			h: TASK_SHAPE_SIZE.HEIGHT
		}, config));
		
		this.addEvents(
			'click',
			
			'dblclick',
			
			/**
			 * @event contextmenu
			 * @param framework.widgets.raphael.TaskShape ts
			 * @param Object e
			 */
			'contextmenu'
		);
		
		this.draw();
	},
	
	draw: function() {
		var me = this,
			p = me.paper,
			
			// 矩形坐标
			x = me.x,
			y = me.y,
			w = me.w,
			h = me.h,
			r = me.r,
			
			// 文字坐标
			tx = x + w / 2,
			ty = y + h / 2,
			
			rect = null,
			text = null;

		me.rect = rect = p.rect(me.x, me.y, me.w, me.h, me.r);
		rect.attr({
			'fill': me.merge ? FOREGROUND_STATUS_COLOR[me.taskStatus] : STATUS_COLOR[me.taskStatus],
			'stroke': me.merge ? TASK_SHAPE_COLOR.MERGE_LINE : TASK_SHAPE_COLOR.LINE,
			'stroke-width': 2,
			'opacity': .8
		});
		
		me.text = text = p.text(tx, ty, me.text);
		text.attr({
			'fill': TASK_SHAPE_COLOR.TEXT,
			'font-size': '12px',
			'cursor': 'default'
		});
		
		Ext.QuickTips.register({
			target: [rect.node, text.node],
			title: '作业信息',
			text: me.getTooltip()
		});
		
		this.initEvents()
	},
	
	initEvents: function() {
		this.draggable();
		
		var me = this,
		
			rect = me.rect,
			text = me.text,
			
			onClick = function() {
				me[me.relationSelected !== null ? 'unselect' : 'select']();
				
				me.fireEvent('click', me);
			},
			
			onDblClick = function() {
				me.fireEvent('dblclick', me);
			},
			
			onContextMenu = function(e, target) {
				e.stopEvent();
				me.fireEvent('contextmenu', me, e, target);
			};
			
		rect.click(onClick);
		text.click(onClick);
		
		rect.dblclick(onDblClick);
		text.dblclick(onDblClick);
		
		rect.hover(me.over.createDelegate(me, [true]), me.out.createDelegate(me));
		text.hover(me.over.createDelegate(me, [true]), me.out.createDelegate(me));
		
		Ext.fly(rect.node).on('contextmenu', onContextMenu);
		Ext.fly(text.node).on('contextmenu', onContextMenu);
	},
	
	/**
	 * 折叠所有子任务
	 */
	/*collapseChildren: function() {
		var me = this,
			connectors = me.connectors;
			
		me.childrenExpanded = false;
		
		Ext.iterate(me.children, function(child) {
			// 隐藏当前任务与指定子任务的关系
			var connector = connectors[me.id + '|' + child.id];
			connector.hide();
			
			// 隐藏指定子任务
			child.hide(true);
		});
	},*/
	
	/**
	 * 展开所有子任务
	 */
	/*expandChildren: function() {
		var me = this,
			connectors = me.connectors;
			
		me.childrenExpanded = true;
		
		Ext.iterate(me.children, function(child) {
			// 隐藏当前任务与指定子任务的关系
			var connector = connectors[me.id + '|' + child.id];
			connector.show();
			
			// 隐藏指定子任务
			child.show();
		});
	},
	
	hide: function() {
		if (this.hidden === true) return;
		
		var me = this,
			parents = me.parents;
		
		Ext.iterate(parents, function(parent) {
			// 隐藏指定父任务与当前任务的关系
			var connector = parent.connectors[parent.id + '|' + me.id];
			if (connector) connector.hide();
		});
		
		// 折叠当前任务的子任务
		me.collapseChildren();

		me.rect.hide();
		me.text.hide();
		
		me.hidden = true;
		me.childrenExpanded = false;
		me.parentExpanded = false;
	},
	
	show: function() {
		if (this.hidden === false) return;
		
		var me = this,
			parents = me.parents;
		
		Ext.iterate(parents, function(parent) {
			// 隐藏指定父任务与当前任务的关系
			var connector = parent.connectors[parent.id + '|' + me.id];
			if (connector) connector.show();
		});
		
		// 折叠当前任务的子任务
		me.expandChildren();

		me.rect.show();
		me.text.show();
		
		me.hidden = false;
		me.childrenExpanded = true;
		me.parentExpanded = true;
	},*/

	relation: function(to) {
		var from = this,
			key = from.id + '|' + to.id;
			
		if (!CanvasMgr.hasConnector(key)) {
			var connector = new framework.widgets.raphael.TaskConnector({
				from: from,
				to: to
			});
			
			CanvasMgr.registeConnector(connector);
			
			/*from.addChild(to);
			to.addParent(from);*/
		}
	},
	
	/*addParent: function(parent) {
		this.parents.push(parent);
	},
	
	addChild: function(child) {
		this.children.push(child);
	},*/
	
	getBox: function() {
		return this.rect.getBBox();
	},
	
	/**
	 * 获得作业的提示信息
	 * @return {String}
	 */
	getTooltip: function() {
		var me = this,
			tooltip = [];

		// tooltip.push('坐标: ' + me.x + ', ' + me.y);
		// tooltip.push('最大宽度: ' + me.width);
		tooltip.push('作业ID: ' + me.jobId);
		tooltip.push('任务ID: ' + me.taskId);
		tooltip.push('作业名称: ' + me.name.replace('\n', ''));
		tooltip.push('任务日期: ' + me.taskDate);
		tooltip.push('预设时间: ' + me.settingTime);
		tooltip.push('作业状态: <b>' + (me.merge ? FOREGROUND_TASK_STATUS_NATIVE[me.taskStatus] : FOREGROUND_TASK_STATUS[me.taskStatus]) + '</b>');
		tooltip.push('责任人: ' + me.dutyOfficer);
		tooltip.push('用户组: ' + me.userGroupName);
		tooltip.push('作业类型: ' + S.create('jobType').queryUnique('value', me.jobType).get('name'));
		
		if (!Ext.isEmpty(me.taskBeginTime, false)) {
			tooltip.push('开始时间: ' + me.taskBeginTime);
		}
		
		if (!Ext.isEmpty(me.taskEndTime, false)) {
			tooltip.push('结束时间: ' + me.taskEndTime);
		}
		
		return tooltip.join('<br>');
	},
	
	/**
	 * 选中任务
	 * @param {Boolean} single 是否单选
	 */
	select: function(single) {
		var me = this;
		
		if (me.relationSelected == null) {
			me.over();
		}
		
		if (single !== true) {
			// 如果不是单选，则选中所有关联关系
			var connectors = CanvasMgr.getConnectors(me);
			Ext.iterate(connectors, function(connector) {
				connector.select();
			});
			
			me.relationSelected = true;
			
		} else {
			// 如果之前未选中，则更改为单选
			if (me.relationSelected == null)
				me.relationSelected = false;
		}
	},
	
	/**
	 * 取消选中
	 * @param {Boolean} single 是否单个取消
	 */
	unselect: function(single) {
		var me = this;
		if (me.relationSelected == null) return;
		
		if (single === true) {
			// 如果当前任务已经选择了关联关系，则该任务不应该被取消选中
			if (me.relationSelected === true) {
				return;
			}
			
		} else {			
			var connectors = CanvasMgr.getConnectors(me);
			Ext.iterate(connectors, function(connector) {
				connector.unselect();
			});
		}
			
		me.out();
		me.relationSelected = null;
	},
	
	over: function() {
		if (this.relationSelected != null) return false;
		
		var me = this,
			rect = me.rect;
			
		rect.attr({
			'stroke': TASK_SHAPE_COLOR.SELECT,
			'stroke-width': 2
		});
		
		return true;
	},
	
	out: function() {
		if (this.relationSelected != null) return false;
		
		var me = this,
			rect = me.rect;
			
		rect.attr({
			'stroke': me.merge ? TASK_SHAPE_COLOR.MERGE_LINE : TASK_SHAPE_COLOR.LINE,
			'stroke-width': 2
		});
		
		return true;
	},
	
	refresh: function() {
		var me = this;
		
		me.text.attr({'text': '正在加载任务信息...'});
		
		(function() {
			var task = framework.syncRequest({
				url: 'task?id=' + me.id,
				decode: true,
				root: 'task'
			});
			
			me.setValue(task);
		}).defer(100);
	},
	
	setValue: function(task) {
		Ext.apply(this, task);
		
		var me = this,
		
			rect = me.rect,
			text = me.text;

		rect.attr({
			'fill': me.merge ? FOREGROUND_STATUS_COLOR[me.taskStatus] : STATUS_COLOR[me.taskStatus]
		});

		text.attr({
			'text': CanvasMgr.processText(me.name).text
		});

		Ext.QuickTips.register({
			target: [rect.node, text.node],
			title: '作业信息',
			text: me.getTooltip()
		});
	},
	
	getValue: function() {
		return {
			merge: this.merge,
			taskId: this.taskId,
			jobId: this.jobId,
			jobName: this.jobName,
			name: this.name,
			jobType: this.jobType,
			cycleType: this.cycleType,
			taskDate: this.taskDate,
			taskStatus: this.taskStatus,
			dutyOfficer: this.dutyOfficer,
			taskBeginTime: this.taskBeginTime,
			taskEndTime: this.taskEndTime,
			lastActionId: this.lastActionId
		};
	},
	
	draggable: function() {
		var me = this,
			p = me.paper,
			
			rect = me.rect,
			text = me.text,
			
			lx = 0,
			ly = 0,
			
			up = function() {
				rect.animate({
					'opacity': .7
				}, 500);
			},
		
			start = function(x, y) {
				lx = rect.attr('x');
				ly = rect.attr('y');
				
				rect.animate({
					'opacity': .2
				}, 500);
			},
			
			move = function(dx, dy, x, y) {
				var nx = lx + dx,
					ny = ly + dy;
					
				rect.attr({
					x: nx,
					y: ny
				})
				
				text.attr({
					x: nx + me.w / 2,
					y: ny + me.h / 2
				});
				
				var connectors = CanvasMgr.getConnectors(me);
				Ext.iterate(connectors, function(connector) {
					connector.connection();
				});
				
				p.safari();
			};
		
		rect.drag(move, start, up);
		text.drag(move, start, up);
	}
});

framework.widgets.raphael.TaskConnector = Ext.extend(Ext.util.Observable, {
	
	/**
	 * @type Raphael.Paper 
	 */
	paper: null,
	
	/**
	 * 父任务
	 * @type TaskShape
	 */
	from: null,
	
	/**
	 * 子任务
	 * @type TaskShape
	 */
	to: null,
	
	/**
	 * 关联线
	 * @type Raphael.Element
	 */
	line: null,
	
	/**
	 * 箭头
	 * @type 
	 */
	arrow: null,

	/**
	 * 是否隐藏
	 * @type Boolean
	 */
	hidden: false,
	
	/**
	 * 关联关系是否选中
	 * @type Boolean
	 */
	selected: false,
	
	constructor: function(config) {
		this.id = Ext.id(this, 'task-connector');
		
		framework.widgets.raphael.TaskConnector.superclass.constructor.apply(this, arguments);
		
		this.paper = CanvasMgr.getCanvas();
		this.from = config.from;
		this.to = config.to;

		this.connection();
		
		this.initEvents();
	},
	
	connection: function() {
		var me = this,
		
			from = me.from,
			to = me.to,
		
			bf = from.rect.getBBox(),
			bt = to.rect.getBBox(),
			
			p = [{
				// 父任务上边中心坐标
				x: bf.x + bf.width / 2,
				y: bf.y - 1
			}, {
				// 父任务下边中心坐标
				x: bf.x + bf.width / 2,
				y: bf.y + bf.height + 1
			}, {
				// 父任务左边中心坐标
				x: bf.x - 1,
				y: bf.y + bf.height / 2
			}, {
				// 父任务右边中心坐标
				x: bf.x + bf.width + 1,
				y: bf.y + bf.height / 2
			}, {
				// 子任务上边中心坐标
				x: bt.x + bt.width / 2,
				y: bt.y - 1
			}, {
				// 子任务下边中心坐标
				x: bt.x + bt.width / 2,
				y: bt.y + bt.height + 1
			}, {
				// 子任务左边中心坐标
				x: bt.x - 1,
				y: bt.y + bt.height / 2
			}, {
				// 子任务右边中心坐标
				x: bt.x + bt.width + 1,
				y: bt.y + bt.height / 2
			}],
			
			d = {},
			dis = [];
			
		for (var i = 0; i < 4; i++) {
			for (var j = 4; j < 8; j++) {
				var dx = Math.abs(p[i].x - p[j].x),
					dy = Math.abs(p[i].y - p[j].y);
					
				if ((i == j - 4) || (((i != 3 && j != 6) || p[i].x < p[j].x) && ((i != 2 && j != 7) || p[i].x > p[j].x) && ((i != 0 && j != 5) || p[i].y > p[j].y) && ((i != 1 && j != 4) || p[i].y < p[j].y))) {
	                dis.push(dx + dy);
	                d[dis[dis.length - 1]] = [i, j];
	            }
			}
		}
		
		var res = [0, 4];
		if (dis.length != 0) {
	        res = d[Math.min.apply(Math, dis)];
	    }
	    
	    var x1 = p[res[0]].x,
	        y1 = p[res[0]].y,
	        x4 = p[res[1]].x,
	        y4 = p[res[1]].y;
	    dx = Math.max(Math.abs(x1 - x4) / 2, 10);
	    dy = Math.max(Math.abs(y1 - y4) / 2, 10);
	    var x2 = [x1, x1, x1 - dx, x1 + dx][res[0]].toFixed(3),
	        y2 = [y1 - dy, y1 + dy, y1, y1][res[0]].toFixed(3),
	        x3 = [0, 0, 0, 0, x4, x4, x4 - dx, x4 + dx][res[1]].toFixed(3),
	        y3 = [0, 0, 0, 0, y1 + dy, y1 - dy, y4, y4][res[1]].toFixed(3);

	    // 箭头的角度
	    var fx = Number(x4.toFixed(3)),
	    	fy = Number(y4.toFixed(3)),
	    	
	    	fAngle = 50, len = 10,
			rad = Math.tan(Raphael.rad(fAngle / 2)) * len, 
			lx = 0,
			ly = 0,
			rx = 0,
			ry = 0,
			cx = 0,
			cy = 0,
			alen = 4; // 箭头长度
	    
		// 计算结束点是在对象的哪个边以确定最终的箭头角度
	    for (var i = 4; i < 8; i++) {
	    	var point = p[i];
	    	if (x4 == point.x && y4 == point.y) {
	    		if (i == 4) {
	    			// 上边
	    			lx = fx - rad;
					ly = fy - len;
					rx = fx + rad;
					ry = ly;
					cx = fx;
					cy = ly + alen;
	    		} else if (i == 5) {
	    			// 下边
	    			lx = fx - rad;
					ly = fy + len;
					rx = fx + rad;
					ry = ly;
					cx = fx;
					cy = ly - alen;
	    		} else if (i == 6) {
	    			// 左边
	    			lx = fx - len;
					ly = fy - rad;
					rx = lx;
					ry = fy + rad;
					cx = lx + alen;
					cy = fy;
	    		} else if (i == 7) {
	    			// 右边
	    			lx = fx + len;
					ly = fy - rad;
					rx = lx;
					ry = fy + rad;
					cx = lx - alen;
					cy = fy;
	    		}
	    		
	    		break;
	    	}
	    }
	    
	    var path = ["M", x1.toFixed(3), y1.toFixed(3), "C", x2, y2, x3, y3, fx, fy].join(','),
			arrowPath = ['M', fx, fy, 'L', lx, ly, 'L', cx, cy, 'L', rx, ry, 'L', fx, fy, 'Z'].join(','),
			
			paper = me.paper,
			line = me.line,
	    	arrow = me.arrow;
	    	
	    if (line) {
	    	line.attr({path: path});
	    	arrow.attr({path: arrowPath});
	    	
	    } else {
	    	var tooltip = [];
	    	tooltip.push('<b style="color:#666"><i>父任务</i></b>');
	    	tooltip.push(from.getTooltip());
	    	tooltip.push('<b style="color:#666"><i>子任务</i></b>');
	    	tooltip.push(to.getTooltip());
	    	tooltip = tooltip.join('<br>');
	    	
	    	me.line = line = paper.path(path).attr({
	    		'stroke': TASK_SHAPE_COLOR.RELATION_LINE,
	    		'stroke-width': 1
	    	});
	    	
	    	me.arrow = arrow = paper.path(arrowPath).attr({
	    		'stroke': TASK_SHAPE_COLOR.RELATION_LINE,
	    		'fill': TASK_SHAPE_COLOR.ARROW
	    	});
	    	
	    	Ext.QuickTips.register({
				target: [line.node, arrow.node],
				title: '任务关系',
				text: tooltip,
				dismissDelay: 10000000
			});
	    }
	    
	},
	
	initEvents: function() {
		var me = this,
		
			line = me.line,
			arrow = me.arrow,
			
			onSelect = function() {
				me[me.selected ? 'unselect' : 'select']();
			};
			
		line.hover(me.over.createDelegate(me), me.out.createDelegate(me));
		arrow.hover(me.over.createDelegate(me), me.out.createDelegate(me));
		
		line.click(onSelect);
		arrow.click(onSelect);
	},
	
	over: function() {
		if (this.selected === true) return;
		
		var selectAttr = {
				'stroke': TASK_SHAPE_COLOR.SELECT
			};
		
		var me = this,
			
			line = me.line,
			arrow = me.arrow,
			from = me.from,
			to = me.to;
			
		line.attr(selectAttr);
		
		arrow.attr(Ext.apply({
			'fill': TASK_SHAPE_COLOR.SELECT
		}, selectAttr));
		
		from.over();
		to.over();
	},
	
	out: function() {
		if (this.selected === true) return;
		
		var selectAttr = {
				'stroke': TASK_SHAPE_COLOR.LINE
			};
		
		var me = this,
			
			line = me.line,
			arrow = me.arrow,
			from = me.from,
			to = me.to;
			
		line.attr(selectAttr);
		
		arrow.attr(Ext.apply({
			'fill': TASK_SHAPE_COLOR.ARROW_LINE
		}, selectAttr));
		
		from.out();
		to.out();
	},
	
	select: function() {
		this.over();
		this.from.select(true);
		this.to.select(true);
		this.selected = true;
	},
	
	unselect: function() {
		this.selected = false;
		this.from.unselect(true);
		this.to.unselect(true);
		this.out();
	}/*,
	
	hide: function() {
		if (this.hidden === true) return;
		
		var me = this;
			
		me.line.hide();
		me.arrow.hide();
		
		me.hidden = true;
	},
	
	show: function() {
		if (this.hidden === false) return;
		
		var me = this;
		
		me.line.show();
		me.arrow.show();
		
		me.hidden = false;
	}*/
});

TASK_SHAPE_SIZE = {
	WIDTH: 150,
	HEIGHT: 40
};

TASK_SHAPE_COLOR = {
	LINE: '#999',
	MERGE_LINE: 'blue',
	RELATION_LINE: '#aaa',
	TEXT: '#fff',
	SELECT: '#5292fc',
	ARROW: '#eee',
	
	NOT_RUNNING: '#bbb',
	RUNNING: 'orange',
	RUN_SUCCESS: '#76ae00',
	RUN_FAILURE: 'red'
};

STATUS_COLOR = {
	0: TASK_SHAPE_COLOR.NOT_RUNNING,
	1: TASK_SHAPE_COLOR.NOT_RUNNING,
	2: TASK_SHAPE_COLOR.NOT_RUNNING,
	3: TASK_SHAPE_COLOR.RUNNING,
	4: TASK_SHAPE_COLOR.RUN_FAILURE,
	5: TASK_SHAPE_COLOR.RUN_SUCCESS,
	6: TASK_SHAPE_COLOR.NOT_RUNNING,
	7: TASK_SHAPE_COLOR.NOT_RUNNING,
	8: TASK_SHAPE_COLOR.NOT_RUNNING,
	9: TASK_SHAPE_COLOR.RUNNING,
	10: TASK_SHAPE_COLOR.RUN_FAILURE,
	11: TASK_SHAPE_COLOR.RUN_SUCCESS
};

FOREGROUND_STATUS_COLOR = {
	0: TASK_SHAPE_COLOR.NOT_RUNNING,
	1: TASK_SHAPE_COLOR.RUNNING,
	2: TASK_SHAPE_COLOR.RUN_FAILURE,
	3: TASK_SHAPE_COLOR.RUN_SUCCESS
};

(function(R) {
	R.fn.task = function(config) {
		return new framework.widgets.raphael.TaskShape(Ext.apply({paper: this}, config));
	};
	
	var canvas = null,
		tasks = {},
		connectors = {},
		parentConnectors = {},
		childrenConnectors = {};
	
	CanvasMgr = {};
	Ext.apply(CanvasMgr, {
		init: function(c) {
			canvas = c;
		},
		
		getCanvas: function() {
			return canvas;
		},
		
		registeTask: function(task) {
			tasks[task.id] = task;
		},
		
		getTask: function(id) {
			return tasks[id];
		},
		
		hasTask: function(id) {
			return tasks[id] != null;
		},
		
		registeConnector: function(connector) {
			var from = connector.from, 
				to = connector.to;

			var children = childrenConnectors[from.id] || [];
			children.push(connector);
			childrenConnectors[from.id] = children;
			
			var parents = parentConnectors[to.id] || [];
			parents.push(connector);
			parentConnectors[to.id] = parents;
				
			connectors[from.id + '|' + to.id] = connector;
		},
		
		getParentConnectors: function(task) {
			return parentConnectors[task.id] || [];
		},
		
		getChildrenConnectors: function(task) {
			return childrenConnectors[task.id] || [];
		},
		
		getConnectors: function(task) {
			return this.getParentConnectors(task).concat(this.getChildrenConnectors(task)) || [];
		},
		
		getConnector: function(key) {
			return connectors[key];
		},
		
		hasConnector: function(key) {
			return connectors[key] != null;
		},
		
		clear: function() {
			canvas.clear();
			
			tasks = {};
			connectors = {};
		},
		
		/**
		 * 处理指定文本
		 * @param {String} text
		 */
		processText: function(text) {
			var newText = '',
				width = TASK_SHAPE_SIZE.WIDTH,
				height = TASK_SHAPE_SIZE.HEIGHT;
				
			if (!Ext.isEmpty(text, false)) {
				var len = text.len(),
					
					newText = '',
					width = TASK_SHAPE_SIZE.WIDTH,
					height = TASK_SHAPE_SIZE.HEIGHT,
					
					lines = parseInt(len % 20 == 0 ? len / 20 : len / 20 + 1),
					pos = {};
					
				if (lines > 1) {
					lines = 2;
					for (var i = 0; i < lines; i++) {
						pos[i * 20 + 20] = true;
					}
					
					var p = 0;
					for (var i = 0; i < text.length; i++) {
						var c = text.substr(i, 1);
						p += c.len();
						
						if (pos[p] === true) {
							c += '\n';
						}
						
						if (p >= 36) {
							newText += '...';
							break;
						} else {
							newText += c;
						}
					}
				} else {
					newText = text;
				}
			}
	
			return {
				text: newText,
				width: width,
				height: height
			};
		}
	});
})(Raphael);