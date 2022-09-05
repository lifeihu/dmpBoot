package org.springframework.ui;

import java.util.Date;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.springframework.util.StringUtils;

public class ConditionExpression {
	public static final String EQ = "eq"; // 等于
	public static final String GT = "gt"; // 大于
	public static final String GE = "ge"; // 大于等于
	public static final String LT = "lt"; // 小于
	public static final String LE = "le"; // 小于等于
	public static final String LK = "lk"; // 模糊匹配
	public static final String LKS = "lks"; // 模糊匹配
	public static final String LKE = "lke"; // 模糊匹配
	public static final String IN = "in"; // IN
	public static final String NIN = "nn"; // NOTIN
	public static final String NEQ = "nq"; // 不等于
	public static final String NULL = "nul"; // ISNULL
	public static final String NOT_NULL = "nnl"; // NOTNULL

	public static final String START_MATCH = "startMatch";
	public static final String END_MATCH = "endMatch";
	public static final String ANY_MATCH = "anyMatch";

	private String column;
	private String op;
	private String matchMode;
	private Object value;

	public ConditionExpression(String column, String op, Object value) {
		this.column = column;
		this.op = op;
		this.value = value;
	}

	public ConditionExpression(String column, String op, Object value, String matchMode) {
		this.column = column;
		this.op = op;
		this.matchMode = matchMode;
		this.value = value;
	}

	public String getColumn() {
		return column;
	}

	public String getOp() {
		return op;
	}

	public Object getValue() {
		return value;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public void setOp(String op) {
		this.op = op;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getMatchMode() {
		return matchMode;
	}

	public void setMatchMode(String matchMode) {
		this.matchMode = matchMode;
	}

	public boolean isEmpty() {
		return !StringUtils.hasText(this.column) || this.value == null;
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> valueClazz) {
		if (value == null || (value instanceof String && !StringUtils.hasText((String) value)))
			return null;

		DateConverter converter = new DateConverter();
		converter.setPatterns(new String[] { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss" });
		ConvertUtils.register(converter, Date.class);

		return (T) ConvertUtils.convert(value, valueClazz);
	}
}
