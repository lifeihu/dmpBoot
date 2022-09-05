package com.sw.bi.scheduler.background.javatype.tablerelation;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.taskexcuter.Parameters;
import com.sw.bi.scheduler.background.taskexcuter.xml.DxFileUtils;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.EtlTable;
import com.sw.bi.scheduler.model.EtlTableRelation;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.service.EtlTableRelationService;
import com.sw.bi.scheduler.service.EtlTableService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.util.DateUtil;

//元数据表血缘关系分析
//对指定目录下的.sql的hive sql脚本文件进行解析,解析出etl_table和etl_table_relation
//本程序配置成一个定点任务的形式.
//然后前台再提供一个元数据关系查询的界面.
//经过验证. 表的关系解析是正确的. 那个批量生成测试数据的脚本有点问题. 本脚本是正确的.
//规范要求:  所有的建表语句都必须写在hive sql脚本中.
// 1. 配置一个类型为MapReduce的任务. 用来定点解析元数据表之间的关系.
//    程序路径    ${scheduler_path}/scheduler jar ${scheduler_path}/scheduler.jar com.sw.bi.scheduler.background.javatype.tablerelation.HiveTableRelation
//    /home/tools/scheduler/scheduler jar   /home/tools/meta.jar com.sw.bi.scheduler.background.javatype.tablerelation.HiveTableRelation
// 2. 制作一个前台查询界面,可以查询元数据表的关系
// lifeng 2012-02-21
@Component
public class TableRelation {
	
	    private static final Logger log = Logger.getLogger(TableRelation.class);
	    
		@Autowired
		private JobService jobService;
		
		@Autowired
		private EtlTableService etlTableService;

		@Autowired
		private EtlTableRelationService etlTableRelationService;
		
	
	    private String sql_path = Parameters.hivesqlPath;

	    
	    private HashMap<String,Long> map = new HashMap<String,Long>();
	    private HashMap<String,Long> table_map = new HashMap<String,Long>();
	    
	    private HashMap<String,String> error_map1 = new HashMap<String,String>();
	    private HashMap<String,String> error_map2 = new HashMap<String,String>();
	    private HashMap<String,String> error_map3 = new HashMap<String,String>();
	    private HashMap<String,String> error_map4 = new HashMap<String,String>();
	
	    public static void main(String args[]) throws IOException{
	    	TableRelation.getHiveTableRelation().buildHiveTableRelations();
	    }
	     
	 	private static TableRelation getHiveTableRelation() {
			return BeanFactory.getBean(TableRelation.class);
		}
	 	
	 	//考虑每天的解析都保留快照
	 	private void buildHiveTableRelations() throws IOException{
	 		log.info("sql_path1: "+sql_path);
	 		sql_path = "/home/tools/etl/";
	 		log.info("sql_path2: "+sql_path);
	 		log.info("开始删除etlTableRelation表历史记录...");
	 		etlTableRelationService.delete(etlTableRelationService.queryAll());
	 		log.info("etlTableRelation表历史记录删除完毕...");
	 		log.info("开始删除etlTable表历史记录...");
	 		etlTableService.delete(etlTableService.queryAll());
	 		log.info("etlTable表历史记录删除完毕...");
	 		initDual();
	 		log.info("开始initJobMap()...");
	 		initJobMap();
	 		log.info("initJobMap()完毕...");
	 		log.info("开始parseDir,程序路径是: "+sql_path+" 当前动作:buildEtlTables");
	 		parseDir(sql_path,"buildEtlTables");
	 		log.info("parseDir完毕");
	 		log.info("开始initTableMap()...");
	 		initTableMap();
	 		log.info("initTableMap()完毕...");
	 		log.info("开始parseDir,程序路径是: "+sql_path+" 当前动作:buildEtlTableRelations");
	 		parseDir(sql_path,"buildEtlTableRelations");
	 		log.info("程序运行完毕!!!");
	 		

	 		for (Iterator it = error_map1.keySet().iterator(); it.hasNext();) {
	             System.out.println(error_map1.get((String) it.next()));
	 		}
	 		for (Iterator it = error_map2.keySet().iterator(); it.hasNext();) {
	             System.out.println(error_map2.get((String) it.next()));
	 		}
	 		for (Iterator it = error_map3.keySet().iterator(); it.hasNext();) {
	             System.out.println(error_map3.get((String) it.next()));
	 		}
	 		for (Iterator it = error_map4.keySet().iterator(); it.hasNext();) {
	             System.out.println(error_map4.get((String) it.next()));
	 		}
	 		
	 		
	 		
	 	}
	 	
	 	private  void parseDir(String dir,String action) throws IOException{
			File root = new File(dir);
			File[] filesOrDirs = root.listFiles();
			for(int i=0;i<filesOrDirs.length;i++){
				File file = filesOrDirs[i];
				if(file.isDirectory()){
					parseDir(file.getAbsolutePath(),action);
				}else{
					if(file.getName().endsWith(".sql")){
						parseFile(file.getAbsolutePath(),action);
					}else{
						////log.info("info: ========================="+file.getName()+"该文件不是以.sql结尾的,略过解析.=========================");
					}
				}
			}
		}
		
		private  void parseFile(String filename,String action) throws IOException{
			
			String test_string = DxFileUtils.file2String(filename, "UTF-8");

			String[] sqls = test_string.split(";");
			for(int i=0;i<sqls.length;i++){
				String sql = sqls[i];
				if("buildEtlTables".equals(action)){
					parseSqlToBuildEtlTables(sql,filename);
				}else if("buildEtlTableRelations".equals(action)){
					parseSqlToBuildEtlTableRelations(sql,filename);
				}
			}
		}
		
