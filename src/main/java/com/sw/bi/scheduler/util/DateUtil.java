package com.sw.bi.scheduler.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public static Date now() {
		return Calendar.getInstance().getTime();
	}

	public static Date getToday() {
		return DateUtil.clearTime(new Date());
	}

	public static Calendar getTodayCalendar() {
		return getCalendar(getToday());
	}

	public static Date getYesterday() {
		return clearTime(getYesterdayCalendar().getTime());
	}

	public static Date getYesterday(Date date) {
		Calendar yesterdayCalendar = getCalendar(date);
		yesterdayCalendar.add(Calendar.DATE, -1);

		return clearTime(yesterdayCalendar.getTime());
	}

	public static Calendar getYesterdayCalendar() {
		Calendar yesterdayCalendar = cloneCalendar();
		yesterdayCalendar.add(Calendar.DATE, -1);
		yesterdayCalendar.set(Calendar.HOUR_OF_DAY, 0);
		yesterdayCalendar.set(Calendar.MINUTE, 0);
		yesterdayCalendar.set(Calendar.SECOND, 0);
		yesterdayCalendar.set(Calendar.MILLISECOND, 0);

		return yesterdayCalendar;
	}

	public static Date getTomorrow() {
		return clearTime(getTomorrowCalendar().getTime());
	}

	public static Date getTomorrow(Date date) {
		Calendar tomorrowCalendar = getCalendar(date);
		tomorrowCalendar.add(Calendar.DATE, 1);

		return clearTime(tomorrowCalendar.getTime());
	}

	public static Calendar getTomorrowCalendar() {
		Calendar tomorrowCalendar = cloneCalendar();
		tomorrowCalendar.add(Calendar.DATE, 1);
		tomorrowCalendar.set(Calendar.HOUR_OF_DAY, 0);
		tomorrowCalendar.set(Calendar.MINUTE, 0);
		tomorrowCalendar.set(Calendar.SECOND, 0);
		tomorrowCalendar.set(Calendar.MILLISECOND, 0);

		return tomorrowCalendar;
	}

	public static String formatDate(Date date) {
		return format(date, DATE_FORMAT);
	}

	public static String formatDateTime(Date date) {
		return format(date, DATE_TIME_FORMAT);
	}

	public static String format(Date date, String pattern) {
		return format(date, new SimpleDateFormat(pattern));
	}

	public static String format(Date date, SimpleDateFormat format) {
		return format.format(date);
	}

	public static Date parseDate(String text) {
		return parse(text, DATE_FORMAT);
	}

	public static Date parseDateTime(String text) {
		return parse(text, DATE_TIME_FORMAT);
	}

	public static Date parse(String text, String pattern) {
		return parse(text, new SimpleDateFormat(pattern));
	}

	public static Date parse(String text, SimpleDateFormat format) {
		try {
			return format.parse(text);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Date clearTime(Date date) {
		Calendar calendar = DateUtil.getCalendar(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar.getTime();
	}

	public static Calendar cloneCalendar() {
		return (Calendar) Calendar.getInstance().clone();
	}

	public static Calendar cloneCalendar(Calendar calendar) {
		return (Calendar) calendar.clone();
	}

	public static Calendar getCalendar(Date date) {
		Calendar calendar = cloneCalendar();
		calendar.setTime(date);

		return calendar;
	}

	public static Calendar getCalendar(Date date, boolean clearTime) {
		Calendar calendar = getCalendar(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar;
	}

	// strBeginDate必须是yyyyMMdd格式
	public static String getDay(String strBeginDate, int n) {
		Calendar calendar = getCalendar(parse(strBeginDate, "yyyyMMdd"));
		calendar.add(Calendar.DATE, n);

		return format(calendar.getTime(), "yyyyMMdd");
	}

	/**
	 * 校验当前时间是否指定时间范围内(时间格式: HH:mm)
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public static boolean isTimeRange(String startTime, String endTime) {
		if (startTime == null || startTime.length() == 0) {
			throw new IllegalArgumentException("开始时间不允许为空");
		}

		if (endTime == null || endTime.length() == 0) {
			throw new IllegalArgumentException("结束时间不允许为空");
		}

		Calendar calendar = DateUtil.getCalendar(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime.substring(0, 2)));
		calendar.set(Calendar.MINUTE, Integer.parseInt(startTime.substring(3)));
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		long start = calendar.getTimeInMillis();
		///System.out.println(DateUtil.formatDateTime(calendar.getTime()));

		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime.substring(0, 2)));
		calendar.set(Calendar.MINUTE, Integer.parseInt(endTime.substring(3)));
		long end = calendar.getTimeInMillis();

		// 传入的时间有可能开始时间大于结束时间,这里结束时间默认需要加一天
		// 如: 22:00 09:00，这表示从今天晚上22点到第二天早上9点
		if (start > end) {
			calendar.add(Calendar.DATE, 1);
			end = calendar.getTimeInMillis();
		}
		///System.out.println(DateUtil.formatDateTime(calendar.getTime()));

		long now = System.currentTimeMillis();

		return now >= start && now <= end;
	}
	
	


}
