/*===================================================================================
 Copyright (c) 2004-2006 by www.koubei.com, All rights reserved.
 8f., HuaXing technology building, 477# wensan road, HangZhou, China
 
 This software is the confidential and proprietary information of 
 Koubei.com, Inc. ("Confidential Information"). You shall not disclose 
 such Confidential Information and shall use it only in accordance 
 with the terms of the license agreement you entered into with Koubei.com, Inc.
===================================================================================
 File name: 	StringProcessor.java
 Author: 		�����
 Date: 			2007-1-24
 Description: 	
 Others: 		 
 Function List:  
 		1. ...
 History: 
===================================================================================*/
package com.sw.bi.scheduler.background.taskexcuter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 * 
 * �ַ���
 * 
 * @author �����
 * @version 1.0
 ******************************************************************************/
public class StringProcessor {

	private static final Object[] String = null;

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		
/*		String aaa="abcdefgllhijklmn5465";
		String[] bbb=StringProcessor.midString(aaa, "b","l");
		System.out.println("bbb[0]:"+bbb[0]);//cdefghijk
		System.out.println("bbb[1]:"+bbb[1]);//lmn
		
		
		
		String aaa="abcdefgllhijklmn5465";
		String[] bbb=StringProcessor.midString(aaa, "b","l");
		//ab       cdefg     llhijklmn5465
		//         Ԫ��0        Ԫ��1
*/		
		//�����ǵõ��ڶ������͵�������֮����ַ�,����Ԫ��0;Ȼ���Ԫ��0�����ַ�֮���,����Ԫ��1
        		
//		TODO Auto-generated method stub
//		StringProcessor sp = new StringProcessor() ;
//		String s = "abcdfafa;gkd<tAble jkljkj/>al;g<table jkljkj,.></>kl;ad'gkda'gj'dagj'<table jkljkj/> efg<table jkljkj/> ABC" ;
//		System.out.println(	s ) ;
//		System.out.println(	sp.fomateToXmlLike( s , "table" )) ;
//		System.out.println( removeHTMLLable( "kl&;ssds&nbsp;&d;j<table>fdddda</table>kjkljkljk<br>l/>j;jlk;jl;" ) ) ;
//		String s = "aassssaaasssssddsaasssssaafffffggaaggggaagggaagaaggg" ;
//		int[] i = getSubStringPos( s , "aa"  , true ) ;
//		for( int j = 0 ; j<i.length ; j++ ){
//			System.out.println( i[j] ) ;
//			System.out.println( s.substring( i[j] , i[j] + 2 ) ) ;
//		}
//		System.out.println( removeOutHTMLLable( "kafja<CCCC>lsj<dafdafdsa.,>dfa   fd<fdsafd	saf>dljf\nsaljfkdafjdk" ) );
//		String[] sa = StringProcessor.midString( "s<><>afdsaa<>fasdgaghagadgfaf" , "<a>" , "<>" ) ;
//		System.out.println( sa[0] ) ;
//		System.out.println( sa[1] ) ;
//        String [] tem = getStringArrayByPattern("<a href='/CD.asp?DI=50508842&CI=50072863&MarketType=N' target='_blank'>  <a href='/CD.asp?DI=50508842&CI=50072864&MarketType=N' target='_blank'>", "(/CD.asp\\?DI=\\d+&CI=\\d+&MarketType=N)");
//        System.out.println(StringProcessor.isMatch("<a href='/CD.asp?DI=50508842&CI=50072863&MarketType=N' target='_blank'>  <a href='/CD.asp?DI=50508842&CI=50072864&MarketType=N' target='_blank'>", "/CD.asp\\?DI=\\d+&CI=\\d+&MarketType=N"));
	}
	
	public static String changCoding( String s , String fencode , String bencode ){
		try{
			String str = new String( s.getBytes(fencode) , bencode ) ;
			return str ;
		}catch( UnsupportedEncodingException e ){
			return s ;
		}
	}

	public static String removeHTMLLableExe( String str ){
		str = stringReplace( str , ">\\s*<" , "><" ) ;
		str = stringReplace( str , "&nbsp;" , " " ) ;
		str = stringReplace( str , "<br ?/?>" , "\n" ) ;
		str = stringReplace( str , "<([^<>]+)>" , "" ) ;
		str = stringReplace( str , "\\s\\s\\s*" , " " ) ;
		str = stringReplace( str , "^\\s*" , "" ) ;
		str = stringReplace( str , "\\s*$" , "" ) ;
		str = stringReplace( str , " +" , " " ) ;
		return str ;
	}

	public static String removeHTMLLable( String str ){
		str = stringReplace( str , "\\s" , "" ) ;//ȥ��ҳ���Ͽ��������ַ�
		str = stringReplace( str , "<br ?/?>" , "\n" ) ;//ȥ<br><br />
		str = stringReplace( str , "<([^<>]+)>" , "" ) ;//ȥ��<>�ڵ��ַ�
		str = stringReplace( str , "&nbsp;" , " " ) ;//�滻�ո�
		str = stringReplace( str , "&(\\S)(\\S?)(\\S?)(\\S?);" , "" ) ;//ȥ<br><br />
		return str ;
	}
	/**
	 * ȥ��HTML��ǩ֮����ַ�
	 * @param str Դ�ַ�
	 * @return Ŀ���ַ�
	 */
	public static String removeOutHTMLLable( String str ){
		str = stringReplace( str , ">([^<>]+)<" , "><" ) ;
		str = stringReplace( str , "^([^<>]+)<" , "<" ) ;
		str = stringReplace( str , ">([^<>]+)$" , ">" ) ;
		return str ;
	}
	
	/**
	 * 
	 * �ַ��滻
	 * @param str Դ�ַ�
	 * @param sr ������ʽ��ʽ
	 * @param sd �滻�ı�
	 * @return ���
	 
	 */
	public static String stringReplace( String str , String sr , String sd ) {
		String regEx=sr ;
		Pattern p=Pattern.compile(regEx,Pattern.CASE_INSENSITIVE); 
		Matcher m=p.matcher(str);
		str = m.replaceAll(sd) ;
		return str ;
	}
	/**
	 * 
	 * ��html��ʡ��д���滻�ɷ�ʡ��д��
	 * @param str html�ַ�
	 * @param pt ��ǩ��table
	 * @return ���
	 
	 */
	public static String fomateToFullForm( String str , String pt ) {
		String regEx="<"+pt+"\\s+([\\S&&[^<>]]*)/>";
		Pattern p=Pattern.compile(regEx,Pattern.CASE_INSENSITIVE); 
		Matcher m=p.matcher(str);
		String[] sa = null ;
		String sf = "" ;
		String sf2 = "" ;
		String sf3 = "" ;
		for( ;m.find(); ) {
			sa = p.split(str) ;
			if( sa == null ) {
				break ;
			}
			sf = str.substring( sa[0].length() , str.indexOf("/>" , sa[0].length()) ) ;
			sf2 = sf + "></" + pt + ">" ;
			sf3 = str.substring( sa[0].length() + sf.length() + 2 ) ;
			str = sa[0] + sf2 + sf3 ;
			sa = null ;
		}
		return str ;
	}
	
	/**
	 * 
	 * �õ��ַ���Ӵ�λ������
	 * @param str �ַ�
	 * @param sub �Ӵ�
	 * @param b true�Ӵ�ǰ��,false�Ӵ����
	 * @return �ַ���Ӵ�λ������
	 
	 */
	public static int[] getSubStringPos( String str , String sub , boolean b ){
		//int[] i = new int[(new Integer((str.length()-stringReplace( str , sub , "" ).length())/sub.length())).intValue()] ;
		String[] sp = null ;
		int l = sub.length() ; 
		sp = splitString( str , sub ) ;
		if( sp==null){
			return null ;
		}
		int[] ip = new int[sp.length-1] ;
		for( int i = 0 ; i<sp.length-1 ; i++ ){
			ip[i]=sp[i].length()+l ;
			if( i!=0){
				ip[i]+=ip[i-1] ;
			}
		}
		if( b ){
			for( int j = 0 ; j < ip.length ; j++ ){
				ip[j]=ip[j]-l ;
			}
		}
		return ip ;
	}
	/**
	 * 
	 * ���������ʽ�ָ��ַ�
	 * @param str Դ�ַ�
	 * @param ms ������ʽ
	 * @return Ŀ���ַ���
	 */
	public static String[] splitString( String str , String ms ){
		String regEx=ms;
		Pattern p=Pattern.compile(regEx,Pattern.CASE_INSENSITIVE); 
		String[] sp = p.split( str ) ;
		return sp ;
	}
	
	/**
	 * *************************************************************************
	 * ���������ʽ��ȡ�ַ�,��ͬ���ַ�ֻ����һ��
	 * @author ������
	 * @param str  Դ�ַ�
	 * @param pattern ������ʽ
	 * @return Ŀ���ַ������
	 *************************************************************************
	 */
	
	//�ﴫ��һ���ַ��ѷ��pattern��ʽ���ַ�����ַ�����
	//java.util.regex��һ����������ʽ���Ƶ�ģʽ4���ַ����ƥ�乤�������
	public static String[] getStringArrayByPattern(String str,String pattern){
		Pattern p = Pattern.compile(pattern,Pattern.CASE_INSENSITIVE);
		Matcher matcher= p.matcher(str);
		//����
		Set<String> result = new HashSet<String>();//Ŀ���ǣ���ͬ���ַ�ֻ����һ�����  ���ظ�Ԫ��
		//boolean  find()  ������Ŀ���ַ��������һ��ƥ���Ӵ���  
		while(matcher.find()){	
			for(int i=0; i<matcher.groupCount(); i++){  //int  groupCount()  ���ص�ǰ�������õ�ƥ���������
        //System.out.println(matcher.group(i));
				result.add(matcher.group(i));
				
			}
		}
		String[] resultStr = null; 
		if(result.size() > 0){
			resultStr = new String[result.size()];
			return result.toArray(resultStr);//��Set resultת��ΪString[] resultStr
		}
		return resultStr;
		
	}
	
	/**
	 * 
	 * �ַ��Ƿ�Ϊ��
	 * @param s �ַ�
	 * @return �Ƿ�Ϊ��
	 
	 */
	public static boolean isEmpty( String s ) {
		if( s==null || s.equals("") ){
			return true ;
		} else {
			return false ;
		}
	}
	/**
	 * �õ���һ��b,e֮����ַ�,������e����Ӵ�
	 * @param s Դ�ַ�
	 * @param b ��־��ʼ
	 * @param e ��־����
	 * @return b,e֮����ַ�
	 */
	
	/*String aaa="abcdefghijklmn";
	String[] bbb=StringProcessor.midString(aaa, "b","l");
	System.out.println("bbb[0]:"+bbb[0]);//cdefghijk
	System.out.println("bbb[1]:"+bbb[1]);//lmn
	�������ǵõ��ڶ������͵�������֮����ַ�,����Ԫ��0;Ȼ���Ԫ��0�����ַ�֮���,����Ԫ��1
    */		
	