		// 对sql的解析,今后可能会随着hive sql中的语句的复杂度的增加或者hive新语法的出现有所修改.
		private  void parseSqlToBuildEtlTables(String sql,String filename){
			String[] words = sql.split("\\s+");
			for(int i=0;i<words.length;i++){
				String word = words[i].toLowerCase();
				if("create".equals(word)){
					String table_name = "";
					if("if".equals(words[i+2])){
						table_name = words[i+5];
					}else{
						table_name = words[i+2];
					}
					if(table_name!=null&&table_name.endsWith("(")){
						table_name = table_name.substring(0,table_name.length()-1);
					}

					Date task_date = DateUtil.getToday();
					
					//这里的filename是程序目录下面的。 而map中存放的是上线作业的。map中的范围要小一些
					Long job_id = map.get(filename);
					if(job_id==null){
						//job记录被删除了. 脚本还放在那里.
						////log.info("info: 当前job表中,没有用到"+filename+" 产生的原因可能是: job记录已经删除了,脚本还在程序目录中.");
						error_map1.put(filename, "info: 当前job表中,没有用到"+filename+" 产生的原因可能是: job记录已经删除了,脚本还在程序目录中.");
						continue;
					}
				
					EtlTable etlTable = new EtlTable();
					etlTable.setTaskDate(task_date);
					etlTable.setTableName(table_name);
					etlTable.setProgramFullName(filename);
					etlTable.setJobId(job_id);
					etlTable.setCreateTable(sql);

					etlTableService.save(etlTable);
				}
			}
		}
		
		
		
		private  void parseSqlToBuildEtlTableRelations(String sql,String filename){

			String[] words = sql.split("\\s+");
			String from_table = "";
			String to_table = "";
			for(int i=0;i<words.length;i++){
				String word = words[i].toLowerCase();
				if("insert".equals(word)){
					to_table = words[i+3];
				}
				if("from".equals(word)){
					String is_from_table=words[i+1];
					if(is_from_table.indexOf("(")>=0||is_from_table.indexOf("select")>=0){
						continue;
					}
					from_table=words[i+1];
				}
			}
			if(from_table!=""&&to_table!=""){
					if(table_map.get(to_table)==null){
						//log.info("===to_table: "+to_table);
						// 脚本存在,job中没配上去
						////log.info("info: "+to_table+"表所在的程序脚本没有被job表中的记录使用...");
						error_map2.put(to_table, "info: "+to_table+"表所在的程序脚本没有被job表中的记录使用...");
						return;
					}
					
					
					long table_id = table_map.get(to_table);
					
					if(table_map.get(from_table)==null){
						// 这个是因为dual表的create table语句没有在脚本中. 导致etl_table没有table_name是dual的记录
						//log.info("===from_table: "+from_table);
						/////log.info("error: "+from_table+"的建表语句没有放在hivesql的脚本中,请及时补上.");
						error_map3.put(from_table, "error: "+from_table+"的建表语句没有放在hivesql的脚本中,请及时补上.");
					}
	
					long parent_table_id = 0;

					
					if(table_map.get(from_table)==null){
						//log.info("★★★from_table: "+from_table+" 没有对应的parent_table_id");
						error_map4.put(from_table, "★★★from_table: "+from_table+" 没有对应的parent_table_id");
						return;
					}
					
					parent_table_id = table_map.get(from_table);
					
 
					
					Date task_date = DateUtil.getToday();
					
					EtlTableRelation etlTableRelation = new EtlTableRelation();
					etlTableRelation.setTaskDate(task_date);
					etlTableRelation.setTableId(table_id);
					etlTableRelation.setParentTableId(parent_table_id);
					
					etlTableRelationService.save(etlTableRelation);
				

			}
		}
		
		//所有在线的作业，把程序路径和作业ID放入map中
		private  void initJobMap(){
			List<Job> jobs = jobService.getOnlineJobs();
			//List<Job> jobs = jobService.queryAll();
			for(Job job : jobs){
				map.put(job.getProgramPath(), job.getJobId());
			}
		}
		
		private void initTableMap(){
			List<EtlTable> list = etlTableService.queryAll();
			for(EtlTable etlTable:list){
				table_map.put(etlTable.getTableName(), etlTable.getEtlTableId());
			}
		}
		
		
		private void initDual(){
			Date task_date = DateUtil.getToday();
			EtlTable etlTable = new EtlTable();
			etlTable.setTaskDate(task_date);
			etlTable.setTableName("dual");
			etlTable.setProgramFullName("");
			etlTable.setJobId(0l);
			String sql = "CREATE TABLE if not exists dual("+"\n"+
                                       "s   STRING)"+"\n"+
                                       "STORED AS TEXTFILE";
			etlTable.setCreateTable(sql);

			etlTableService.save(etlTable);
			
			Criteria criteria = etlTableService.createCriteria();
			criteria.add(Restrictions.eq("tableName", "dual"));
			criteria.setProjection(Projections.property("etlTableId"));
			Long table_id = (Long) criteria.uniqueResult();
			
			EtlTableRelation etlTableRelation = new EtlTableRelation();
			etlTableRelation.setTaskDate(task_date);
			etlTableRelation.setTableId(table_id);
			etlTableRelation.setParentTableId(0);
			
			etlTableRelationService.save(etlTableRelation);
			
		}
	 
}
