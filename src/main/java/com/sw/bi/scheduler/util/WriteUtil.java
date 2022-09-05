package com.sw.bi.scheduler.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDatasyncConfig;

public class WriteUtil {

	public static Object DataxWriteLinux(JobDatasyncConfig jobDatasyncConfig, Job job,Datasource dataSource) {
		FileSystem fs;
		File dfs;
		Configuration conf = new Configuration();
	    try
	    {
//	      String fileName = "/home/tools/datax/datax_file_template/pgtohdfs/jobId"+job.getJobId()+"/";
		  String fileName = "F:/";
		  dfs = new File(fileName +"pgtohdfs.json");
          if (!dfs.exists()) {  
        	  dfs.createNewFile();  
          }  
        FileWriter fw = new FileWriter(dfs.getAbsoluteFile());  
        BufferedWriter bw = new BufferedWriter(fw); 
//	      Datasource dataId = jobDatasyncConfig.getDatasourceBySourceDatasourceId();
//	      System.out.println("sourceid:"+dataId.getDatasourceId());
	      //DatasourceServiceImpl service = new DatasourceServiceImpl();
	      //Datasource newData = service.queryById(1L);
	      System.out.println("databaseName:"+dataSource.getDatabaseName());
	      if(dataSource.getDatasourceId()==10){
	  	  String json="{\"job\":"
				+ "{\"setting\":"
				+ "{\"speed\":{\"byte\":"+jobDatasyncConfig.getSpeedBytes()+",\"channel\":\""+jobDatasyncConfig.getThreadNumber()+"\"},"
				+ "\"errorLimit\":{\"record\":"+jobDatasyncConfig.getErrorLimitrecords()+",\"percentage\":"+jobDatasyncConfig.getErrorthreshold()+"}},"
				+ "\"content\":"
				+ "[{\"reader\":"
				+ "{\"name\":\"postgresqlreader\","
				+ "\"parameter\":"
				+ "{\"username\":\"pgxl\",\"password\":\"Pg!@#123\",\"where\":\"\",\"connection\":["
				+ "{\"querySql\":[\""+jobDatasyncConfig.getSourceDatapath()+"\"],"
				+ "\"jdbcUrl\":[\"jdbc:postgresql://10.2.50.102:15432/postgres\"]}]}},"
				+ "\"writer\":"
				+ "{\"name\":\"hdfswriter\","
				+ "\"parameter\":"
				+ "{\"defaultFS\":\"hdfs://nn1:8020\","
				+ "\"fileType\":\""+jobDatasyncConfig.getTargetFileType()+"\","
				+ "\"path\":\""+jobDatasyncConfig.getTargetDatapath()+"\","
				+ "\"fileName\":\""+jobDatasyncConfig.getReferTableName()+"\","
				//+ "\"column\":["+jobDatasyncConfig.getHdfsColumns()+"],"
				//+ "\"writeMode\":\""+jobDatasyncConfig.getWriteMode()+"\","
				+ "\"encoding\":\""+jobDatasyncConfig.getTargetCharset()+"\","
				+ "\"fieldDelimiter\":\""+jobDatasyncConfig.getTargetDelimiter()+"\"}}}]}}";
	  	  System.out.println("json:"+json);
	  	  bw.write(json);
	      }
	      bw.close();
	      jobDatasyncConfig.setUserXml(dfs.toString());
	    }
	    catch (IOException e)
	    {
	      e.printStackTrace();
	    }
	return jobDatasyncConfig;
	}
	  public static void main(String[] args) {
		  WriteUtil util = new WriteUtil();
		  //final JobDatasyncConfig WriteUtil = null;
		  //Job job = new Job();
		  //job.setJobId(1L);
		  //util.DataxWriteLinux(WriteUtil, job);
		  
	  }

}
