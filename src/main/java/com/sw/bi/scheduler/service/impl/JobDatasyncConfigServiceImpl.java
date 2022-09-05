package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.util.Configure.JobStatus;
import com.sw.bi.scheduler.util.Configure.JobType;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;

@Service
public class JobDatasyncConfigServiceImpl extends GenericServiceHibernateSupport<JobDatasyncConfig> implements JobDatasyncConfigService {

	@Override
	public JobDatasyncConfig getJobDatasyncConfigByJob(long jobId) {
		return getHibernateTemplate().get(entityClass, jobId);
	}

	@Override
	public void saveOrUpdate(final JobDatasyncConfig config) {
		if (config.getJobType() == JobType.FTP_FILE_TO_HDFS.indexOf() || config.getJobType() == JobType.FTP_FILE_TO_HDFS_FIVE_MINUTE.indexOf() ||
				config.getJobType() == JobType.FTP_FILE_TO_HDFS_YESTERDAY.indexOf()) {

			final StringBuffer sql = new StringBuffer("select count(*) count from job_datasync_config jdc left join job j on jdc.jobid = j.job_id ");
			sql.append("where jdc.jobid <> :jobId ");
			sql.append("and jdc.ftp_datasource_id = :ftpDatasourceId ");
			sql.append("and j.job_status = :jobStatus ");

			getHibernateTemplate().execute(new HibernateCallback<Integer>() {

				@Override
				public Integer doInHibernate(Session session) {
					/*StringBuffer ftpDirSql = new StringBuffer(sql.toString());
					ftpDirSql.append("and jdc.ftp_dir = :ftpDir");

					SQLQuery query = session.createSQLQuery(ftpDirSql.toString());
					query.addScalar("count", Hibernate.INTEGER);
					query.setParameter("jobId", config.getJobId());
					query.setParameter("ftpDatasourceId", config.getDatasourceByFtpDatasourceId().getDatasourceId());
					query.setParameter("jobStatus", (long) JobStatus.ON_LINE.indexOf());
					query.setParameter("ftpDir", config.getFtpDir());
					Integer count = (Integer) query.uniqueResult();

					if (count != null && count > 0) {
						throw new Warning("\"FTP远程目录\" 不允许重复!");
					}*/

					StringBuffer hdfsPath = new StringBuffer(sql.toString());
					hdfsPath.append("and jdc.hdfs_path = :hdfsPath");

					SQLQuery query = session.createSQLQuery(hdfsPath.toString());
					query.addScalar("count", StandardBasicTypes.INTEGER);
					query.setParameter("jobId", config.getJobId());
					query.setParameter("ftpDatasourceId", config.getDatasourceByFtpDatasourceId().getDatasourceId());
					query.setParameter("jobStatus", (long) JobStatus.ON_LINE.indexOf());
					query.setParameter("hdfsPath", config.getHdfsPath());
					Number count = (Number) query.uniqueResult();

					if (count != null && count.intValue() > 0) {
						throw new Warning("\"HDFS路径\" 不允许重复!");
					}

					return count.intValue();
				}

			});

			/*Criteria criteria = createCriteria();
			criteria.add(Restrictions.eq("ftpDir", config.getFtpDir()));
			criteria.add(Restrictions.not(Restrictions.eq("jobId", config.getJobId())));
			JobDatasyncConfig c = (JobDatasyncConfig) criteria.uniqueResult();

			if (c != null) {
				throw new Warning("\"FTP远程目录\" 不允许重复!");
			}

			criteria = createCriteria();
			criteria.add(Restrictions.eq("hdfsPath", config.getHdfsPath()));
			criteria.add(Restrictions.not(Restrictions.eq("jobId", config.getJobId())));
			c = (JobDatasyncConfig) criteria.uniqueResult();

			if (c != null) {
				throw new Warning("\"HDFS路径\" 不允许重复!");
			}*/
		}

		getHibernateTemplate().saveOrUpdate(config);
	}

}
