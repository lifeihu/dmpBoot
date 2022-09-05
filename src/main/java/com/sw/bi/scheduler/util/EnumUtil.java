package com.sw.bi.scheduler.util;

public class EnumUtil {

	public enum ResourceType {
		MODULE, // JS Moudle
		URL; // URL
	}

	/**
	 * 用户状态
	 */
	public enum UserStatus {
		NORMAL, // 正常
		REMOVE; // 删除
	}

	/**
	 * 数据源类型
	 */
	public enum DatasourceType {
		MYSQL(0), // MySQL
		SQLSERVER(1), // SQLServer
		ORACLE(2), // Oracle
		FTP(3), // Ftp
		GREENPLUM(7), // Greenplum
		DATABASE(4); // 数据库数据源(包含MySQL、SQLServer、GP和Oracle),此类型为虚拟类型,需要查询时手工组织条件

		final int value;

		private DatasourceType(int value) {
			this.value = value;
		}

		public int indexOf() {
			return value;
		}
	}

	/**
	 * 数据源查看类型
	 * 
	 * @author shiming.hong
	 */
	public enum DatasourceViewType {
		ALL_USER, // 所有用户可见
		CREATE_USER, // 仅创建者可见
	}

}
