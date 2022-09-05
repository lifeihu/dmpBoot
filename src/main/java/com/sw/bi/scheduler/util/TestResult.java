package com.sw.bi.scheduler.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class TestResult {

	public static void main(String[] args) {
		
		
		
    String result ="{\"job\":{\"content\":[{\"reader\":{\"parameter\":{\"userPassword\":\"mongodb\",\"address\":[\"192.168.90.85:27017\"],\"dbName\":\"db\",\"column\":[{\"name\":\"phone\",\"type\":\"String\"},{\"name\":\"appId\",\"type\":\"String\"}],\"userName\":\"adtimedb\",\"collectionName\":\"dbc_phone_tyid_fromHIVE_2_temp\"},\"name\":\"mongodbreader\"},\"writer\":{\"parameter\":{\"password\":\"dms2015\",\"session\":[],\"column\":[\"phone\",\"appId\"],\"connection\":[{\"jdbcUrl\":\"jdbc:mysql://10.2.30.36:3306/test?yearIsDateType=false&amp;useUnicode=true&amp;characterEncoding=utf-8\",\"table\":[\"test_mashifeng\"]}],\"writeMode\":\"insert\",\"username\":\"dms\",\"preSql\":[]},\"name\":\"mysqlwriter\"}}],\"setting\":{\"errorLimit\":{\"record\":0,\"percentage\":0.9},\"speed\":{\"byte\":102546,\"channel\":10}}}}";
    JSONObject results = JSON.parseObject(result);

    JSONObject a1 = (JSONObject) results.get("job");
    
    JSONArray content =  a1.getJSONArray("content");
    
    String jsonStr2 = "{\"column\": [{\"name\":\"phone2222\",\"type\":\"String\"}], \"dbName\": \"tag_per_data\"}";
    JSONObject customerReadJson = JSON.parseObject(jsonStr2);
    
    
    String jsonStr3 = "{\"column\": [{\"name\":\"phone3333\",\"type\":\"String\"}], \"dbName\": \"tag_per_data\"}";
    JSONObject customerWriteJson = JSON.parseObject(jsonStr3);
    
    
    if(content.size()>0){
    	  for(int i=0; i<content.size(); i++){
    	    JSONObject readOrWrite = content.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
    	     
    	        //read
    	    	JSONObject readObject = (JSONObject)readOrWrite.get("reader");
    	    	JSONObject readParameter = (JSONObject) readObject.get("parameter");
    	    	readParameter.putAll(customerReadJson);
    	    	System.out.println(readParameter);
    	    	//writer
    	    	JSONObject writeObject = (JSONObject)readOrWrite.get("writer");
    	    	JSONObject writeParameter = (JSONObject) writeObject.get("parameter");
    	    	writeParameter.putAll(customerWriteJson);
    	    	System.out.println(writeParameter);
    	    
    	  }
    	}
    	System.out.println(results);
    
    
	}

}
