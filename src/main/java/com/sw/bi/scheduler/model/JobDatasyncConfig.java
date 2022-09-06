package com.sw.bi.scheduler.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.sql.Timestamp;

/**
 * JobDatasyncConfig entity. @author MyEclipse Persistence Tools
 */

@JsonIgnoreProperties(value = { "hibernateLazyInitializer" })
public class JobDatasyncConfig implements java.io.Serializable {

	// Fields

	private Long jobId;

	private Datasource datasourceByTargetDatasourceId;
	private Datasource datasourceBySourceDatasourceId;
	private Datasource datasourceByFinalyDatasourceId;
	private Datasource datasourceByInitDatasourceId;
	private Datasource datasourceByFtpDatasourceId;

	private Long jobType;
	private String xmlTemplate;
	private String sourceCommitThreshold;
	private String sourceColumns;
	private String sourceDelimiter;
	private String sourceCharset;
	private String sourceFileType;
	private String sourceCodec;
	private String targetDatapath;
	private String targetColumns;
	private String targetDelimiter;
	private String targetCommitThreshold;
	private String targetCharset;
	private String targetFileType;
	private String targetCodec;
	private String targetEscape;
	private String initSql;
	private String finalyFailSql;
	private String finalySuccessSql;
	private Integer threadNumber;
	private String errorthreshold;
	private String userXml;
	private String successFlag;
	private String checkSeconds;
	private Integer timeoutMinutes;
	private String ftpDir;
	private String ftpBakDir;
	@Deprecated
	private String ftpErrDir;
	private String linuxTmpDir;
	private String linuxBakDir;
	private String linuxErrDir;
	private String hdfsPath;
	private String hiveTableName;
	private String hiveFields;
	private String createTableSql;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String sourceDatapath;
	private Integer fileNumber;
	private String dateTimePosition;
	private String fileUniquePattern;

	// add by zhuzhongji 2015年9月11日09:07:26
	private String sourceTableName;
	private String sourceColumnFields;
	private String targetTableName;
	private String targetColumnFields;

	// 以下二个字段用于导入HDFS后自动创建分区与导入的HDFS目录的关联
	private String referDbName;
	private String referTableName;
	private String referPartName;

	// 新增datax参数
	// add by mashifeng 2018年12月1日09:07:26
	private Long speedBytes;
	private Long errorLimitrecords;
	private String sourceCustomerParameter;
	private String targetCustomerParameter;
	
//	private String hdfsColumns;
	// private String writeMode;

	// add by mashifeng 2018年12月18日19:07:26
//	private Long sourceDataType;
//	private Long targetDataType;

	// add by mashifeng 2019年1月17日19:07:26
	// private String targetFileName;
	// private String targetFtpWriteMode;
	// private String targetProtocol;
	// private String sourceProtocol;
	// add by mashifeng 2019年1月20日17:07:26
//	private String sourceDbName;
//	private String targetDbName;
//	private String sourceDbColumnFields;
//	private String targetDbColumnFields;
//	private Boolean isReplace;
//	private String replaceKey;
	// add by mashifeng 2019年1月22日17:07:26
//	private Boolean isSplitData;
//	private String sourceSplitPk;
//	private String targetColumnName;

	// Constructors
	/** default constructor */
	public JobDatasyncConfig() {
	}

	/** minimal constructor */
	public JobDatasyncConfig(Long jobId, Long jobType) {
		this.jobId = jobId;
		this.jobType = jobType;
	}

