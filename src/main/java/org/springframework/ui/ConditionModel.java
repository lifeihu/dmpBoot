package org.springframework.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.CollectionUtils;
import org.springframework.util.JsonUtil;
import org.springframework.util.StringUtils;

public class ConditionModel {
	private static final String separator = "-";

	private Map<String, String> jsonMap = null;
	private Map<String, String> hashAlias = new HashMap<String, String>();
	private Map<String, Collection<ConditionExpression>> hashCondition = new HashMap<String, Collection<ConditionExpression>>();
	private Map<String, OrderBy> hashOrderBy = new LinkedHashMap<String, OrderBy>();

	private int start = 0;
	private int limit = 15;

	public ConditionModel() {}

	public ConditionModel(Map<String, String> jsonMap) {
		this.jsonMap = jsonMap;
		this.generate();
	}

	public ConditionModel(Map<String, String> jsonMap, Integer start, Integer limit) {
		this.jsonMap = jsonMap;
		this.start = start == null ? 0 : start;
		this.limit = limit == null ? 15 : limit;

		this.generate();
	}

	public ConditionModel(String json) {
		if (JsonUtil.isBlank(json))
			return;

		this.jsonMap = JsonUtil.decode(json, Map.class, String.class, String.class);
		this.generate();
	}

	public ConditionModel(String json, Integer start, Integer limit) {
		this.start = start == null ? 0 : start;
		this.limit = limit == null ? 15 : limit;

		this.jsonMap = JsonUtil.decode(json, Map.class, String.class, String.class);
		this.generate();
	}

	public ConditionModel(Integer start, Integer limit) {
		this.start = start == null ? 0 : start;
		this.limit = limit == null ? 15 : limit;
	}

	private void generate() {
		Map<String, String> map = this.jsonMap;
		if (CollectionUtils.isEmpty(map))
			return;

		String column = null, op = null, v = null, matchMode = ConditionExpression.ANY_MATCH;
		for (String key : map.keySet()) {
			v = map.get(key);

			if (key.endsWith(ConditionExpression.EQ)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.EQ;
			} else if (key.endsWith(ConditionExpression.NEQ)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.NEQ;
			} else if (key.endsWith(ConditionExpression.GT)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.GT;
			} else if (key.endsWith(ConditionExpression.GE)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.GE;
			} else if (key.endsWith(ConditionExpression.LT)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.LT;
			} else if (key.endsWith(ConditionExpression.LE)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.LE;
			} else if (key.endsWith(ConditionExpression.LK)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.LK;
			} else if (key.endsWith(ConditionExpression.LKS)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.LK;
				matchMode = ConditionExpression.START_MATCH;
			} else if (key.endsWith(ConditionExpression.LKE)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.LK;
				matchMode = ConditionExpression.END_MATCH;
			} else if (key.endsWith(ConditionExpression.IN)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.IN;
			} else if (key.endsWith(ConditionExpression.NIN)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.NIN;
			} else if (key.endsWith(ConditionExpression.NULL)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.NULL;
			} else if (key.endsWith(ConditionExpression.NOT_NULL)) {
				column = key.substring(0, key.lastIndexOf(ConditionModel.separator));
				op = ConditionExpression.NOT_NULL;
			} else {
				column = key;
				op = ConditionExpression.LK;
			}

			this.addCondition(column, op, v, matchMode);
		}
	}

	/**
	 * 添加别名
	 * 
	 * @param column
	 */
	public void addAlias(String column) {
		if (!StringUtils.hasText(column) || hashAlias.get(column) != null)
			return;

		if (column.indexOf(".") == -1)
			return;

		String alias = column.substring(0, column.lastIndexOf("."));
		int pos = alias.indexOf(".");
		if (pos > -1) {
			String[] aliases = alias.split("\\.");

			String lastAlias = null;
			for (int i = 0; i < aliases.length; i++) {
				String a = aliases[i];

				if (lastAlias != null)
					a = lastAlias + "." + a;

				if (!this.hashAlias.containsKey(a))
					this.hashAlias.put(a, a.replaceAll("\\.", "_"));

				lastAlias = a;
			}
		} else {
			if (!this.hashAlias.containsKey(alias))
				this.hashAlias.put(alias, alias.replaceAll("\\.", "_"));
		}
	}

