package com.sw.bi.scheduler.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

/**
 * 执行代理
 * 
 * @author shiming.hong
 */
public class ExecAgent {
	private static final Logger log = Logger.getLogger(ExecAgent.class);

	private String hostname;
	private int port;
	private String username;
	private String password;
	private Connection connection;

	private boolean connecting = false;

	public ExecAgent(String hostname, String username, String password) {
		this(hostname, 22, username, password);
	}

	public ExecAgent(String hostname, int port, String username, String password) {
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.password = password;
		// this.connection = new Connection(this.hostname, this.port);
	}

	/**
	 * 建立与网关机连接
	 * 
	 * @return
	 * @throws IOException
	 */
	public synchronized boolean connect() {
		if (!connecting) {
			try {
				connection = new Connection(this.hostname, this.port);

				connection.connect();
				connecting = connection.authenticateWithPassword(username, password);
				log.info("ssh connected: " + hostname + " - " + username);

				if (!connecting) {
					log.warn("登录用户验证失败.");
				}

				return connecting;

			} catch (IOException e) {
				log.warn("创建网关机连接失败.", e);
				return false;
			}
		}

		return true;
	}

	/**
	 * 断开与网关机的连接
	 */
	public void close() {
		if (connection != null) {
			connection.close();
			connection = null;

			log.info("ssh disconnected: " + hostname + " - " + username);
		}

		connecting = false;
	}

	/**
	 * 批量执行指定命令
	 * 
	 * @param commands
	 * @return
	 */
	public Collection<ExecResult> execCommand(String[] commands) {
		Collection<ExecResult> execResults = new ArrayList<ExecResult>(commands.length);

		if (commands == null || commands.length == 0) {
			log.warn("没有指定需要执行的命令.");

			ExecResult execResult = new ExecResult();
			execResult.setStderr("没有指定需要执行的Hadoop命令.");
			execResults.add(execResult);

			return execResults;
		}

		if (!this.connect()) {
			ExecResult execResult = new ExecResult();
			execResult.setStderr("与网关机(" + hostname + ")建立连接失败,请检查主机IP、用户或密码是否输入正确.");
			execResults.add(execResult);

			return execResults;
		}

		Session session = null;
		boolean success = true;
		for (int i = 0, len = commands.length; i < len; i++) {
			String command = commands[i];

			try {
				ExecResult execResult = new ExecResult();
				execResults.add(execResult);

				execResult.setCommand(command);

				// 只有是成功状态时会执行命令，如果有一个执行失败则后面的命令都默认不执行了以失败状态返回
				if (success) {
					session = this.openSession(execResults);

					// Session没打开则忽略
					if (session == null) {
						log.warn("网关机(" + hostname + ")打开Session失败.");
						success = false;
						continue;
					}

					session.execCommand(command);
					session.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.TIMEOUT, 30000);

					execResult.setStdout(IOUtils.toString(new StreamGobbler(session.getStdout())));
					execResult.setStderr(IOUtils.toString(new StreamGobbler(session.getStderr())));
					execResult.setExitValue(session.getExitStatus());

					if (execResult.getExitValue() == 0) {
						continue;

					} else {
						success = false;
						log.warn("命令(" + command + ")执行失败: " + execResult.getStderr());
					}

				} else {
					execResult.setStderr("命令(" + command + ")被忽略执行.");

				}

			} catch (IOException e) {
				log.warn("命令(" + command + ")执行异常.", e);

			} finally {
				if (session != null) {
					session.close();
				}
			}
		}

		return execResults;
	}

	/**
	 * 打开Session
	 * 
	 * @return
	 */
	private Session openSession(Collection<ExecResult> execResults) {
		Session session = null;
		try {
			session = connection.openSession();

		} catch (Exception e) {
			boolean reconect = this.connecting == false || connection == null || e.getMessage().indexOf("this connection is closed") > -1;

			if (reconect) {
				this.close();

				if (!this.connect()) {
					ExecResult execResult = new ExecResult();
					execResult.setStderr("与网关机(" + hostname + ")建立连接失败,请检查主机IP、用户或密码是否输入正确.");
					execResults.add(execResult);
				}

				try {
					session = connection.openSession();

				} catch (IOException e1) {}

			} else {
				e.printStackTrace();
			}
		}

		if (session == null) {
			ExecResult execResult = new ExecResult();
			execResult.setStderr("网关机(" + hostname + ")打开Session失败.");
			execResults.add(execResult);
		}

		return session;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * 命令执行结果
	 * 
	 * @author shiming.hong
	 * 
	 */
	public static class ExecResult {

		private String command;
		private String stdout;
		private String stderr;
		private int exitValue = -1;

		private Collection<ExecResult> execResults = new ArrayList<ExecResult>();

		/**
		 * 执行成功
		 * 
		 * @return
		 */
		public boolean success() {
			boolean success = (exitValue == 0 && !StringUtils.hasText(getStderr()));

			if (!success && StringUtils.hasText(stderr)) {
				// 如果错误信息中带有“is deprecated”时则认为此次结果是正确的
				success = stderr.indexOf("is deprecated") > -1;
			}

			return success;
		}

		public boolean failure() {
			boolean failure = (exitValue != 0 || StringUtils.hasText(getStderr()));

			if (failure && StringUtils.hasText(stderr)) {
				// 如果错误信息中带有“is deprecated”时则不认为此次结果是失败的
				failure = stderr.indexOf("is deprecated") == -1;
			}

			return failure;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public String[] getStdoutAsArrays() {
			return StringUtils.hasText(stdout) ? stdout.split("\n") : null;
		}

		public String getStdout() {
			return stdout;
		}

		public boolean isEmptyStdout() {
			return stdout == null || !StringUtils.hasText(stdout);
		}

		public String getStderr() {
			return stderr;
		}

		public void setStdout(String stdout) {
			this.stdout = stdout;
		}

		public void setStderr(String stderr) {
			this.stderr = stderr;
		}

		public void setExitValue(Integer exitValue) {
			this.exitValue = exitValue == null ? 0 : exitValue.intValue();
		}

		public int getExitValue() {
			return exitValue;
		}

		public Collection<ExecResult> getExecResults() {
			return execResults;
		}

		public void setExecResults(Collection<ExecResult> execResults) {
			this.execResults = execResults;
		}

	}
}
