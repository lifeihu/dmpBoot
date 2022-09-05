package org.springframework.resolver;

/**
 * 该类用于前台Ext的异常捕获(一般开发过程能预料到的错误，或有意需要提醒时用到该类)
 * 
 * @author shiming.hong
 */
public class Warning extends RuntimeException {
	private static final long serialVersionUID = 3050215158344673802L;

	public Warning(String msg) {
		super(msg);
	}
}