	/** full constructor */
	public JobDatasyncConfig(Long jobId, Datasource datasourceByTargetDatasourceId,
                             Datasource datasourceBySourceDatasourceId, Datasource datasourceByFinalyDatasourceId,
                             Datasource datasourceByInitDatasourceId, Datasource datasourceByFtpDatasourceId, Long jobType,
                             String xmlTemplate, String sourceCommitThreshold, String sourceColumns, String sourceDelimiter,
                             String targetDatapath, String targetColumns, String targetDelimiter, String targetCommitThreshold,
                             String initSql, String finalyFailSql, String finalySuccessSql, String errorthreshold, String userXml,
                             String successFlag, String checkSeconds, String ftpDir, String ftpBakDir, String linuxTmpDir,
                             String linuxBakDir, String linuxErrDir, String hdfsPath, Timestamp createTime, Timestamp updateTime,
                             String sourceDatapath, Long speedBytes, Long errorLimitrecords, String sourceCustomerParameter, String targetCustomerParameter) {
		this.jobId = jobId;
		this.datasourceByTargetDatasourceId = datasourceByTargetDatasourceId;
		this.datasourceBySourceDatasourceId = datasourceBySourceDatasourceId;
		this.datasourceByFinalyDatasourceId = datasourceByFinalyDatasourceId;
		this.datasourceByInitDatasourceId = datasourceByInitDatasourceId;
		this.datasourceByFtpDatasourceId = datasourceByFtpDatasourceId;
		this.jobType = jobType;
		this.xmlTemplate = xmlTemplate;
		this.sourceCommitThreshold = sourceCommitThreshold;
		this.sourceColumns = sourceColumns;
		this.sourceDelimiter = sourceDelimiter;
		this.targetDatapath = targetDatapath;
		this.targetColumns = targetColumns;
		this.targetDelimiter = targetDelimiter;
		this.targetCommitThreshold = targetCommitThreshold;
		this.initSql = initSql;
		this.finalyFailSql = finalyFailSql;
		this.finalySuccessSql = finalySuccessSql;
		this.errorthreshold = errorthreshold;
		this.userXml = userXml;
		this.successFlag = successFlag;
		this.checkSeconds = checkSeconds;
		this.ftpDir = ftpDir;
		this.ftpBakDir = ftpBakDir;
		this.linuxTmpDir = linuxTmpDir;
		this.linuxBakDir = linuxBakDir;
		this.linuxErrDir = linuxErrDir;
		this.hdfsPath = hdfsPath;
		this.createTime = createTime;
		this.updateTime = updateTime;
		this.sourceDatapath = sourceDatapath;
		this.speedBytes = speedBytes;
		this.errorLimitrecords = errorLimitrecords;
		this.sourceCustomerParameter = sourceCustomerParameter;
		this.targetCustomerParameter = targetCustomerParameter;
		
		// this.targetFileName=targetFileName;
		// this.targetFtpWriteMode=targetFtpWriteMode;
		// this.targetProtocol=targetProtocol;
		// this.sourceProtocol=sourceProtocol;
//		this.sourceDbName = sourceDbName;
//		this.targetDbName = targetDbName;
//		this.sourceDbColumnFields = sourceDbColumnFields;
//		this.targetDbColumnFields = targetDbColumnFields;
//		this.isReplace = isReplace;
//		this.replaceKey = replaceKey;
//		this.isSplitData = isSplitData;
//		this.sourceSplitPk = sourceSplitPk;
//		this.targetColumnName = targetColumnName;

	}

	// Property accessors

	public Long getJobId() {
		return jobId;
	}

	public Datasource getDatasourceByTargetDatasourceId() {
		return datasourceByTargetDatasourceId;
	}

	public void setDatasourceByTargetDatasourceId(Datasource datasourceByTargetDatasourceId) {
		this.datasourceByTargetDatasourceId = datasourceByTargetDatasourceId;
	}

	public Datasource getDatasourceBySourceDatasourceId() {
		return datasourceBySourceDatasourceId;
	}

	public void setDatasourceBySourceDatasourceId(Datasource datasourceBySourceDatasourceId) {
		this.datasourceBySourceDatasourceId = datasourceBySourceDatasourceId;
	}

	public Datasource getDatasourceByFinalyDatasourceId() {
		return datasourceByFinalyDatasourceId;
	}

	public void setDatasourceByFinalyDatasourceId(Datasource datasourceByFinalyDatasourceId) {
		this.datasourceByFinalyDatasourceId = datasourceByFinalyDatasourceId;
	}

	public Datasource getDatasourceByInitDatasourceId() {
		return datasourceByInitDatasourceId;
	}

	public void setDatasourceByInitDatasourceId(Datasource datasourceByInitDatasourceId) {
		this.datasourceByInitDatasourceId = datasourceByInitDatasourceId;
	}

	public Datasource getDatasourceByFtpDatasourceId() {
		return datasourceByFtpDatasourceId;
	}

