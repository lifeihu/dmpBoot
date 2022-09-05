package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Server;
import com.sw.bi.scheduler.model.ServerOperate;
import com.sw.bi.scheduler.model.ServerShell;
import com.sw.bi.scheduler.model.ServerUser;
import com.sw.bi.scheduler.service.ServerOperateService;
import com.sw.bi.scheduler.service.ServerService;
import com.sw.bi.scheduler.service.ServerShellService;
import com.sw.bi.scheduler.service.ServerUserService;
import com.sw.bi.scheduler.util.Configure.ServerOperateStatus;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ServerOperateServiceImpl extends GenericServiceHibernateSupport<ServerOperate> implements ServerOperateService {

	@Autowired
	private ServerService serverService;

	@Autowired
	private ServerUserService serverUserService;

	@Autowired
	private ServerShellService serverShellService;

	@Override
	public void online(long serverOperateId) {
		ServerOperate serverOperate = this.get(serverOperateId);

		if (serverOperate == null) {
			throw new Warning("未找到指定服务器操作.");
		}

		serverOperate.setStatus(ServerOperateStatus.ON_LINE.indexOf());
		super.update(serverOperate);
	}

	@Override
	public void offline(long serverOperateId) {
		ServerOperate serverOperate = this.get(serverOperateId);

		if (serverOperate == null) {
			throw new Warning("未找到指定服务器操作.");
		}

		serverOperate.setStatus(ServerOperateStatus.OFF_LINE.indexOf());
		super.update(serverOperate);
	}

	@Override
	public Map<String, String> execute(long serverOperateId) {
		ServerOperate serverOperate = this.get(serverOperateId);

		if (serverOperate == null) {
			throw new Warning("未找到指定服务器操作.");
		}

		ServerUser serverUser = serverUserService.get(serverOperate.getServerUserId());
		ServerShell serverShell = serverShellService.get(serverOperate.getServerShellId());

		boolean showResult = serverOperate.isShowResult();
		Long[] serverIds = serverOperate.getServerIds();
		String shellPath = serverShell.getPath();
		String shellCommand = serverShell.getCommand();
		String username = serverUser.getUsername();
		String password = null;
		try {
			password = new DxDESCipher().decrypt(serverUser.getPassword());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 多线程执行服务器操作

		// 执行线程池
		ExecutorService serverOperateExecutorService = Executors.newFixedThreadPool(serverIds.length);

		CountDownLatch serverOperateCountDown = new CountDownLatch(serverIds.length);

		try {
			Map<String, String> results = new HashMap<String, String>();

			for (Long serverId : serverIds) {
				Server server = serverService.get(serverId);
				// 这里需要给服务器名加上前缀,因为操作执行完后会删除该服务器的连接
				// 如果服务器名与网关机名称相同时就会影响到网关机有关的操作(目前已发现的有Hudson打包)
				String serverName = "operate-" + server.getName();
				String serverIp = server.getIp();

				SshUtil.registerAgent(serverName, serverIp, 22, username, password);

				ServerOperateCallable callable = new ServerOperateCallable(serverName, shellPath, shellCommand, showResult, serverOperateCountDown);
				Future<String> future = serverOperateExecutorService.submit(callable);

				synchronized (results) {
					String result = future.get();

					if (showResult) {
						results.put(serverIp, result);
					} else {
						if (StringUtils.hasText(result)) {
							results.put(serverIp, result);
						}
					}
				}
			}

			serverOperateCountDown.await();

			return results;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!serverOperateExecutorService.isShutdown()) {
				serverOperateExecutorService.shutdown();
			}
		}

		return null;
	}

	private class ServerOperateCallable implements Callable<String> {

		private String serverName;
		private String shellPath;
		private String shellCommand;
		private boolean showResult;
		private CountDownLatch serverOperateCountDown;

		public ServerOperateCallable(String serverName, String shellPath, String shellCommand, boolean showResult, CountDownLatch serverOperateCountDown) {
			this.serverName = serverName;
			this.shellPath = shellPath;
			this.shellCommand = shellCommand;
			this.showResult = showResult;
			this.serverOperateCountDown = serverOperateCountDown;
		}

		@Override
		public String call() throws Exception {
			ExecResult execResult = null;

			try {
				if (StringUtils.hasText(shellPath)) {
					execResult = SshUtil.execCommand(serverName, "/bin/bash " + shellPath);

				} else {
					String command = shellCommand.replaceAll("\n", " ");
					execResult = SshUtil.execCommand(serverName, "source /etc/profile && " + command);

					/*String[] commands = shellCommand.split("&&");
					for (int i = 0, len = commands.length; i < len; i++) {
						commands[i] = "source /etc/profile && " + commands[i].trim();
					}

					execResult = SshUtil.execCommand(serverName, commands);

					if (execResult.failure()) {
						return execResult.getStderr();

					} else {
						StringBuilder stdout = new StringBuilder();

						for (ExecResult er : execResult.getExecResults()) {
							stdout.append(er.getCommand()).append("\n");
							stdout.append(er.getStdout()).append("\n");
						}

						return stdout.toString();
					}*/
				}

				if (execResult.success()) {
					return showResult ? execResult.getStdout() : null;
				} else {
					return execResult.getStderr();
				}

			} finally {
				SshUtil.unregisterAgent(serverName);
				serverOperateCountDown.countDown();
			}
		}

	}

}
