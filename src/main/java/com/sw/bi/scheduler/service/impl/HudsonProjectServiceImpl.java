package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.HudsonProject;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.service.HudsonProjectService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserGroupService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.HudsonPublishStatus;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.ResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.io.*;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class HudsonProjectServiceImpl extends GenericServiceHibernateSupport<HudsonProject> implements HudsonProjectService {
	private static final Logger log = Logger.getLogger(HudsonProjectServiceImpl.class);

	private String sourcePath = Configure.property(Configure.HUDSON_SYNC_SOURCE_PATH);
	private String sourceUsername = Configure.property(Configure.HUDSON_SYNC_SOURCE_PATH_USERNAME);
	private String sourcePassword = Configure.property(Configure.HUDSON_SYNC_SOURCE_PATH_PASSWORD);

	private Collection<File> targetPaths = new HashSet<File>();
	private File tempPath;

	private Writer logger;

	@Autowired
	private UserGroupService userGroupService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Override
	public void publish(long hudsonProjectId) {
		String uuid = hudsonProjectId + "_" + UUID.randomUUID().toString();
		this.initLogger(uuid);

		HudsonProject hp = this.get(hudsonProjectId);
		log("开始发布 \"" + hp.getName() + "\" 项目.");

		HudsonPublishStatus status = HudsonPublishStatus.PUBLISH_SUCCESS;

		try {
			this.startPublish(uuid, hp);

			File latestVersion = this.downloadLatestVersion(hp);
			if (latestVersion == null) {
				status = HudsonPublishStatus.PUBLISH_FAILURE;
				log("SVN上未找到项目(" + hp.getName() + ")的最新版本,可以检查一下SVN服务器是否断开."); //可能是因为与hundson打包服务器的IP不连通
				return;
			}

			if (latestVersion.getName().endsWith(".zip")) {
				// 对于Zip文件需要先解压后发布
				// 解压缩指定文件
				File unzipDirectory = this.unzip(latestVersion);
				if (unzipDirectory == null) {
					status = HudsonPublishStatus.PUBLISH_FAILURE;
					log(latestVersion.getAbsolutePath() + " 解压失败.");
					return;
				}

				this.publishTarget(hp, unzipDirectory);

			} else {
				// 对于Jar文件直接发布
				this.publishTarget(hp, latestVersion);
			}

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log(sw.toString());

			status = HudsonPublishStatus.PUBLISH_FAILURE;

		} finally {
			this.finishPublish(hp, status);
		}

	}

	@Override
	public String getPublishLog(String logFile) {
		File logger = new File(tempPath, logFile);

		if (!logger.exists()) {
			return "日志文件(" + logger.getAbsolutePath() + ")不存在.";
		}

		try {
			return IOUtils.toString(new InputStreamReader(new FileInputStream(logger), "utf-8"));
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	/**
	 * 初始化日志文件
	 * 
	 * @param uuid
	 */
	private void initLogger(String uuid) {
		try {
			File logFile = new File(tempPath, uuid + ".log");
			if (!logFile.exists()) {
				logFile.createNewFile();
				log.info("创建Hudson发布的日志文件(" + uuid + ".log).");
			}

			logger = new OutputStreamWriter(new FileOutputStream(logFile), "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 更新项目发布信息
	 * 
	 * @param uuid
	 * @param hp
	 */
	private void startPublish(String uuid, HudsonProject hp) {
		hp.setPublishLogFile(uuid + ".log");
		hp.setPublishStartTime(DateUtil.now());
		hp.setPublishEndTime(null);
		hp.setCreateTime(DateUtil.now());
		hp.setPublishStatus(HudsonPublishStatus.PUBLISHING.ordinal());
		getHibernateTemplate().update(hp);
		this.flush();

		// 安装一个默认的认证用于访问需要认证的URL
		Authenticator.setDefault(new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(sourceUsername, sourcePassword.toCharArray());
			}

		});
	}

	/**
	 * 获得指定项目在svn上的最新版本
	 * 
	 * @param hp
	 * @return
	 * @throws MalformedURLException
	 */
	private File downloadLatestVersion(HudsonProject hp) {
		log("正在从SVN服务器(" + sourcePath + ")上更新项目(" + hp.getName() + ")的最新版本...");

		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;

		try {
			String sourceUrl = this.sourcePath + hp.getSvnPath().replaceFirst("^/", "");
			URL latestVersionUrl = getLatestVersionURL(sourceUrl);
			if (latestVersionUrl == null) {
				log.error("未找到最新版本的文件.");
				return null;
			}

			bis = new BufferedInputStream(latestVersionUrl.openStream());

			String fileName = latestVersionUrl.getFile();
			File file = new File(tempPath, fileName.substring(fileName.lastIndexOf("/") + 1));
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();

			bos = new BufferedOutputStream(new FileOutputStream(file));
			IOUtils.copy(bis, bos);
			bos.close();

			// log.info("download " + file + " success.");
			log("成功下载项目 \"" + hp.getName() + "\" 的最新版本(" + file.getAbsolutePath() + ").");

			return file;

		} catch (Exception e) {
			log.error("hudson sync execption.", e);

		} finally {
			try {
				if (bis != null) {
					bis.close();
				}

				if (bos != null) {
					bos.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * 获得最近一个版本的URL地址 <dir name="20120524" href="20120524/" /> <dir
	 * name="20120525" href="20120525/" />
	 * 
	 * <file name="package-checksums.md5" href="package-checksums.md5" /> <file
	 * name="sw_udf-2345.jar" href="sw_udf-2345.jar" />
	 * 
	 * @param url
	 */
	private URL getLatestVersionURL(String url) {
		InputStreamReader isr = null;
		BufferedReader reader = null;
		try {
			isr = new InputStreamReader(new URL(url).openStream());
			reader = new BufferedReader(isr);
			String line = null;
			long maxPath = 0l;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf("<dir ") > -1) {
					int pos = line.indexOf("\"") + 1;
					char[] chars = line.toCharArray();
					String path = "";
					for (int i = pos; i < chars.length; i++) {
						if (chars[i] == '"') {
							break;
						} else {
							path += chars[i];
						}
					}
					//截取出20120524  赋值给maxPath  下次再截取出20120525  把两者中大的赋值给maxPath
					maxPath = Math.max(maxPath, Long.parseLong(path));

				} else if (line.indexOf("<file ") > -1 && (line.indexOf(".zip") > -1 || line.indexOf(".jar") > -1)) {
					int pos = line.indexOf("\"") + 1;
					char[] chars = line.toCharArray();
					String path = "";
					for (int i = pos; i < chars.length; i++) {
						if (chars[i] == '"') {
							break;
						} else {
							path += chars[i];
						}
					}
					//上面解析出path= sw_udf-2345.jar   所以返回new URL(url + "/" + path)
					//返回最新的jar包完整的url地址
					return new URL(url + "/" + path);
				}
			}

			return getLatestVersionURL(url + "/" + maxPath); //递归下去

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				if (reader != null) {
					reader.close();
				}

				if (isr != null) {
					isr.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * 解压缩指定文件
	 * 
	 * @param file
	 */
	private File unzip(File file) {
		if (file == null) {
			return null;
		}

		log("正在解压项目最新版本(" + file.getAbsolutePath() + ")...");

		// http://192.168.17.243/svn/bi/daijin/etl-daijin/20120525/20120525142129/daijin-2346-sql.zip
		// daijin-2346-sql
		String zipFileName = file.getName().substring(file.getName().lastIndexOf("/") + 1, file.getName().lastIndexOf("."));
		File targetDirectory = new File(file.getParentFile(), zipFileName);
		if (targetDirectory.exists()) {
			targetDirectory.delete();
		}
		targetDirectory.mkdirs(); //建立daijin-2346-sql目录

		FileInputStream fis = null;
		ZipInputStream zis = null;

		try {
			fis = new FileInputStream(file);
			zis = new ZipInputStream(fis);
			ZipEntry entry = null;

			while ((entry = zis.getNextEntry()) != null) {
				String entryName = entry.getName();

				//过滤掉无用,多余的目录和文件
				if ("target/".equals(entryName) || "hudsonBuild.properties".equals(entryName)) {
					continue;
				}

				if (entry.isDirectory()) {
					File entryDirectory = new File(targetDirectory, entry.getName());
					if (!entryDirectory.exists()) {
						entryDirectory.mkdirs();
					}

					continue;
				}

				File entryFile = new File(targetDirectory, entry.getName());
				if (!entryFile.exists()) {
					entryFile.delete();
				}
				entryFile.createNewFile();

				FileOutputStream fos = new FileOutputStream(entryFile);

				IOUtils.copy(zis, fos);
				fos.close();
			}

			log("成功解压到: " + targetDirectory);

			return targetDirectory;

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (zis != null) {
					zis.close();
				}

				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private boolean publishTarget(HudsonProject hp, File latestVersion) throws Exception {
		if (!latestVersion.exists()) {
			log("文件(" + latestVersion.getAbsolutePath() + ")不存在.");
			return false;
		}

		for (File targetPath : targetPaths) {
			File target = new File(targetPath, hp.getLocalPath());
			if (target.exists()) {
				System.gc();
				FileUtils.deleteDirectory(target);
			}
			target.mkdirs();

			try {
				log("正在发布项目(" + hp.getName() + "): " + latestVersion.getAbsolutePath() + " -> " + target.getAbsolutePath());

				if (latestVersion.isDirectory()) {
					FileUtils.copyDirectory(latestVersion, target);
				} else {
					FileUtils.copyFileToDirectory(latestVersion, target, true);
				}

			} catch (Exception e) {
				throw e;

			}
		}

		return true;
	}

	/**
	 * 项目发布结束
	 * 
	 * @param hp
	 * @param publishStatus
	 */
	private void finishPublish(HudsonProject hp, HudsonPublishStatus publishStatus) {
		hp.setPublishEndTime(DateUtil.now());
		hp.setPublishStatus(publishStatus.ordinal());
		hp.setUpdateTime(DateUtil.now());
		getHibernateTemplate().update(hp);

		log("\"" + hp.getName() + "\" 项目发布完成(" + publishStatus.toString() + ").");
		getOperateLoggerService().log(OperateAction.PUBLISH, hp);

		if (logger != null) {
			IOUtils.closeQuietly(logger);
		}
	}

	/**
	 * 记录发布日志
	 * 
	 * @param content
	 */
	private void log(String content) {
		if (logger == null) {
			return;
		}

		try {
			logger.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - " + content + "\r\n");
			logger.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public PaginationSupport paging(ConditionModel model, ResultTransformer resultTransformer) {
		Long userGroupId = model.getValue("userGroupId", Long.class);
		model.removeCondition("userGroupId");

		if (userGroupId == null) {
			return new PaginationSupport(model.getStart(), model.getLimit());
		}

		// 告警日志需要只查出与指定用户组及子用户组相关的记录
		// 所以这里需要得到指定用户组及子用户组下所有用户的登录名(因为日志表中责任人存的是登录名)
		Collection<Long> userIds = new ArrayList<Long>();

		UserGroup userGroup = userGroupService.get(userGroupId);

		// 超级用户组允许查看所有数据源
		if (!userGroup.isAdministrator()) {
			// 获得指定用户组及子用户组下所有用户
			Collection<User> users = userGroupRelationService.getUsersByUserGroup(userGroupId, true);
			for (User user : users) {
				userIds.add(user.getUserId());
			}
		}

		Criteria criteria = this.createCriteria(model);

		if (userIds.size() > 0) {
			criteria.add(Restrictions.in("createBy", userIds));
		}

		return super.paging(criteria, model.getStart(), model.getLimit(), resultTransformer);
	}

	@Override
	protected void initDao() throws Exception {
		String[] targets = Configure.property(Configure.HUDSON_SYNC_TARGET_PATH).split(",");

		for (String target : targets) {
			File targetPath = new File(target);
			if (!targetPath.exists()) {
				targetPath.mkdirs();
			}

			targetPaths.add(targetPath);
		}

		tempPath = new File(Configure.property(Configure.HUDSON_SYNC_TEMP_PATH));
		if (!tempPath.exists()) {
			tempPath.mkdirs();
		}

		super.initDao();
	}

}