	public void setDatasourceByFtpDatasourceId(Datasource datasourceByFtpDatasourceId) {
		this.datasourceByFtpDatasourceId = datasourceByFtpDatasourceId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public Long getJobType() {
		return this.jobType;
	}

	public void setJobType(Long jobType) {
		this.jobType = jobType;
	}

	public String getXmlTemplate() {
		return this.xmlTemplate;
	}

	public void setXmlTemplate(String xmlTemplate) {
		this.xmlTemplate = xmlTemplate;
	}

	public String getSourceCommitThreshold() {
		return this.sourceCommitThreshold;
	}

	public void setSourceCommitThreshold(String sourceCommitThreshold) {
		this.sourceCommitThreshold = sourceCommitThreshold;
	}

	public String getSourceColumns() {
		return this.sourceColumns;
	}

	public void setSourceColumns(String sourceColumns) {
		this.sourceColumns = sourceColumns;
	}

	public String getSourceDelimiter() {
		return this.sourceDelimiter;
	}

	public void setSourceDelimiter(String sourceDelimiter) {
		this.sourceDelimiter = sourceDelimiter;
	}

	public String getSourceCharset() {
		return sourceCharset;
	}

	public void setSourceCharset(String sourceCharset) {
		this.sourceCharset = sourceCharset;
	}

	public String getSourceFileType() {
		return sourceFileType;
	}

	public void setSourceFileType(String sourceFileType) {
		this.sourceFileType = sourceFileType;
	}

	public String getSourceCodec() {
		return sourceCodec;
	}

	public void setSourceCodec(String sourceCodec) {
		this.sourceCodec = sourceCodec;
	}

	public String getTargetDatapath() {
		return this.targetDatapath;
	}

	public void setTargetDatapath(String targetDatapath) {
		this.targetDatapath = targetDatapath;
	}

	public String getTargetColumns() {
		return this.targetColumns;
	}

	public void setTargetColumns(String targetColumns) {
		this.targetColumns = targetColumns;
	}

	public String getTargetDelimiter() {
		return this.targetDelimiter;
	}

	public void setTargetDelimiter(String targetDelimiter) {
		this.targetDelimiter = targetDelimiter;
	}

	public String getTargetCommitThreshold() {
		return this.targetCommitThreshold;
	}

	public void setTargetCommitThreshold(String targetCommitThreshold) {
		this.targetCommitThreshold = targetCommitThreshold;
	}

	public String getTargetCharset() {
		return targetCharset;
	}

	public void setTargetCharset(String targetCharset) {
		this.targetCharset = targetCharset;
	}

	public String getTargetFileType() {
		return targetFileType;
	}

	public void setTargetFileType(String targetFileType) {
		this.targetFileType = targetFileType;
	}

	public String getTargetCodec() {
		return targetCodec;
	}

	public void setTargetCodec(String targetCodec) {
		this.targetCodec = targetCodec;
	}

	public String getTargetEscape() {
		return targetEscape;
	}

	public void setTargetEscape(String targetEscape) {
		this.targetEscape = targetEscape;
	}

	public String getInitSql() {
		return this.initSql;
	}

	public void setInitSql(String initSql) {
		this.initSql = initSql;
	}

	public String getFinalyFailSql() {
		return this.finalyFailSql;
	}

	public void setFinalyFailSql(String finalyFailSql) {
		this.finalyFailSql = finalyFailSql;
	}

	public String getFinalySuccessSql() {
		return this.finalySuccessSql;
	}

	public void setFinalySuccessSql(String finalySuccessSql) {
		this.finalySuccessSql = finalySuccessSql;
	}

	public Integer getThreadNumber() {
		return threadNumber;
	}

	public void setThreadNumber(Integer threadNumber) {
		this.threadNumber = threadNumber;
	}

	public String getErrorthreshold() {
		return this.errorthreshold;
	}

	public void setErrorthreshold(String errorthreshold) {
		this.errorthreshold = errorthreshold;
	}

	public String getUserXml() {
		return this.userXml;
	}

	public void setUserXml(String userXml) {
		this.userXml = userXml;
	}

	public String getSuccessFlag() {
		return this.successFlag;
	}

	public void setSuccessFlag(String successFlag) {
		this.successFlag = successFlag;
	}

	public String getCheckSeconds() {
		return this.checkSeconds;
	}

	public void setCheckSeconds(String checkSeconds) {
		this.checkSeconds = checkSeconds;
	}

	public Integer getTimeoutMinutes() {
		return timeoutMinutes;
	}

	public void setTimeoutMinutes(Integer timeoutMinutes) {
		this.timeoutMinutes = timeoutMinutes;
	}

	public String getFtpDir() {
		return this.ftpDir;
	}

	public void setFtpDir(String ftpDir) {
		this.ftpDir = ftpDir;
	}

	public String getFtpBakDir() {
		return this.ftpBakDir;
	}

	public void setFtpBakDir(String ftpBakDir) {
		this.ftpBakDir = ftpBakDir;
	}

	@Deprecated
	public String getFtpErrDir() {
		return this.ftpErrDir;
	}

	@Deprecated
	public void setFtpErrDir(String ftpErrDir) {
		this.ftpErrDir = ftpErrDir;
	}

	public String getLinuxTmpDir() {
		return this.linuxTmpDir;
	}

	public void setLinuxTmpDir(String linuxTmpDir) {
		this.linuxTmpDir = linuxTmpDir;
	}

	public String getLinuxBakDir() {
		return linuxBakDir;
	}

	public void setLinuxBakDir(String linuxBakDir) {
		this.linuxBakDir = linuxBakDir;
	}

	public String getLinuxErrDir() {
		return this.linuxErrDir;
	}

	public void setLinuxErrDir(String linuxErrDir) {
		this.linuxErrDir = linuxErrDir;
	}

	public String getHdfsPath() {
		return this.hdfsPath;
	}

	public void setHdfsPath(String hdfsPath) {
		this.hdfsPath = hdfsPath;
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

	public String getSourceDatapath() {
		return this.sourceDatapath;
	}

	public void setSourceDatapath(String sourceDatapath) {
		this.sourceDatapath = sourceDatapath;
	}

	public Integer getFileNumber() {
		return fileNumber;
	}

	public void setFileNumber(Integer fileNumber) {
		this.fileNumber = fileNumber;
	}

	public String getFileUniquePattern() {
		return fileUniquePattern;
	}

	public void setFileUniquePattern(String fileUniquePattern) {
		this.fileUniquePattern = fileUniquePattern;
	}

	public String getDateTimePosition() {
		return dateTimePosition;
	}

	public void setDateTimePosition(String dateTimePosition) {
		this.dateTimePosition = dateTimePosition;
	}

	public String getHiveTableName() {
		return hiveTableName;
	}

	public void setHiveTableName(String hiveTableName) {
		this.hiveTableName = hiveTableName;
	}

	public String getHiveFields() {
		return hiveFields;
	}

	public void setHiveFields(String hiveFields) {
		this.hiveFields = hiveFields;
	}

	public String getCreateTableSql() {
		return createTableSql;
	}

	public void setCreateTableSql(String createTableSql) {
		this.createTableSql = createTableSql;
	}

	public String getReferDbName() {
		return referDbName;
	}

	public void setReferDbName(String referDbName) {
		this.referDbName = referDbName;
	}

	public String getReferTableName() {
		return referTableName;
	}

	public void setReferTableName(String referTableName) {
		this.referTableName = referTableName;
	}

	public String getReferPartName() {
		return referPartName;
	}

	public void setReferPartName(String referPartName) {
		this.referPartName = referPartName;
	}

	public String getSourceTableName() {
		return sourceTableName;
	}

	public void setSourceTableName(String sourceTableName) {
		this.sourceTableName = sourceTableName;
	}

	public String getSourceColumnFields() {
		return sourceColumnFields;
	}

	public void setSourceColumnFields(String sourceColumnFields) {
		this.sourceColumnFields = sourceColumnFields;
	}

	public String getTargetTableName() {
		return targetTableName;
	}

	public void setTargetTableName(String targetTableName) {
		this.targetTableName = targetTableName;
	}

	public String getTargetColumnFields() {
		return targetColumnFields;
	}

	public void setTargetColumnFields(String targetColumnFields) {
		this.targetColumnFields = targetColumnFields;
	}

	// 新增datax参数
	// add by mashifeng 2018年12月1日09:07:26
	public Long getSpeedBytes() {
		return speedBytes;
	}

	public void setSpeedBytes(Long speedBytes) {
		this.speedBytes = speedBytes;
	}

	public Long getErrorLimitrecords() {
		return errorLimitrecords;
	}

	public void setErrorLimitrecords(Long errorLimitrecords) {
		this.errorLimitrecords = errorLimitrecords;
	}
	
	public String getSourceCustomerParameter() {
		return sourceCustomerParameter;
	}

	public void setSourceCustomerParameter(String sourceCustomerParameter) {
		this.sourceCustomerParameter = sourceCustomerParameter;
	}

	public String getTargetCustomerParameter() {
		return targetCustomerParameter;
	}

	public void setTargetCustomerParameter(String targetCustomerParameter) {
		this.targetCustomerParameter = targetCustomerParameter;
	}
	
	

/*	public String getHdfsColumns() {
		return hdfsColumns;
	}

	public void setHdfsColumns(String hdfsColumns) {
		this.hdfsColumns = hdfsColumns;
	}*/

	/*
	 * public String getWriteMode() { return writeMode; }
	 * 
	 * public void setWriteMode(String writeMode) { this.writeMode = writeMode;
	 * }
	 */

	// add by mashifeng 2018年12月18日19:07:26
/*	public Long getSourceDataType() {
		return sourceDataType;
	}

	public void setSourceDataType(Long sourceDataType) {
		this.sourceDataType = sourceDataType;
	}

	public Long getTargetDataType() {
		return targetDataType;
	}

	public void setTargetDataType(Long targetDataType) {
		this.targetDataType = targetDataType;
	}*/

	// add by mashifeng 2019年1月17日19:07:26
	/*
	 * public String getTargetFileName() { return targetFileName; }
	 * 
	 * public void setTargetFileName(String targetFileName) {
	 * this.targetFileName = targetFileName; }
	 * 
	 * public String getTargetFtpWriteMode() { return targetFtpWriteMode; }
	 * 
	 * public void setTargetFtpWriteMode(String targetFtpWriteMode) {
	 * this.targetFtpWriteMode = targetFtpWriteMode; }
	 */

	/*
	 * public String getTargetProtocol() { return targetProtocol; }
	 * 
	 * public void setTargetProtocol(String targetProtocol) {
	 * this.targetProtocol = targetProtocol; }
	 * 
	 * public String getSourceProtocol() { return sourceProtocol; }
	 * 
	 * public void setSourceProtocol(String sourceProtocol) {
	 * this.sourceProtocol = sourceProtocol; }
	 */
	// add by mashifeng 2019年1月20日17:07:26
/*	public String getSourceDbName() {
		return sourceDbName;
	}

	public void setSourceDbName(String sourceDbName) {
		this.sourceDbName = sourceDbName;
	}

	public String getTargetDbName() {
		return targetDbName;
	}

	public void setTargetDbName(String targetDbName) {
		this.targetDbName = targetDbName;
	}*/

/*	public String getSourceDbColumnFields() {
		return sourceDbColumnFields;
	}

	public void setSourceDbColumnFields(String sourceDbColumnFields) {
		this.sourceDbColumnFields = sourceDbColumnFields;
	}

	public String getTargetDbColumnFields() {
		return targetDbColumnFields;
	}

	public void setTargetDbColumnFields(String targetDbColumnFields) {
		this.targetDbColumnFields = targetDbColumnFields;
	}*/
/*
	public Boolean getIsReplace() {
		return isReplace;
	}

	public void setIsReplace(Boolean isReplace) {
		this.isReplace = isReplace;
	}

	public String getReplaceKey() {
		return replaceKey;
	}

	public void setReplaceKey(String replaceKey) {
		this.replaceKey = replaceKey;
	}

	// add by mashifeng 2019年1月22日17:07:26
	public Boolean getIsSplitData() {
		return isSplitData;
	}

	public void setIsSplitData(Boolean isSplitData) {
		this.isSplitData = isSplitData;
	}

	public String getSourceSplitPk() {
		return sourceSplitPk;
	}

	public void setSourceSplitPk(String sourceSplitPk) {
		this.sourceSplitPk = sourceSplitPk;
	}

	public String getTargetColumnName() {
		return targetColumnName;
	}

	public void setTargetColumnName(String targetColumnName) {
		this.targetColumnName = targetColumnName;
	}
*/
}