_package('framework.widgets.form');

_import([
	'framework.widgets.ux.Spinner',
	'framework.widgets.ux.SpinnerField',
	
	'framework.widgets.form.DatePicker'
]);

framework.widgets.form.DateTimeField = Ext.extend(Ext.form.DateField, {
	format: 'Y-m-d H:i:s',
	
	/**
	 * @cfg disabledTime
	 * @type {Boolean}
	 * @description 是否禁用时间的修改
	 */
	disabledTime: false,
	
	onTriggerClick : function(){
        if(this.disabled){
            return;
        }
        if(this.menu == null){
            this.menu = new Ext.menu.DateMenu({
                hideOnClick: false,
                // ======== add by hsm ======= //
                showTime: true,
                disabledTime: this.disabledTime
                // ======== add by hsm ======= //
            });
        }
        this.onFocus();
        Ext.apply(this.menu.picker,  {
            minDate : this.minValue,
            maxDate : this.maxValue,
            disabledDatesRE : this.disabledDatesRE,
            disabledDatesText : this.disabledDatesText,
            disabledDays : this.disabledDays,
            disabledDaysText : this.disabledDaysText,
            format : this.format,
            showToday : this.showToday,
            minText : String.format(this.minText, this.formatDate(this.minValue)),
            maxText : String.format(this.maxText, this.formatDate(this.maxValue))
        });
        this.menu.picker.setValue(this.getValue() || new Date());
        this.menu.show(this.el, "tl-bl?");
        this.menuEvents('on');
    },
    
    setValue : function(date){
    	return Ext.form.DateField.superclass.setValue.call(this, this.formatDate(this.parseDate(date)));
    }
});

Ext.override(Ext.menu.DateMenu, {
	initComponent: function(){
        this.on('beforeshow', this.onBeforeShow, this);
        if(this.strict = (Ext.isIE7 && Ext.isStrict)){
            this.on('show', this.onShow, this, {single: true, delay: 20});
        }
        Ext.apply(this, {
            plain: true,
            showSeparator: false,
            items: this.picker = new Ext.DatePicker(Ext.apply({
                internalRender: this.strict || !Ext.isIE,
                ctCls: 'x-menu-date-item'
            }, this.initialConfig))
        });
        this.picker.purgeListeners();
        Ext.menu.DateMenu.superclass.initComponent.call(this);
        this.relayEvents(this.picker, ["select"]);
        this.on('select', this.menuHide, this);
        if(this.handler){
            this.on('select', this.handler, this.scope || this);
        }
    }
});

Ext.reg('datetimefield', framework.widgets.form.DateTimeField);