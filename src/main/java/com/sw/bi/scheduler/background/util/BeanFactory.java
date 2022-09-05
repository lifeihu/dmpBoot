package com.sw.bi.scheduler.background.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.sms.SmsService;

@SuppressWarnings("rawtypes")
public class BeanFactory {
	private static ApplicationContext ctx;
	private static Properties props;

	static {
		if (ctx == null) {
			ctx = new ClassPathXmlApplicationContext(new String[] { "com/sw/bi/scheduler/background/config/applicationContext-scheduler-background.xml" });
		}

		if (props == null) {
			props = new Properties();
			try {
				props.load(BeanFactory.class.getResourceAsStream("/database-scheduler.properties"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static <T extends GenericService> T getService(Class<T> kind) {
		return ctx.getBean(kind);
	}

	public static <T extends Object> T getBean(Class<T> kind) {
		return ctx.getBean(kind);
	}

	/**
	 * <pre>
	 * 	通过注入方式获得SmsService时在只起调度系统一个项目时没什么问题
	 *  但是当于监控平台一起启动时会报错,原因仍没查出，但是可以通过非
	 *  注入的方式实现，在需要发送短信时通过BeanFactory.getSmsService()
	 *  方法得到SmsService对象，然后再使用
	 *  TODO 等调度六期时需要都改一下(参考com.sw.bi.scheduler.background.javatype.check.CheckTest)
	 * </pre>
	 * 
	 * @return
	 */
	@Deprecated
	public static SmsService getSmsService() {
		return ctx.getBean(SmsService.class);
	}

	public static Connection connection() {
		Connection connection = null;

		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(props.getProperty("scheduler.database.connection.url"), props.getProperty("scheduler.database.connection.username"), props
					.getProperty("scheduler.database.connection.password"));

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return connection;
	}
}
