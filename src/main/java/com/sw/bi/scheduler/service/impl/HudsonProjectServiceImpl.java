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
		log("???????????? \"" + hp.getName() + "\" ??????.");

		HudsonPublishStatus status = HudsonPublishStatus.PUBLISH_SUCCESS;

		try {
			this.startPublish(uuid, hp);

			File latestVersion = this.downloadLatestVersion(hp);
			if (latestVersion == null) {
				status = HudsonPublishStatus.PUBLISH_FAILURE;
				log("SVN??????????????????(" + hp.getName() + ")???????????????,??????????????????SVN?????????????????????."); //??????????????????hundson??????????????????IP?????????
				return;
			}

			if (latestVersion.getName().endsWith(".zip")) {
				// ??????Zip??????????????????????????????
				// ?????????????????????
				File unzipDirectory = this.unzip(latestVersion);
				if (unzipDirectory == null) {
					status = HudsonPublishStatus.PUBLISH_FAILURE;
					log(latestVersion.getAbsolutePath() + " ????????????.");
					return;
				}

				this.publishTarget(hp, unzipDirectory);

			} else {
				// ??????Jar??????????????????
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
			return "????????????(" + logger.getAbsolutePath() + ")?????????.";
		}

		try {
			return IOUtils.toString(new InputStreamReader(new FileInputStream(logger), "utf-8"));
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	/**
	 * ?????????????????????
	 * 
	 * @param uuid
	 */
	private void initLogger(String uuid) {
		try {
			File logFile = new File(tempPath, uuid + ".log");
			if (!logFile.exists()) {
				logFile.createNewFile();
				log.info("??????Hudson?????????????????????(" + uuid + ".log).");
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
	 * ????????????????????????
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

		// ??????????????????????????????????????????????????????URL
		Authenticator.setDefault(new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(sourceUsername, sourcePassword.toCharArray());
			}

		});
	}

	/**
	 * ?????????????????????svn??????????????????
	 * 
	 * @param hp
	 * @return
	 * @throws MalformedURLException
	 */
	private File downloadLatestVersion(HudsonProject hp) {
		log("?????????SVN?????????(" + sourcePath + ")???????????????(" + hp.getName() + ")???????????????...");

		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;

		try {
			String sourceUrl = this.sourcePath + hp.getSvnPath().replaceFirst("^/", "");
			URL latestVersionUrl = getLatestVersionURL(sourceUrl);
			if (latestVersionUrl == null) {
				log.error("??????????????????????????????.");
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
			log("?????????????????? \"" + hp.getName() + "\" ???????????????(" + file.getAbsolutePath() + ").");

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
	 * ???????????????????????????URL?????? <dir name="20120524" href="20120524/" /> <dir
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
					//?????????20120524  ?????????maxPath  ??????????????????20120525  ???????????????????????????maxPath
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
					//???????????????path= sw_udf-2345.jar   ????????????new URL(url + "/" + path)
					//???????????????jar????????????url??????
					return new URL(url + "/" + path);
				}
			}

			return getLatestVersionURL(url + "/" + maxPath); //????????????

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
	 * ?????????????????????
	 * 
	 * @param file
	 */
	private File unzip(File file) {
		if (file == null) {
			return null;
		}

		log("??????????????????????????????(" + file.getAbsolutePath() + ")...");

		// http://192.168.17.243/svn/bi/daijin/etl-daijin/20120525/20120525142129/daijin-2346-sql.zip
		// daijin-2346-sql
		String zipFileName = file.getName().substring(file.getName().lastIndexOf("/") + 1, file.getName().lastIndexOf("."));
		File targetDirectory = new File(file.getParentFile(), zipFileName);
		if (targetDirectory.exists()) {
			targetDirectory.delete();
		}
		targetDirectory.mkdirs(); //??????daijin-2346-sql??????

		FileInputStream fis = null;
		ZipInputStream zis = null;

		try {
			fis = new FileInputStream(file);
			zis = new ZipInputStream(fis);
			ZipEntry entry = null;

			while ((entry = zis.getNextEntry()) != null) {
				String entryName = entry.getName();

				//???????????????,????????????????????????
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

			log("???????????????: " + targetDirectory);

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
			log("??????(" + latestVersion.getAbsolutePath() + ")?????????.");
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
				log("??????????????????(" + hp.getName() + "): " + latestVersion.getAbsolutePath() + " -> " + target.getAbsolutePath());

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
	 * ??????????????????
	 * 
	 * @param hp
	 * @param publishStatus
	 */
	private void finishPublish(HudsonProject hp, HudsonPublishStatus publishStatus) {
		hp.setPublishEndTime(DateUtil.now());
		hp.setPublishStatus(publishStatus.ordinal());
		hp.setUpdateTime(DateUtil.now());
		getHibernateTemplate().update(hp);

		log("\"" + hp.getName() + "\" ??????????????????(" + publishStatus.toString() + ").");
		getOperateLoggerService().log(OperateAction.PUBLISH, hp);

		if (logger != null) {
			IOUtils.closeQuietly(logger);
		}
	}

	/**
	 * ??????????????????
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

		// ???????????????????????????????????????????????????????????????????????????
		// ?????????????????????????????????????????????????????????????????????????????????(?????????????????????????????????????????????)
		Collection<Long> userIds = new ArrayList<Long>();

		UserGroup userGroup = userGroupService.get(userGroupId);

		// ??????????????????????????????????????????
		if (!userGroup.isAdministrator()) {
			// ???????????????????????????????????????????????????
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
