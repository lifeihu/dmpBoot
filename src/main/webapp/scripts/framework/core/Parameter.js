_package('framework.core');

framework.core.Parameter = Ext.extend(Ext.util.Observable, {
	
	parameters: null,
	
	constructor: function(win) {
		win = win || window;
		var location = win.location,
			search = location.search;
		if (search.charAt(0) == '?')
			search = search.substring(1);

		this.parameters = Ext.isEmpty(search, false) ? {} : Ext.urlDecode(search);
	},
	
	/**
	 * 获得字符串
	 * @param {String} key
	 * @return {String}
	 */
	get: function(key) {
		var value = this.parameters[key];
		
		if (value == undefined)
			return null;
		
		if (typeof value == 'object')
			return value;

		return Ext.isEmpty(String(value), false) || String(value) == 'null' ? null : String(value);
	},
	
	/**
	 * 获得数值
	 * @param {String} key
	 * @return {Number}
	 */
	getNumber: function(key) {
		var value = this.parameters[key];
		if (typeof value == 'object')
			return value;
			
		try {
			value = Number(value);
		} catch(e) {
			value = null;
		}
		
		return value;
	},
	
	/**
	 * 获得日期
	 * @param {String} key
	 * @param {String} pattern
	 * @return {Date}
	 */
	getDate: function(key, pattern) {
		var value = this.parameters[key];
		if (value instanceof Date)
			return value;
		
		if (typeof value == 'string') {
			pattern = pattern || 'Y-n-j';
			value = Date.parseDate(value, pattern);
			return value;
		}
		
		return null;
	}, 
	
	/**
	 * 获得日期时间
	 * @param {String} key
	 * @param {String} pattern
	 * @return {Date}
	 */
	getDatetime: function(key, pattern) {
		pattern = pattern || 'Y-n-j G:i:s';
		return this.getDate(key, pattern);
	},
	
	/**
	 * 获得布尔
	 * @param {String} key
	 */
	getBoolean: function(key) {
		var value = this.get(key);
		if (value == 'true' || value == '1' || value == 'yes')
			return true;
		else 
			return false;
	}
});