/*	String aaa="abcdefgllhijklmn5465";
	String[] bbb=StringProcessor.midString(aaa, "b","l");
	//ab       cdefg       llhijklmn5465
	//         Ԫ��0         Ԫ��1
*/	
	public static String[] midString( String s , String b , String e ){
		int i = s.indexOf( b )+b.length() ;
		int j = s.indexOf( e , i ) ;
		String[] sa = new String[2] ;
		if( i < b.length() || j<i+1 || i>j ){
			sa[1] = s ;
			sa[0] = null ;
			return sa ;
		}else{
			sa[0] = s.substring( i , j ) ;
			sa[1] = s.substring( j ) ;
			return sa ;
		}
	}
	
	/**
	 * ����ǰһ��������е�������ʽ���
	 * @param s 
	 * @param pf
	 * @param pb
	 * @param start
	 * @return
	 */
	public static String stringReplace( String s , String pf , String pb , int start ){
		Pattern pattern_hand = Pattern.compile( pf ) ;
		Matcher matcher_hand = pattern_hand.matcher(s) ;
		int gc = matcher_hand.groupCount() ;
		int pos = start ;
		String sf1 = "" ;
		String sf2 = "" ;
		String sf3 = "" ;
		int if1 = 0 ;
		String strr = "" ;
		while( matcher_hand.find(pos)){
			sf1 = matcher_hand.group() ;
			if1 = s.indexOf(sf1,pos) ;
			if( if1>=pos){
				strr += s.substring(pos, if1) ;
				pos = if1 + sf1.length() ;
				sf2 = pb ;
				for( int i = 1 ; i<=gc ; i++ ){
					sf3 = "\\"+i ;
					sf2 = replaceAll( sf2, sf3 , matcher_hand.group(i)) ;
				}
				strr += sf2 ;
			}else{
				return s ;
			}
		}
		strr = s.substring(0, start) + strr;
		return strr ;
	}
	/**
	 * ���ı��滻
	 * @param s Դ�ַ�
	 * @param sf ���ַ�
	 * @param sb �滻�ַ�
	 * @return �滻����ַ�
	 */
	public static String replaceAll( String s , String sf , String sb ){
		int i = 0 , j = 0 ;
		int l = sf.length() ;
		boolean b = true ;
		boolean o = true ;
		String str = "" ;
		do{
			j=i ;
			i=s.indexOf(sf,j);
			if( i>j){
				str+=s.substring(j, i) ;
				str+=sb ;
				i+=l ;
				o = false ;
			}else{
				str+=s.substring(j) ;
				b=false ;
			}
		}while( b ) ;
		if( o ){
			str=s ;
		}
		return str ;
	}
	/**
	 * �ж��Ƿ�����ַ���ʽƥ��
	 * @param str �ַ�
	 * @param pattern ������ʽ��ʽ
	 * @return �Ƿ�ƥ����true,��false
	 */
	public static boolean isMatch( String str , String pattern ){
		Pattern pattern_hand = Pattern.compile(pattern);
		Matcher matcher_hand = pattern_hand.matcher( str );
		boolean b = matcher_hand.matches();
		return b ;
	}
	
	/**
	 * ��ȡ�ַ�
	 * @param s Դ�ַ�
	 * @param jmp ���jmp
	 * @param sb ȡ��sb
	 * @param se ��se
	 * @return ֮����ַ�
	 */
	public static String subStringExe( String s , String jmp , String sb , String se ){
		if( isEmpty( s )){
			return "" ;
		}
		int i = s.indexOf( jmp ) ;
		if( i>=0 && i<s.length()) {
			s = s.substring( i+1 ) ;
		}
		i = s.indexOf( sb ) ;
		if( i>=0 && i<s.length()){
			s = s.substring( i+1 ) ;
		}
		if( se == "" ){
			return s ;
		}else{
			i = s.indexOf( se ) ;
			if( i>= 0 && i<s.length()){
				s = s.substring( i+1 ) ;
			}
			return s ;
		}
	}
	/**
	 * *************************************************************************
	 * ��Ҫͨ��URL��������ݽ��б���
	 * @author ������
	 * @param Դ�ַ�
	 * @return ������������
	 *************************************************************************
	 */
	public static String URLEncode(String src){
		String return_value = "";
		try {
			if (src != null)
			{
				return_value = URLEncoder.encode(src, "GBK");
				
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return_value = src;
		}
		
		return return_value;
	}
	
	
	
	
	
	
	/**
	 * *************************************************************************
	 * @author ���  2007.4.18
	 * @param ����&#31119;test&#29031;&#27004;&#65288;&#21271;&#22823;&#38376;&#24635;&#24215;&#65289;&#31119;
	 * @return ������������
	 *************************************************************************
	 */
	public static String getGBK(String str){
		
		return transfer(str);
	}
	public static String transfer(String str) {
        Pattern p = Pattern.compile("&#\\d+;");
        Matcher m = p.matcher(str);
        while (m.find()) {
            String old = m.group();
            str = str.replaceAll(old, getChar(old));
        }
        return str;
    }
    public static String getChar(String str) {
        String dest = str.substring(2, str.length() - 1);
        char ch = (char) Integer.parseInt(dest);
        return "" + ch;
    }
	
	
	
	
	
	
	
}