	public void removeAlias(String column) {
		if (StringUtils.hasText(column))
			return;

		if (column.indexOf(".") == -1) {
			return;
		}

		String alias = column.substring(0, column.lastIndexOf("."));
		int pos = alias.indexOf(".");
		if (pos > -1) {
			String[] aliases = alias.split("\\.");

			String lastAlias = null;
			for (int i = 0; i < aliases.length; i++) {
				String a = aliases[i];

				if (lastAlias != null)
					a = lastAlias + "." + a;

				if (this.hashAlias.containsKey(a))
					this.hashAlias.remove(a);

				lastAlias = a;
			}
		} else {
			if (this.hashAlias.containsKey(alias)) {
				this.hashAlias.remove(alias);
			}
		}
	}

	public int getAliasSize() {
		return this.hashAlias.size();
	}

	public boolean containsAlias(String alias) {
		return this.hashAlias.containsKey(alias);
	}

	public String getAliasValue(String alias) {
		String value = this.hashAlias.get(alias);
		if (!StringUtils.hasText(value))
			value = alias;

		return alias;
	}

	public Set<String> getAliasKey() {
		return this.hashAlias.keySet();
	}

	public Collection<String> getAliasValues() {
		return this.hashAlias.values();
	}

	public ConditionExpression addCondition(String column, String op, Object value) {
		return this.addCondition(column, op, value, ConditionExpression.ANY_MATCH);
	}

	public ConditionExpression addCondition(String column, String op, Object value, String matchMode) {
		this.addAlias(column);
		ConditionExpression condition = new ConditionExpression(column, op, value, matchMode);

		Collection<ConditionExpression> conditions = this.hashCondition.get(column);
		if (conditions == null) {
			conditions = new ArrayList<ConditionExpression>();
		}
		conditions.add(condition);

		this.hashCondition.put(column, conditions);

		return condition;
	}

	public void removeCondition(String column) {
		this.removeAlias(column);
		this.hashCondition.remove(column);
	}

	public int getConditionSize() {
		return this.hashCondition.size();
	}

	public ConditionExpression getCondition(String column) {
		Collection<ConditionExpression> conditions = this.getConditions(column);

		if (conditions != null && conditions.size() > 0) {
			return conditions.iterator().next();
		}

		return null;
	}

	public Collection<ConditionExpression> getConditions(String column) {
		if (!StringUtils.hasText(column) || this.hashCondition == null || this.hashCondition.size() == 0)
			return null;

		return this.hashCondition.get(column);
	}

	public Set<String> getColumns() {
		return this.hashCondition.keySet();
	}

	public void addOrder(String column, String direction) {
		if (StringUtils.hasText(column)) {
			hashOrderBy.put(column, new OrderBy(column, direction));
		}
	}

	public boolean hasOrderByColumn(String column) {
		return hashOrderBy.containsKey(column);
	}

	public OrderBy getOrderBy(String column) {
		return hashOrderBy.get(column);
	}

	public Collection<OrderBy> getOrderBys() {
		return hashOrderBy.values();
	}

	public int getOrderByCount() {
		return hashOrderBy.size();
	}

	public String toOrderByString() {
		String result = "";

		for (OrderBy orderBy : hashOrderBy.values()) {
			result = "," + orderBy.toString();
		}

		if (StringUtils.hasText(result)) {
			result = result.substring(1);
		}

		return result;
	}

	public String toOrderBySqlString() {
		String result = "";

		for (OrderBy orderBy : hashOrderBy.values()) {
			result = "," + orderBy.toSqlString();
		}

		if (StringUtils.hasText(result)) {
			result = result.substring(1);
		}

		return result;
	}

	public <T> T getValue(String column, Class<T> valueClazz) {
		ConditionExpression condition = this.getCondition(column);
		if (condition == null)
			return null;

		return condition.getValue(valueClazz);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (String column : getColumns()) {
			ConditionExpression ce = getCondition(column);

			if (ce == null)
				continue;

			sb.append(", ").append(column).append(" ").append(ce.getOp()).append(" ").append(ce.getValue());
		}

		return sb.substring(1);
	}

	public int getStart() {
		return start;
	}

	public int getLimit() {
		return limit;
	}

	public void setStart(Integer start) {
		this.start = start == null ? 0 : start;
	}

	public void setLimit(Integer limit) {
		this.limit = limit == null ? 15 : limit;
	}
}
