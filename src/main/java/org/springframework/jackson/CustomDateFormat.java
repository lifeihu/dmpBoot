package org.springframework.jackson;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CustomDateFormat extends DateFormat {

	private static final long serialVersionUID = -3161942617506568819L;

	private static Map<String, DateFormat> formats = new HashMap<String, DateFormat>();

	private static final CustomDateFormat instance = new CustomDateFormat();

	private CustomDateFormat() {
		this.setPatterns(new String[] {
				"yyyy-M-d", "yyyy-M-d HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"
		});
	}

	public static CustomDateFormat getDateFormat() {
		return instance;
	}

	@Override
	public Object clone() {
		return new CustomDateFormat();
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		StringBuffer result = null;

		for (DateFormat format : formats.values()) {
			try {
				result = format.format(date, toAppendTo, fieldPosition);

				if (result != null) {
					break;
				}

			} catch (Exception e) {
				continue;
			}
		}

		return result;
	}

	@Override
	public Date parse(String source, ParsePosition pos) {
		Date date = null;

		for (DateFormat format : formats.values()) {
			try {
				date = format.parse(source, pos);

				if (date != null) {
					break;
				}

			} catch (Exception e) {
				continue;
			}
		}

		return date;
	}

	public void setPatterns(String[] patterns) {
		for (String pattern : patterns) {
			if (!formats.containsKey(pattern)) {
				formats.put(pattern, new SimpleDateFormat(pattern));
			}
		}
	}

}
