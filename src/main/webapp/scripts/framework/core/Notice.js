_package('framework.core');

framework.core.Notice = function() {
	Ext.onReady(this.onReady, this);
	
	framework.core.Notice.superclass.constructor.apply(this, arguments);
};

Ext.extend(framework.core.Notice, Ext.util.Observable, {
	
	STATUS_EXCEPTION :          'exception',
    STATUS_ERROR:               'error',
    STATUS_NOTICE:              'notice',
    STATUS_WARNING:             'warning',
    STATUS_HELP:                'help',
    
    STATUS_DESC: {
    	'exception': '异常',
    	'error': '错误',
    	'warning': '警告',
    	'notice': '信息',
    	'help': '帮助'
    },
    
    /**
     * @private
     * @type 
     * @description ref to message-box Element
     */
    msgCt : null,
    
    onReady : function() {
    	// create the msgBox container
        this.msgCt = Ext.DomHelper.insertFirst(document.body, {id:'msg-div'}, true);
        this.msgCt.setStyle('position', 'absolute');
        this.msgCt.setStyle('z-index', 9999);
        this.msgCt.setWidth(400);
    },
    
    info: function(o) {
    	this.addMessage(this.STATUS_NOTICE, o);
    },
    
    warn: function(o) {
    	this.addMessage(this.STATUS_WARNING, o);
    },
    
    error: function(o) {
    	this.addMessage(this.STATUS_ERROR, o);
    },
    
    exceptoin: function(o) {
    	this.addMessage(this.STATUS_EXCEPTION, o);
    },
    
    help: function(o) {
    	this.addMessage(this.STATUS_HELP, o);
    },
    
    /**
     * 添加消息至队列
     * @param {String} status
     * @param {Object} o
     */
    addMessage: function(status, o) {
    	// 消息内容
    	if (Ext.isString(o)) {
    		o = {msg: o};
    	}
    	var msg = o.msg;
    	
    	if (Ext.isEmpty(msg, false)) {
    		return;
    	}
    	
    	// 消息状态
    	status = o.status || status;
    	
    	// 消息停留时间(秒)
    	var delay = o.delay;
    	if (!Ext.isNumber(delay)) {
	    	delay = 3;    // <-- default delay of msg box is 1 second.
	        if (status == false) {
	            delay = 5;    // <-- when status is error, msg box delay is 3 seconds.
	        }
	        // add some smarts to msg's duration (div by 13.3 between 3 & 9 seconds)
	        delay = msg.length / 13.3;
	        if (delay < 3) {
	            delay = 3;
	        }
	        else if (delay > 11) {
	            delay = 11;
	        }
    	}

    	// 回调
    	var callback = o.callback, // 回调方法
    		scope = o.scope, // 回调方法的作用域
    		callbackDelay = o.callbackDelay || 300, // 回调方法被调起的延迟秒数
    	
    		slideConfig = { easing: 'bounceOut' };

    	if (Ext.isFunction(callback)) {
    		slideConfig.callback = function() {
    			callback.defer(callbackDelay, scope);
    		};
    	}
    		
        this.msgCt.alignTo(document, 't-t');
        var noticeEl = Ext.DomHelper.append(this.msgCt, {
        	html: this.buildMessageBox(status, msg/*String.format.apply(String, Array.prototype.slice.call(arguments, 1))*/)
        }, true);
        
        noticeEl.child('.x-box-mc .x-tool-close').on({
        	click: function() {
        		noticeEl.ghost('t', {
        			remove: true,
        			stopFx: true
        		})
        	},
        	single: true
        });
        
        noticeEl.slideIn('t', slideConfig)
	        .pause(delay)
	        .ghost("t", {remove:true});
    },
    
    /***
     * buildMessageBox
     */
    buildMessageBox : function(title, msg) {
        switch (title) {
            case true:
                title = this.STATUS_NOTICE;
                break;
            case false:
                title = this.STATUS_ERROR;
                break;
        }
        
        return [
            '<div class="x-box-blue notice-msg">',
            '<div class="x-box-tl"><div class="x-box-tr"><div class="x-box-tc"></div></div></div>',
            '<div class="x-box-ml"><div class="x-box-mr"><div class="x-box-mc">',
            	'<div class="x-tool x-tool-close">&nbsp;</div>',
            	'<h3 class="x-notice-icon icon-status-' + title + '">', this.STATUS_DESC[title], '</h3>', 
            	'<div class="x-notice-msg">', msg, '</div>', 
            '</div></div></div>',
            '<div class="x-box-bl"><div class="x-box-br"><div class="x-box-bc"></div></div></div>',
            '</div>'
        ].join('');
    },

    /**
     * decodeStatusIcon
     * @param {Object} status
     */
    decodeStatusIcon : function(status) {
        iconCls = '';
        switch (status) {
            case true:
            case this.STATUS_OK:
                iconCls = this.ICON_OK;
                break;
            case this.STATUS_NOTICE:
                iconCls = this.ICON_NOTICE;
                break;
            case false:
            case this.STATUS_ERROR:
                iconCls = this.ICON_ERROR;
                break;
            case this.STATUS_HELP:
                iconCls = this.ICON_HELP;
                break;
        }
        return iconCls;
    }
	
});

Notice = new framework.core.Notice();