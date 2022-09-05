Ext.override(Ext.DatePicker, {
	/**
	 * @cfg 是否显示时间
	 * @type Boolean
	 */
	showTime: false,
	
	/**
	 * @cfg disabledTime
	 * @type {Boolean}
	 * @description 是否禁用时间的修改
	 */
	disabledTime: false,
	
	initComponent : function(){
        Ext.DatePicker.superclass.initComponent.call(this);

        // =================== add by hsm =================== //
        // this.value = this.value ? this.value.clearTime() : new Date().clearTime();
        this.value = this.value ? this.value : new Date();
		this.format = this.showTime ? Ext.isEmpty(this.format, false) ? 'Y-n-j G:i:s' : this.format : 'Y-n-j';
        // =================== add by hsm =================== //
        
        this.addEvents(
            /**
             * @event select
             * Fires when a date is selected
             * @param {DatePicker} this
             * @param {Date} date The selected date
             */
            'select'
        );

        if(this.handler){
            this.on('select', this.handler,  this.scope || this);
        }

        this.initDisabledDays();
    },
    
    /**
     * Sets the value of the date field
     * @param {Date} value The date to set
     */
    setValue : function(value){
        var old = this.value;
        
        // =================== add by hsm =================== //
        // this.value = value.clearTime(true);
        this.value = value;
        // =================== add by hsm =================== //
        if(this.el){
            this.update(this.value);
        }
    },

    /**
     * Gets the current selected value of the date field
     * @return {Date} The selected date
     */
    getValue : function() {
    	// =================== add by hsm =================== //
    	// return this.value;
        return this.showTime ? this.value : this.value.clearTime();
        // =================== add by hsm =================== //
    },
    
    // private
    selectToday : function(){
        if(this.todayBtn && !this.todayBtn.disabled){
        	// =================== add by hsm =================== //
            // this.setValue(new Date().clearTime());
        	this.setValue(new Date());
        	// =================== add by hsm =================== //
            this.fireEvent('select', this, this.value);
        }
    },
    
    // private
    onRender : function(container, position){
        var m = [
             '<table cellspacing="0">',
                '<tr><td class="x-date-left"><a href="#" title="', this.prevText ,'">&#160;</a></td><td class="x-date-middle" align="center"></td><td class="x-date-right"><a href="#" title="', this.nextText ,'">&#160;</a></td></tr>',
                '<tr><td colspan="3"><table class="x-date-inner" cellspacing="0"><thead><tr>'],
                dn = this.dayNames,
                i;
        for(i = 0; i < 7; i++){
            var d = this.startDay+i;
            if(d > 6){
                d = d-7;
            }
            m.push('<th><span>', dn[d].substr(0,1), '</span></th>');
        }
        m[m.length] = '</tr></thead><tbody><tr>';
        for(i = 0; i < 42; i++) {
            if(i % 7 === 0 && i !== 0){
                m[m.length] = '</tr><tr>';
            }
            m[m.length] = '<td><a href="#" hidefocus="on" class="x-date-date" tabIndex="1"><em><span></span></em></a></td>';
        }
        
        // =================== add by hsm =================== //
        /*m.push('</tr></tbody></table></td></tr>',
                this.showToday ? '<tr><td colspan="3" class="x-date-bottom" align="center"></td></tr>' : '',
                '</table><div class="x-date-mp"></div>');*/
        m.push('</tr></tbody></table></td></tr>',
                this.showToday 
                	? this.showTime 
                		? '<tr><td colspan="3" align="center"><table class="x-date-table" cellspacing="0"><tr><td class="x-date-hour" align="center">时</td><td class="x-date-minute" align="center">分</td><td class="x-date-second" align="center">秒</td></tr></table></td></tr><tr><td colspan="3" class="x-date-bottom" align="center"></td></tr>' 
                		: '<tr><td colspan="3" class="x-date-bottom" align="center"></td></tr>'
                	: '',
                '</table><div class="x-date-mp"></div>');
		// =================== add by hsm =================== //

        var el = document.createElement('div');
        el.className = 'x-date-picker';
        el.innerHTML = m.join('');

        container.dom.insertBefore(el, position);

        this.el = Ext.get(el);
        this.eventEl = Ext.get(el.firstChild);

        this.prevRepeater = new Ext.util.ClickRepeater(this.el.child('td.x-date-left a'), {
            handler: this.showPrevMonth,
            scope: this,
            preventDefault:true,
            stopDefault:true
        });

        this.nextRepeater = new Ext.util.ClickRepeater(this.el.child('td.x-date-right a'), {
            handler: this.showNextMonth,
            scope: this,
            preventDefault:true,
            stopDefault:true
        });

        this.monthPicker = this.el.down('div.x-date-mp');
        this.monthPicker.enableDisplayMode('block');

        this.keyNav = new Ext.KeyNav(this.eventEl, {
            'left' : function(e){
                if(e.ctrlKey){
                    this.showPrevMonth();
                }else{
                    this.update(this.activeDate.add('d', -1));    
                }
            },

            'right' : function(e){
                if(e.ctrlKey){
                    this.showNextMonth();
                }else{
                    this.update(this.activeDate.add('d', 1));    
                }
            },

            'up' : function(e){
                if(e.ctrlKey){
                    this.showNextYear();
                }else{
                    this.update(this.activeDate.add('d', -7));
                }
            },

            'down' : function(e){
                if(e.ctrlKey){
                    this.showPrevYear();
                }else{
                    this.update(this.activeDate.add('d', 7));
                }
            },

            'pageUp' : function(e){
                this.showNextMonth();
            },

            'pageDown' : function(e){
                this.showPrevMonth();
            },

            'enter' : function(e){
                e.stopPropagation();
                return true;
            },

            scope : this
        });

        this.el.unselectable();

        this.cells = this.el.select('table.x-date-inner tbody td');
        this.textNodes = this.el.query('table.x-date-inner tbody span');

        this.mbtn = new Ext.Button({
            text: '&#160;',
            tooltip: this.monthYearText,
            renderTo: this.el.child('td.x-date-middle', true )
        });
        this.mbtn.el.child('em').addClass('x-btn-arrow');

        if(this.showToday){
            this.todayKeyListener = this.eventEl.addKeyListener(Ext.EventObject.SPACE, this.selectToday,  this);
            var today = (new Date()).dateFormat(this.format);
            this.todayBtn = new Ext.Button({
                renderTo: this.el.child('td.x-date-bottom', true),
                text: String.format(this.todayText, today),
                tooltip: String.format(this.todayTip, today),
                handler: this.selectToday,
                scope: this
            });
        }
        
        // =================== add by hsm =================== //
        if (this.showTime) {
        	this.hourBtn = new Ext.form.SpinnerField({
        		renderTo: this.el.child('td.x-date-hour'),
        		minValue: 0,
        		maxValue: 23,
        		width: 50,
        		disabled: this.disabledTime
        	});
        	
        	this.minuteBtn = new Ext.form.SpinnerField({
        		renderTo: this.el.child('td.x-date-minute'),
        		minValue: 0,
        		maxValue: 59,
        		width: 50,
        		disabled: this.disabledTime
        	});
        	
        	this.secondBtn = new Ext.form.SpinnerField({
        		renderTo: this.el.child('td.x-date-second'),
        		minValue: 0,
        		maxValue: 59,
        		width: 50,
        		disabled: this.disabledTime
        	});
        }
        // =================== add by hsm =================== //
        
        this.mon(this.eventEl, 'mousewheel', this.handleMouseWheel, this);
        this.mon(this.eventEl, 'click', this.handleDateClick,  this, {delegate: 'a.x-date-date'});
        this.mon(this.mbtn, 'click', this.showMonthPicker, this);
        this.onEnable(true);
    },
    
    // private
    update : function(date, forceRefresh){
        var vd = this.activeDate, vis = this.isVisible();
        this.activeDate = date;
        if(!forceRefresh && vd && this.el){
        	// =================== add by hsm =================== //
            // var t = date.getTime();
        	var t = date.clearTime(true).getTime();
           	// =================== add by hsm =================== //
            if(vd.getMonth() == date.getMonth() && vd.getFullYear() == date.getFullYear()){
                this.cells.removeClass('x-date-selected');
                this.cells.each(function(c){
                   if(c.dom.firstChild.dateValue == t){
                       c.addClass('x-date-selected');
                       if(vis){
                           Ext.fly(c.dom.firstChild).focus(50);
                       }
                       return false;
                   }
                });
                return;
            }
        }
        var days = date.getDaysInMonth();
        var firstOfMonth = date.getFirstDateOfMonth();
        var startingPos = firstOfMonth.getDay()-this.startDay;
        
        // =================== add by hsm =================== //
        if (this.showTime) {
        	this.hourBtn.setValue(date.getHours());
        	this.minuteBtn.setValue(date.getMinutes());
        	this.secondBtn.setValue(date.getSeconds());
        }
		// =================== add by hsm =================== //
        
        if(startingPos <= this.startDay){
            startingPos += 7;
        }

        var pm = date.add('mo', -1);
        var prevStart = pm.getDaysInMonth()-startingPos;

        var cells = this.cells.elements;
        var textEls = this.textNodes;
        days += startingPos;

        // convert everything to numbers so it's fast
        var day = 86400000;
        var d = (new Date(pm.getFullYear(), pm.getMonth(), prevStart)).clearTime();
        var today = new Date().clearTime().getTime();
        var sel = date.clearTime().getTime();
        // =================== add by hsm =================== //
        var min = this.minDate ? this.minDate.clearTime() : Number.NEGATIVE_INFINITY;
        var max = this.maxDate ? this.maxDate.clearTime() : Number.POSITIVE_INFINITY;

        /*var min = this.minDate ? this.minDate : Number.NEGATIVE_INFINITY;
        var max = this.maxDate ? this.maxDate : Number.POSITIVE_INFINITY;*/
        // =================== add by hsm =================== //
        var ddMatch = this.disabledDatesRE;
        var ddText = this.disabledDatesText;
        var ddays = this.disabledDays ? this.disabledDays.join('') : false;
        var ddaysText = this.disabledDaysText;
        var format = this.format;

        if(this.showToday){
        	// =================== add by hsm =================== //
            // var td = new Date().clearTime();
            var td = new Date();
            // =================== add by hsm =================== //
            var disable = (td < min || td > max ||
                (ddMatch && format && ddMatch.test(td.dateFormat(format))) ||
                (ddays && ddays.indexOf(td.getDay()) != -1));

            if(!this.disabled){
                this.todayBtn.setDisabled(disable);
                this.todayKeyListener[disable ? 'disable' : 'enable']();
            }
        }

        var setCellClass = function(cal, cell){
            cell.title = '';
            var t = d.getTime();
            cell.firstChild.dateValue = t;
            if(t == today){
                cell.className += ' x-date-today';
                cell.title = cal.todayText;
            }
            if(t == sel){
                cell.className += ' x-date-selected';
                if(vis){
                    Ext.fly(cell.firstChild).focus(50);
                }
            }
            // disabling
            if(t < min) {
                cell.className = ' x-date-disabled';
                cell.title = cal.minText;
                return;
            }
            if(t > max) {
                cell.className = ' x-date-disabled';
                cell.title = cal.maxText;
                return;
            }
            if(ddays){
                if(ddays.indexOf(d.getDay()) != -1){
                    cell.title = ddaysText;
                    cell.className = ' x-date-disabled';
                }
            }
            if(ddMatch && format){
                var fvalue = d.dateFormat(format);
                if(ddMatch.test(fvalue)){
                    cell.title = ddText.replace('%0', fvalue);
                    cell.className = ' x-date-disabled';
                }
            }
        };

        var i = 0;
        for(; i < startingPos; i++) {
            textEls[i].innerHTML = (++prevStart);
            d.setDate(d.getDate()+1);
            cells[i].className = 'x-date-prevday';
            setCellClass(this, cells[i]);
        }
        for(; i < days; i++){
            var intDay = i - startingPos + 1;
            textEls[i].innerHTML = (intDay);
            d.setDate(d.getDate()+1);
            cells[i].className = 'x-date-active';
            setCellClass(this, cells[i]);
        }
        var extraDays = 0;
        for(; i < 42; i++) {
             textEls[i].innerHTML = (++extraDays);
             d.setDate(d.getDate()+1);
             cells[i].className = 'x-date-nextday';
             setCellClass(this, cells[i]);
        }

        this.mbtn.setText(this.monthNames[date.getMonth()] + ' ' + date.getFullYear());
        
        if(!this.internalRender){
            var main = this.el.dom.firstChild;
            var w = main.offsetWidth;
            this.el.setWidth(w + this.el.getBorderWidth('lr'));
            Ext.fly(main).setWidth(w);
            this.internalRender = true;
            // opera does not respect the auto grow header center column
            // then, after it gets a width opera refuses to recalculate
            // without a second pass
            if(Ext.isOpera && !this.secondPass){
                main.rows[0].cells[1].style.width = (w - (main.rows[0].cells[0].offsetWidth+main.rows[0].cells[2].offsetWidth)) + 'px';
                this.secondPass = true;
                this.update.defer(10, this, [date]);
            }
        }
    },
    
    handleDateClick : function(e, t){
        e.stopEvent();
        if(!this.disabled && t.dateValue && !Ext.fly(t.parentNode).hasClass('x-date-disabled')){
        	// =================== add by hsm =================== //
            // this.setValue(new Date(t.dateValue));
        	var d = new Date(t.dateValue);
        	if (this.showTime) {
        		d.setHours(this.hourBtn.getValue());
        		d.setMinutes(this.minuteBtn.getValue());
        		d.setSeconds(this.secondBtn.getValue());
        	}
        	this.setValue(d);
            // =================== add by hsm =================== //
            this.fireEvent('select', this, this.value);
        }
    }
});