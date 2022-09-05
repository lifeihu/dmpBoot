package com.sw.bi.scheduler.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.util.ExecAgent.ExecResult;

/**
 * SSH工具类
 * 
 * @author shiming.hong
 * 
 */
public class SshUtil {
	private static final Logger log = Logger.getLogger(SshUtil.class);
	private static final Map<String, ExecAgent> EXEC_AGENTS = new HashMap<String, ExecAgent>();

	public static final String DEFAULT_GATEWAY = "localhost";
	public static final int DEFAULT_GATEWAY_PORT = 22;
	private static final String DEFAULT_USERNAME = "tools";
	private static final String DEFAULT_PASSWORD = "tools";   //这里是调度网关机的tools账号的密码，如果修改过了，这里也要修改
//	private static final String DEFAULT_PASSWORD = "adtimetools54";   //这里是调度网关机的tools账号的密码，如果修改过了，这里也要修改
	//private static final String DEFAULT_PASSWORD = "zjmoadtimetools123";   //这里是调度网关机的tools账号的密码，如果修改过了，这里也要修改
	static {
		registerAgent(DEFAULT_GATEWAY, DEFAULT_GATEWAY, 22);
	}

	/**
	 * 注册网关机代理
	 * 
	 * @param gateway
	 * @param hostname
	 * @param port
	 */
	public static void registerAgent(String gateway, String hostname, int port) {
		registerAgent(gateway, hostname, port, DEFAULT_USERNAME, DEFAULT_PASSWORD);
	}

	/**
	 * 注册网关机代理
	 * 
	 * @param gateway
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 */
	public static void registerAgent(String gateway, String hostname, int port, String username, String password) {
		if (!EXEC_AGENTS.containsKey(gateway)) {
			EXEC_AGENTS.put(gateway, new ExecAgent(hostname, port, username, password));
		}
	}

	/**
	 * 注销网关机代理
	 * 
	 * @param gateway
	 */
	public static void unregisterAgent(String gateway) {
		if (StringUtils.hasText(gateway)) {
			ExecAgent agent = EXEC_AGENTS.get(gateway);
			if (agent != null) {
				agent.close();
				EXEC_AGENTS.remove(gateway);
			}
		}
	}

	/**
	 * 关闭与指定网关机的连接
	 * 
	 * @param gateway
	 */
	public static void close(String gateway) {
		if (StringUtils.hasText(gateway)) {
			ExecAgent agent = EXEC_AGENTS.get(gateway);
			if (agent != null) {
				agent.close();
			}
		}
	}

	/**
	 * 在指定网关机上执行指定命令
	 * 
	 * @param gateway
	 * @param command
	 * @return
	 */
	public static ExecResult execCommand(String gateway, String command) {
		if (StringUtils.hasText(command)) {
			return execCommand(gateway, new String[] { command });
		}

		ExecResult execResult = new ExecResult();
		execResult.setStderr("没有指定需要执行的命令.");
		return execResult;
	}

	/**
	 * 在指定网关机上执行指定命令并关闭连接
	 * 
	 * @param gateway
	 * @param command
	 * @return
	 */
	public static ExecResult execCommandAndDisconnect(String gateway, String command) {
		if (StringUtils.hasText(command)) {
			return execCommandAndDisconnect(gateway, new String[] { command });
		}

		ExecResult execResult = new ExecResult();
		execResult.setStderr("没有指定需要执行的命令.");
		return execResult;
	}

	/**
	 * 在指定网关机上执行指定命令
	 * 
	 * @param gateway
	 * @param commands
	 * @return
	 */
	public static ExecResult execCommand(String gateway, String[] commands) {
		return execCommand(gateway, commands, false);
	}

	/**
	 * 在指定网关机上执行指定命令并关闭连接
	 * 
	 * @param gateway
	 * @param commands
	 * @return
	 */
	@Deprecated
	public static ExecResult execCommandAndDisconnect(String gateway, String[] commands) {
		return execCommand(gateway, commands, true);
	}

