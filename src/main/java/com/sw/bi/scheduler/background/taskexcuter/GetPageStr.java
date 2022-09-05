/*===================================================================================
 Copyright (c) 2004-2006 by www.koubei.com, All rights reserved.
 8f., HuaXing technology building, 477# wensan road, HangZhou, China
 
 This software is the confidential and proprietary information of 
 Koubei.com, Inc. ("Confidential Information"). You shall not disclose 
 such Confidential Information and shall use it only in accordance 
 with the terms of the license agreement you entered into with Koubei.com, Inc.
===================================================================================
 File name: 	GetPageStr.java
 Author: 		�����
 Date: 			2007-1-24
 Description: 	
 Others: 		 
 Function List:  
 		1. ...
 History: 
===================================================================================*/
package com.sw.bi.scheduler.background.taskexcuter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

public class GetPageStr {

	/***************************************************************************
	 * 
	 * @param args
	 **************************************************************************/
	public static void main(String[] args) {

		System.out.println( GetPageStr.checkUrl("htttpj://jfkda?slf=jda&kj=dk&lkaf=�й�") ) ;
	}

	public static String checkUrl(String str){
		if( str.indexOf("?")>0 ){
			String[] sa=StringProcessor.splitString(str , "\\?" );
			if( sa != null ) if( sa.length > 2 ){
				System.out.println("") ;
			}
			String[] ssa = StringProcessor.splitString(sa[1],"\\&") ;
			
			if( ssa!=null) for(int i = 0 ; i < ssa.length ; i++ ){
				String[] s1 = StringProcessor.splitString(ssa[i],"\\=") ;
				if( s1 != null&&s1.length>=2){
					s1[1] = StringProcessor.URLEncode(s1[1]) ;
					ssa[i]=s1[0]+"="+s1[1] ;
				}else{
					ssa[i]=s1[0]+"=" ;
				}
				if( i==0 ){
					sa[1]=ssa[i] ;
				}else{
					sa[1]+="&"+ssa[i] ;
				}
			}
			return sa[0]+"?"+sa[1] ;
		}
		return str ;
	}

	public static String getWebContentGetMethod( String url, String coding ){
		url = checkUrl(url) ;
		if( StringProcessor.isEmpty(url)){
			return null ;
		}

		HttpClient httpClient = new HttpClient();

		GetMethod getMethod = new GetMethod( url );

		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler());
		try {
			// ִ��getMethod
			int statusCode = httpClient.executeMethod(getMethod);
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: "
						+ getMethod.getStatusLine());
			}

			byte[] responseBody = getMethod.getResponseBody();

			String rs = new String(responseBody , coding );
			return rs ;
		} catch (HttpException e) {

		} catch (IOException e) {

		} finally {

			getMethod.releaseConnection();
		}
		return null ;
	}
	

	public static String GBK( String s ){
		try{
			System.out.println(s);
			s =  new String( s.getBytes("UTF-8"), "GBK");
			System.out.println(s);
		}catch( UnsupportedEncodingException e ){
			return s ;
		}
		return s ;
	}

	public static String getWebContentPostMethod( String url , NameValuePair[] data , String coding ){
		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod(url);
		String sr = null ;

		postMethod.setRequestBody(data);
		try {

			int statusCode = httpClient.executeMethod(postMethod);

			if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
					|| statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
				Header locationHeader = postMethod
						.getResponseHeader("location");
				String location = null;
				if (locationHeader != null) {
					location = locationHeader.getValue();
					return getWebContentGetMethod( location , coding ) ;
				} else {
					System.err.println("Location field value is null.");
				}
			}
			sr = new String( postMethod.getResponseBody() , coding );
		} catch (HttpException e) {
			System.out.println("Please check your provided http address!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			postMethod.releaseConnection();
		}
		return sr ;
	}

	public static String getWebContent(String theURL) {
		String sTotalString = "";
		URL l_url = null;
		HttpURLConnection l_connection = null;
		java.io.InputStream l_urlStream = null;
		BufferedReader l_reader = null;
		try {
			l_url = new URL(theURL);
			l_connection = (HttpURLConnection) l_url.openConnection();
			l_connection.setConnectTimeout(5000);
			l_connection.setRequestProperty("" , "") ;
			l_connection.connect();
			l_urlStream = l_connection.getInputStream();
			l_reader = new BufferedReader(new InputStreamReader(l_urlStream));
			int buffer_size = 1024;
			char[] buffer = new char[buffer_size];
			StringBuffer sb = new StringBuffer();
			int readcount = 0;
			while ((readcount = l_reader.read(buffer, 0, buffer_size)) > 0) {
				sb.append(buffer, 0, readcount);
			}
			sTotalString = sb.toString();
			l_reader.close();
			l_urlStream.close();
			l_connection.disconnect();
		} catch (Exception e) {
			System.out.println(e.toString());
		} finally {
			if (l_reader != null) {
				try {
					l_reader.close();
				} catch (Exception e) {
				}
			}
			if (l_urlStream != null) {
				try {
					l_urlStream.close();
				} catch (Exception e) {
				}
			}
			if (l_connection != null) {
				try {
					l_connection.disconnect();
				} catch (Exception e) {
				}
			}
		}
		return sTotalString;
	}
	

	public static boolean saveImageLD( String urlstr , String savepath ){
		if( StringProcessor.isEmpty( urlstr )|| StringProcessor.isEmpty(savepath)){
			return false ;
		}
		DataInputStream di = null;
	    FileOutputStream fo = null;
	    byte [] b = new byte[1];  
	    try {
	       // input 
	       URL url = new URL( urlstr );
	       URLConnection urlConnection = url.openConnection();
	       urlConnection.connect();
	       di = new DataInputStream(urlConnection.getInputStream());

	       fo = new FileOutputStream( savepath );
	       //  copy the actual file
	       //   (it would better to use a buffer bigger than this)
	       while(-1 != di.read(b,0,1))  {
	         fo.write(b,0,1);
	       }
	       di.close();  
	       fo.close();                
	     }
	     catch (Exception ex) { 
	         return false ;
	     }
	     return true ;
	}
}
