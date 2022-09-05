package org.springframework.ui;

public class OrderBy {
	private String column;
	private String direction;

	public OrderBy(String column, String direction) {
		this.column = column;
		this.direction = !"DESC".equalsIgnoreCase(direction) ? "ASC" : "DESC";
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public boolean isAsc() {
		return !"DESC".equalsIgnoreCase(this.direction);
	}

	@Override
	public String toString() {
		return column + " " + direction;
	}

	public String toSqlString() {
		String sqlColumn = "";
		for (char c : column.toCharArray()) {
			String ch = String.valueOf(c);

			if (ch.equals(ch.toUpperCase())) {
				ch = "_" + ch.toLowerCase();
			}

			sqlColumn += ch;
		}

		return sqlColumn + " " + direction;
	}
}