	/**
	 * 执行Hadoop命令
	 * 
	 * @param gateway
	 * @param command
	 * @return
	 */
	public static ExecResult execHadoopCommand(String gateway, String command) {
		if (StringUtils.hasText(command)) {
			return execHadoopCommand(gateway, new String[] { command });
		}

		ExecResult execResult = new ExecResult();
		execResult.setStderr("没有指定需要执行的Hadoop命令.");
		return execResult;
	}

	/**
	 * 执行Hadoop命令并关闭连接
	 * 
	 * @param gateway
	 * @param command
	 * @return
	 */
	@Deprecated
	public static ExecResult execHadoopCommandAndDisconnect(String gateway, String command) {
		if (StringUtils.hasText(command)) {
			return execHadoopCommandAndDisconnect(gateway, new String[] { command });
		}

		ExecResult execResult = new ExecResult();
		execResult.setStderr("没有指定需要执行的Hadoop命令.");
		return execResult;
	}

	/**
	 * 批量执行Hadoop命令
	 * 
	 * @param gateway
	 * @param commands
	 * @return
	 */
	public static ExecResult execHadoopCommand(String gateway, String[] commands) {
		if (commands == null || commands.length == 0) {
			ExecResult execResult = new ExecResult();
			execResult.setStderr("没有指定需要执行的Hadoop命令.");
			return execResult;
		}

		for (int i = 0, len = commands.length; i < len; i++) {
			commands[i] = Configure.property(Configure.HADOOP_HOME) + "bin/hadoop fs -" + commands[i];
		}

		return execCommand(gateway, commands, false);
	}

	/**
	 * 批量执行Hadoop命令并关闭连接
	 * 
	 * @param gateway
	 * @param commands
	 * @return
	 */
	public static ExecResult execHadoopCommandAndDisconnect(String gateway, String[] commands) {
		if (commands == null || commands.length == 0) {
			ExecResult execResult = new ExecResult();
			execResult.setStderr("没有指定需要执行的Hadoop命令.");
			return execResult;
		}

		for (int i = 0, len = commands.length; i < len; i++) {
			commands[i] = Configure.property(Configure.HADOOP_HOME) + "bin/hadoop fs -" + commands[i];
		}

		return execCommand(gateway, commands, true);
	}

	/**
	 * 在指定网关机上执行指定命令
	 * 
	 * @param gateway
	 * @param commands
	 * @param autoDisconnect
	 *            执行完毕后是否自动关闭连接
	 */
	private static ExecResult execCommand(String gateway, String[] commands, boolean autoDisconnect) {
		ExecResult execResult = new ExecResult();
		autoDisconnect = true; // 强制关闭连接

		ExecAgent agent = EXEC_AGENTS.get(gateway);

		if (agent == null) {
			execResult.setStderr("没有找到网关机(" + gateway + ")的执行代理器.");
			return execResult;
		}

		agent = new ExecAgent(agent.getHostname(), agent.getPort(), agent.getUsername(), agent.getPassword());
		Collection<ExecResult> results = agent.execCommand(commands);

		if (results.size() > 1) {
			execResult.setExecResults(results);

			// 如果有多个执行结果时则需要判断每个结果的返回值
			for (ExecResult result : results) {
				execResult.setExitValue(result.getExitValue());

				// 执行命令结果中只要有一个没成功则就认为此次执行失败
				if (execResult.failure()) {
					break;
				}
			}

		} else if (results.size() == 1) {
			// 只有一个执行结果时则直接将其返回
			execResult = results.iterator().next();

		}

		// 关闭连接
		if (autoDisconnect) {
			agent.close();
		}

		return execResult;
	}

	public static void main(String[] args) {
		SshUtil.registerAgent("datanode2", "192.168.181.209", 22, "root", "abc#123");

		long start = System.currentTimeMillis();
		ExecResult result = SshUtil.execHadoopCommandAndDisconnect("datanode2", "cat /group/user/etl/test1/11.txt");

		if (result.success()) {
			log.info(result.getStdout().length());
		} else {
			log.info(result.getStderr());
		}

		/*ExecResult */result = SshUtil.execHadoopCommand("datanode2", "test -d /group/user1");
		log.info(result.success());
		log.info(result.getExitValue());
		log.info(result.getStdout());
		log.info(result.getStderr());

		log.info("总耗时: " + (System.currentTimeMillis() - start) / 1000 + "s.");
	}
}