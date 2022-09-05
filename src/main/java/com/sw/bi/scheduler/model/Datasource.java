package com.sw.bi.scheduler.model;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSetter;

import java.sql.Timestamp;

/**
 * Datasource entity. @author MyEclipse Persistence Tools
 */

@JsonIgnoreProperties(value = { "hibernateLazyInitializer" })
public class Datasource implements java.io.Serializable, AuthenticationUserGroup {

	// Fields

	private Long datasourceId;
	private String name;
	private String description;
	private Long type;
	private String charset;
	private String ip;
	private String port;
	private String databaseName;
	private String username;
	private String password;
	private Timestamp createTime;
	private Timestamp updateTime;
	private long createBy;
	private Long updateBy;
	private String connectionString;
	private int viewType;
	private boolean active;

	private UserGroup userGroup;

	// Constructors

	/** default constructor */
	public Datasource() {}

	/** minimal constructor */
	public Datasource(Timestamp createTime) {
		this.createTime = createTime;
	}

	/** full constructor */
	public Datasource(String name, String description, Long type, String charset, String ip, String port, String databaseName, String username, String password, Timestamp createTime,
			Timestamp updateTime, String connectionString) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.charset = charset;
		this.ip = ip;
		this.port = port;
		this.databaseName = databaseName;
		this.username = username;
		this.password = password;
		this.createTime = createTime;
		this.updateTime = updateTime;
		this.connectionString = connectionString;
	}

	// Property accessors

	public Long getDatasourceId() {
		return this.datasourceId;
	}

	public void setDatasourceId(Long datasourceId) {
		this.datasourceId = datasourceId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getType() {
		return this.type;
	}

	public void setType(Long type) {
		this.type = type;
	}

	public String getCharset() {
		return this.charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getIp() {
		return this.ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return this.port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getDatabaseName() {
		return this.databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@JsonIgnore
	public String getPassword() {
		return this.password;
	}

	@JsonSetter
	public void setPassword(String password) {
		this.password = password;
	}

	public Timestamp getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	public Timestamp getUpdateTime() {
		return this.updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public long getCreateBy() {
		return createBy;
	}

	public void setCreateBy(long createBy) {
		this.createBy = createBy;
	}

	public Long getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(Long updateBy) {
		this.updateBy = updateBy;
	}

	public String getConnectionString() {
		return this.connectionString;
	}

	public void setConnectionString(String connectionString) {
		this.connectionString = connectionString;
	}

	public int getViewType() {
		return viewType;
	}

	public void setViewType(int viewType) {
		this.viewType = viewType;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	@Override
	public String getEntityName() {
		return "数据源";
	}

	@Override
	public String getLoggerName() {
		return this.getName();
	}

	@Override
	public Long getUserId() {
		return this.getCreateBy();
	}

}